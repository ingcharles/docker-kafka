package com.facturacion.comprobantes.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;

import com.facturacion.comprobantes.domain.model.entity.ConfiguracionEmpresaLocal;
import com.facturacion.comprobantes.domain.repository.ConfiguracionEmpresaLocalRepository;
import com.facturacion.comprobantes.infrastructure.persistence.entity.ConfiguracionEmpresaLocalEntity;
import com.facturacion.comprobantes.infrastructure.persistence.mapper.ConfiguracionEmpresaLocalMapper;

@ApplicationScoped
public class ConfiguracionEmpresaLocalRepositoryImpl implements ConfiguracionEmpresaLocalRepository {

    private final ConfiguracionEmpresaLocalPanacheRepository repository;
    private final ConfiguracionEmpresaLocalMapper mapper;

    @Inject
    public ConfiguracionEmpresaLocalRepositoryImpl(ConfiguracionEmpresaLocalPanacheRepository repository,
            ConfiguracionEmpresaLocalMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Uni<Void> insertarActualizarOEliminar(String ruc, String clave, String valor, Instant actualizadoEn) {
        return repository.<ConfiguracionEmpresaLocalEntity>find("ruc = ?1 and clave = ?2", ruc, clave)
            .firstResult()
            .onItem().transformToUni(existente -> {
                if (valor == null) {
                    return existente == null ? Uni.createFrom().voidItem()
                        : repository.delete(existente).replaceWithVoid();
                }
                if (existente == null) {
                    ConfiguracionEmpresaLocalEntity nueva = new ConfiguracionEmpresaLocalEntity();
                    nueva.id = UUID.randomUUID();
                    nueva.ruc = ruc;
                    nueva.clave = clave;
                    nueva.valor = valor;
                    nueva.actualizadoEn = actualizadoEn;
                    return repository.persist(nueva).replaceWithVoid();
                }
                existente.valor = valor;
                existente.actualizadoEn = actualizadoEn;
                return Uni.createFrom().voidItem();
            });
    }

    @Override
    public Uni<ConfiguracionEmpresaLocal> findByRucAndClave(String ruc, String clave) {
        return repository.<ConfiguracionEmpresaLocalEntity>find("ruc = ?1 and clave = ?2", ruc, clave).firstResult()
            .map(entity -> entity == null ? null : mapper.toDomain(entity));
    }
}
