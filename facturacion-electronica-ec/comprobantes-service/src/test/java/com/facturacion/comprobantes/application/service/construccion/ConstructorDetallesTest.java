package com.facturacion.comprobantes.application.service.construccion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.facturacion.comprobantes.application.dto.CampoAdicionalRequest;
import com.facturacion.comprobantes.application.dto.DetalleComprobanteRequest;
import com.facturacion.comprobantes.application.dto.ImpuestoRequest;
import com.facturacion.comprobantes.domain.model.entity.DetalleComprobante;
import com.facturacion.comprobantes.domain.model.entity.ItemLocal;
import com.facturacion.comprobantes.domain.model.vo.CampoAdicional;
import com.facturacion.comprobantes.domain.model.vo.CatalogosSriAFecha;

/** Los detalles adicionales de la línea: los del llamador (hasta 3) y la "Medida" que sale de la unidad del ítem. */
class ConstructorDetallesTest {

    private final ConstructorDetalles constructor = new ConstructorDetalles(null);
    private final CatalogosSriAFecha catalogos = new CatalogosSriAFecha(LocalDate.of(2026, 11, 14), List.of(),
        List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

    private static ItemLocal item(UUID id, String unidad) {
        ItemLocal item = new ItemLocal();
        item.setId(id);
        item.setUnidadMedida(unidad);
        return item;
    }

    private static CampoAdicionalRequest campo(String nombre, String valor) {
        CampoAdicionalRequest campo = new CampoAdicionalRequest();
        campo.setNombre(nombre);
        campo.setValor(valor);
        return campo;
    }

    private static DetalleComprobanteRequest linea(UUID itemId, List<CampoAdicionalRequest> adicionales) {
        ImpuestoRequest iva = new ImpuestoRequest();
        iva.setCodigo("2");
        iva.setCodigoPorcentaje("4");
        iva.setTarifa(new BigDecimal("15"));
        DetalleComprobanteRequest request = new DetalleComprobanteRequest();
        request.setItemId(itemId);
        request.setCodigoPrincipal("A");
        request.setDescripcion("Producto");
        request.setCantidad(BigDecimal.ONE);
        request.setPrecioUnitario(BigDecimal.TEN);
        request.setImpuestos(List.of(iva));
        request.setDetallesAdicionales(adicionales);
        return request;
    }

    @Test
    void laUnidadDelItemSaleComoMedidaPrimero() {
        UUID id = UUID.randomUUID();

        DetalleComprobante detalle = constructor.construir(
            linea(id, List.of(campo("Centro Logistico", "23"))), Map.of(id, item(id, "UN")), catalogos);

        assertEquals(List.of(new CampoAdicional("Medida", "UN"), new CampoAdicional("Centro Logistico", "23")),
            detalle.getDetallesAdicionales());
    }

    @Test
    void unaMedidaMandadaPorElLlamadorNoSeDuplica() {
        UUID id = UUID.randomUUID();

        DetalleComprobante detalle = constructor.construir(
            linea(id, List.of(campo("medida", "KG"))), Map.of(id, item(id, "UN")), catalogos);

        assertEquals(List.of(new CampoAdicional("medida", "KG")), detalle.getDetallesAdicionales());
    }

    @Test
    void conTresDetallesDelLlamadorNoSeAgregaLaMedidaPorqueElXsdAdmiteTres() {
        UUID id = UUID.randomUUID();

        DetalleComprobante detalle = constructor.construir(
            linea(id, List.of(campo("a", "1"), campo("b", "2"), campo("c", "3"))), Map.of(id, item(id, "UN")),
            catalogos);

        assertEquals(3, detalle.getDetallesAdicionales().size());
    }

    @Test
    void sinItemOSinUnidadSoloQuedanLosDelLlamador() {
        UUID id = UUID.randomUUID();

        assertTrue(constructor.construir(linea(null, null), Map.of(), catalogos).getDetallesAdicionales().isEmpty());
        assertTrue(constructor.construir(linea(id, null), Map.of(id, item(id, " ")), catalogos)
            .getDetallesAdicionales().isEmpty());
    }
}
