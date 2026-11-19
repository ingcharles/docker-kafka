package com.facturacion.notificacion.domain.repository;

import java.util.List;
import java.util.Optional;

import io.smallrye.mutiny.Uni;

import com.facturacion.notificacion.domain.model.entity.PlantillaCorreoEmpresa;
import com.facturacion.notificacion.domain.model.enums.TipoComprobanteCorreo;
import com.facturacion.notificacion.domain.model.vo.VersionPlantillaCorreo;

/** Puerto de persistencia de las plantillas de correo de cada empresa y de su historial. */
public interface PlantillaCorreoRepository {

    /** La plantilla de la empresa para ese tipo, esté activa o no; {@code null} si no tiene. */
    Uni<PlantillaCorreoEmpresa> buscar(String empresaRuc, TipoComprobanteCorreo tipo);

    /**
     * La plantilla ACTIVA de la empresa para ese tipo, o vacío. Es la que se lee en cada correo, por eso el resultado se guarda
     * en memoria por un tiempo corto; quien guarda o elimina una plantilla llama a {@link #renovarCache()} cuando su
     * transacción ya terminó.
     */
    Uni<Optional<PlantillaCorreoEmpresa>> buscarActiva(String empresaRuc, TipoComprobanteCorreo tipo);

    Uni<List<PlantillaCorreoEmpresa>> listar(String empresaRuc);

    /** Versiones anteriores de la plantilla, la más nueva primero. */
    Uni<List<VersionPlantillaCorreo>> listarHistorial(String empresaRuc, TipoComprobanteCorreo tipo);

    /**
     * Guarda la plantilla. Si había una versión anterior ({@code anterior} no es nulo) la deja en el historial como
     * REEMPLAZADA; si no, es la primera versión.
     */
    Uni<PlantillaCorreoEmpresa> guardar(PlantillaCorreoEmpresa plantilla, PlantillaCorreoEmpresa anterior);

    /** Borra la plantilla y deja su última versión en el historial como ELIMINADA. */
    Uni<Void> eliminar(PlantillaCorreoEmpresa plantilla);

    /** Descarta lo que {@link #buscarActiva} tiene en memoria, para que el siguiente correo lea la plantilla nueva. */
    Uni<Void> renovarCache();
}
