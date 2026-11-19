package com.facturacion.notificacion.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;

import com.facturacion.notificacion.domain.model.entity.PlantillaCorreoEmpresa;
import com.facturacion.notificacion.domain.model.enums.AccionHistorialPlantilla;
import com.facturacion.notificacion.domain.model.enums.TipoComprobanteCorreo;
import com.facturacion.notificacion.domain.model.vo.VersionPlantillaCorreo;
import com.facturacion.notificacion.domain.repository.PlantillaCorreoRepository;
import com.facturacion.notificacion.infrastructure.persistence.entity.PlantillaCorreoEntity;
import com.facturacion.notificacion.infrastructure.persistence.entity.PlantillaCorreoHistorialEntity;
import com.facturacion.notificacion.infrastructure.persistence.mapper.PlantillaCorreoMapper;

@ApplicationScoped
public class PlantillaCorreoRepositoryImpl implements PlantillaCorreoRepository {

    /** Nombre de la caché (su plazo y tamaño están en {@code application.properties}). */
    static final String CACHE_PLANTILLAS = "plantilla-correo";

    private static final String POR_EMPRESA_Y_TIPO = "empresaRuc = ?1 and tipoComprobante = ?2";
    private static final String POR_EMPRESA_Y_TIPO_ACTIVA = POR_EMPRESA_Y_TIPO + " and activa = true";
    private static final String POR_EMPRESA_ORDENADO_POR_TIPO = "empresaRuc = ?1 order by tipoComprobante";
    private static final String HISTORIAL_MAS_NUEVO_PRIMERO = POR_EMPRESA_Y_TIPO + " order by version desc";

    private final PlantillaCorreoPanacheRepository repository;
    private final PlantillaCorreoHistorialPanacheRepository historialRepository;
    private final PlantillaCorreoMapper mapper;
    private final Cache cachePlantillas;

    @Inject
    public PlantillaCorreoRepositoryImpl(PlantillaCorreoPanacheRepository repository,
            PlantillaCorreoHistorialPanacheRepository historialRepository, PlantillaCorreoMapper mapper,
            @CacheName(CACHE_PLANTILLAS) Cache cachePlantillas) {
        this.repository = repository;
        this.historialRepository = historialRepository;
        this.mapper = mapper;
        this.cachePlantillas = cachePlantillas;
    }

    @Override
    public Uni<PlantillaCorreoEmpresa> buscar(String empresaRuc, TipoComprobanteCorreo tipo) {
        return repository.<PlantillaCorreoEntity>find(POR_EMPRESA_Y_TIPO, empresaRuc, tipo.name()).firstResult()
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }

    @Override
    @CacheResult(cacheName = CACHE_PLANTILLAS)
    public Uni<Optional<PlantillaCorreoEmpresa>> buscarActiva(String empresaRuc, TipoComprobanteCorreo tipo) {
        return repository.<PlantillaCorreoEntity>find(POR_EMPRESA_Y_TIPO_ACTIVA, empresaRuc, tipo.name()).firstResult()
            .map(entity -> Optional.ofNullable(entity).map(mapper::toDomain));
    }

    @Override
    public Uni<List<PlantillaCorreoEmpresa>> listar(String empresaRuc) {
        return repository.<PlantillaCorreoEntity>list(POR_EMPRESA_ORDENADO_POR_TIPO, empresaRuc)
            .map(entities -> entities.stream().map(mapper::toDomain).toList());
    }

    @Override
    public Uni<List<VersionPlantillaCorreo>> listarHistorial(String empresaRuc, TipoComprobanteCorreo tipo) {
        return historialRepository.<PlantillaCorreoHistorialEntity>list(HISTORIAL_MAS_NUEVO_PRIMERO, empresaRuc,
                tipo.name())
            .map(filas -> filas.stream()
                .map(fila -> new VersionPlantillaCorreo(fila.version, fila.asunto, fila.cuerpoHtml, fila.cuerpoTexto,
                    fila.activa, AccionHistorialPlantilla.valueOf(fila.accion), fila.registradoEn))
                .toList());
    }

    @Override
    public Uni<PlantillaCorreoEmpresa> guardar(PlantillaCorreoEmpresa plantilla, PlantillaCorreoEmpresa anterior) {
        if (anterior == null) {
            return repository.persist(mapper.toEntity(plantilla)).replaceWith(plantilla);
        }
        return repository.<PlantillaCorreoEntity>findById(plantilla.getId())
            .chain(existente -> {
                existente.asunto = plantilla.getAsunto();
                existente.cuerpoHtml = plantilla.getCuerpoHtml();
                existente.cuerpoTexto = plantilla.getCuerpoTexto();
                existente.version = plantilla.getVersion();
                existente.activa = plantilla.isActiva();
                existente.actualizadoEn = plantilla.getActualizadoEn();
                return historialRepository.persist(armarFilaDeHistorial(anterior, AccionHistorialPlantilla.REEMPLAZADA,
                    plantilla.getActualizadoEn()));
            })
            .replaceWith(plantilla);
    }

    @Override
    public Uni<Void> eliminar(PlantillaCorreoEmpresa plantilla) {
        return historialRepository
            .persist(armarFilaDeHistorial(plantilla, AccionHistorialPlantilla.ELIMINADA, Instant.now()))
            .chain(() -> repository.deleteById(plantilla.getId()))
            .replaceWithVoid();
    }

    @Override
    public Uni<Void> renovarCache() {
        return cachePlantillas.invalidateAll();
    }

    private static PlantillaCorreoHistorialEntity armarFilaDeHistorial(PlantillaCorreoEmpresa version,
            AccionHistorialPlantilla accion, Instant cuando) {
        PlantillaCorreoHistorialEntity fila = new PlantillaCorreoHistorialEntity();
        fila.id = UUID.randomUUID();
        fila.empresaRuc = version.getEmpresaRuc();
        fila.tipoComprobante = version.getTipoComprobante();
        fila.version = version.getVersion();
        fila.asunto = version.getAsunto();
        fila.cuerpoHtml = version.getCuerpoHtml();
        fila.cuerpoTexto = version.getCuerpoTexto();
        fila.activa = version.isActiva();
        fila.accion = accion.name();
        fila.registradoEn = cuando;
        return fila;
    }
}
