package com.facturacion.comprobantes.domain.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.facturacion.commons.exception.ErrorNegocioException;
import com.facturacion.comprobantes.domain.model.enums.Catalogo;
import com.facturacion.comprobantes.shared.error.CodigoError;

class CatalogoDefTest {

    private static CatalogoDef def(Catalogo catalogo) {
        return CatalogoDef.buscar(catalogo.nombre()).orElseThrow();
    }

    private static Map<String, Object> mapa(Object... paresClaveValor) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < paresClaveValor.length; i += 2) {
            m.put((String) paresClaveValor[i], paresClaveValor[i + 1]);
        }
        return m;
    }

    /** Debe lanzar un ErrorNegocioException con ese código; devuelve la excepción para revisar detalles. */
    private static ErrorNegocioException rechaza(Executable accion, CodigoError esperado) {
        ErrorNegocioException error = assertThrows(ErrorNegocioException.class, accion);
        assertEquals(esperado, error.getCodigo());
        return error;
    }

    /** Rechazo por campos inválidos: el código del error es CAMPOS_INVALIDOS y el detalle trae el texto del campo. */
    private static void rechazaCampo(Executable accion, String fragmentoDelDetalle) {
        ErrorNegocioException error = rechaza(accion, CodigoError.CAMPOS_INVALIDOS);
        assertTrue(error.getDetalles().stream().anyMatch(d -> d.contains(fragmentoDelDetalle)),
            "Los detalles " + error.getDetalles() + " deberían mencionar '" + fragmentoDelDetalle + "'");
    }

    @Test
    void testDeberiaTodosLosCatalogosDelEnumTienenDefinicionConClave() {
        assertEquals(Catalogo.values().length, CatalogoDef.TODOS.size());
        assertEquals(CatalogoDef.TODOS.size(),
            new HashSet<>(CatalogoDef.TODOS.stream().map(CatalogoDef::nombre).toList()).size(), "nombres únicos");
        CatalogoDef.TODOS.forEach(c -> assertFalse(c.listarClaves().isEmpty(), c.nombre() + " sin clave"));
    }

    @Test
    void testDeberiaBuscarPorNombreDeCable() {
        assertEquals(Catalogo.FORMA_PAGO, CatalogoDef.buscar("forma_pago").orElseThrow().catalogo());
        assertTrue(CatalogoDef.buscar("no_existe").isEmpty());
    }

    @Test
    void testDeberiaFormaPagoAplicaDefaultsDeVigenciaYActivo() {
        Map<String, Object> v = def(Catalogo.FORMA_PAGO).validar(mapa("codigo", "22", "descripcion", "NUEVA"));
        assertEquals("22", v.get("codigo"));
        assertEquals("1900-01-01", v.get(CatalogoDef.COL_VIGENTE_DESDE));
        assertEquals(Boolean.TRUE, v.get(CatalogoDef.COL_ACTIVO));
        assertNull(v.get(CatalogoDef.COL_VIGENTE_HASTA));
    }

    @Test
    void testDeberiaRetencionNormalizaDecimalesYFechas() {
        Map<String, Object> v = def(Catalogo.RETENCION).validar(mapa(
            "impuesto_codigo", "6", "codigo", "4586", "vigente_desde", "2025-05-01",
            "descripcion", "ISD 2.5%", "porcentaje", 2.50));
        assertEquals("2.5", v.get(CatalogoDef.COL_PORCENTAJE));
        assertEquals("2025-05-01", v.get(CatalogoDef.COL_VIGENTE_DESDE));
    }

    @Test
    void testDeberiaRechazaCuerpoNuloYCamposDesconocidos() {
        rechaza(() -> def(Catalogo.IMPUESTO).validar(null), CodigoError.CUERPO_OBLIGATORIO);
        rechaza(() -> def(Catalogo.IMPUESTO).validar(mapa("codigo", "9", "descripcion", "X", "otro", "1")),
            CodigoError.CAMPO_DESCONOCIDO);
    }

    @Test
    void testDeberiaRechazaFaltantesYLongitudes() {
        rechazaCampo(() -> def(Catalogo.IMPUESTO).validar(mapa("descripcion", "X")), "'codigo' es obligatorio");
        rechazaCampo(() -> def(Catalogo.IMPUESTO).validar(mapa("codigo", "123", "descripcion", "X")),
            "entre 1 y 2");
    }

    @Test
    void testDeberiaAgrupaVariosProblemasEnUnSoloErrorConDetalles() {
        ErrorNegocioException error = rechaza(() -> def(Catalogo.TARIFA_IMPUESTO).validar(mapa(
            "tarifa", "abc", "vigente_desde", "01/02/2025")), CodigoError.CAMPOS_INVALIDOS);
        // faltan impuesto_codigo, codigo_porcentaje, descripcion; tarifa no numérica; fecha mal escrita
        assertTrue(error.getDetalles().size() >= 5, "detalles: " + error.getDetalles());
    }

    @Test
    void testDeberiaRechazaTiposYRangosInvalidos() {
        rechazaCampo(() -> def(Catalogo.TARIFA_IMPUESTO).validar(mapa(
            "impuesto_codigo", "2", "codigo_porcentaje", "9", "descripcion", "X", "tarifa", "abc")),
            "'tarifa' debe ser numérico");
        rechazaCampo(() -> def(Catalogo.RETENCION).validar(mapa(
            "impuesto_codigo", "2", "codigo", "99", "descripcion", "X", "porcentaje", 101)), "entre 0 y 100");
        rechazaCampo(() -> def(Catalogo.FORMA_PAGO).validar(mapa(
            "codigo", "22", "descripcion", "X", "vigente_desde", "01/02/2025")), "yyyy-MM-dd");
        rechazaCampo(() -> def(Catalogo.IMPUESTO).validar(mapa("codigo", "9", "descripcion", "X", "activo", "tal")),
            "true o false");
    }

    @Test
    void testDeberiaRechazaVigenciaInvertida() {
        rechazaCampo(() -> def(Catalogo.FORMA_PAGO).validar(mapa(
            "codigo", "22", "descripcion", "X", "vigente_desde", "2025-05-01", "vigente_hasta", "2025-04-30")),
            "anterior");
    }

    @Test
    void testDeberiaSoloLosCatalogosConVigenciaLaDeclaran() {
        assertEquals(List.of(Catalogo.TARIFA_IMPUESTO, Catalogo.FORMA_PAGO, Catalogo.RETENCION),
            CatalogoDef.TODOS.stream().filter(CatalogoDef::tieneVigencia).map(CatalogoDef::catalogo).toList());
    }

    @Test
    void testDeberiaLaTarifaSeVersionaPorFechaYTipoSinTipoSeAsumePorcentaje() {
        CatalogoDef def = def(Catalogo.TARIFA_IMPUESTO);

        Map<String, Object> valores = def.validar(mapa(
            "impuesto_codigo", "3", "codigo_porcentaje", "3011", "descripcion", "ICE cigarrillos", "tarifa", "0.16"));

        assertEquals("PORCENTAJE", valores.get("tipo_tarifa"));
        assertEquals(List.of("impuesto_codigo", "codigo_porcentaje", "tipo_tarifa", "vigente_desde"),
            def.listarClaves().stream().map(CatalogoDef.Columna::nombre).toList());
    }

    @Test
    void testDeberiaElTipoDeTarifaSoloAdmiteLosValoresDelEnum() {
        CatalogoDef def = def(Catalogo.TARIFA_IMPUESTO);

        assertEquals("ESPECIFICA", def.validar(mapa("impuesto_codigo", "3", "codigo_porcentaje", "3011",
            "descripcion", "X", "tarifa", "0.16", "tipo_tarifa", "ESPECIFICA")).get("tipo_tarifa"));
        rechazaCampo(() -> def.validar(mapa("impuesto_codigo", "3", "codigo_porcentaje", "3011",
            "descripcion", "X", "tarifa", "0.16", "tipo_tarifa", "OTRA")), "debe ser uno de");
    }
}
