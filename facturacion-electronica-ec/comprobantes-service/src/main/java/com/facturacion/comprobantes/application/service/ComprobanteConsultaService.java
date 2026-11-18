package com.facturacion.comprobantes.application.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;

import com.facturacion.commons.exception.NotFoundException;
import com.facturacion.commons.paging.PageResult;
import com.facturacion.commons.paging.PagedResponse;
import com.facturacion.comprobantes.application.dto.ComprobanteResponse;
import com.facturacion.comprobantes.application.mapper.ComprobanteResponseMapper;
import com.facturacion.comprobantes.domain.model.entity.Comprobante;
import com.facturacion.comprobantes.domain.model.enums.EstadoComprobante;
import com.facturacion.comprobantes.domain.repository.ComprobanteRepository;
import com.facturacion.comprobantes.shared.error.CodigoError;

/**
 * Lado de consulta de los comprobantes (separado de la emisión, que es otra responsabilidad). Todas las consultas
 * son de una empresa: un comprobante de otro RUC se trata como inexistente (404), así que una api-key no ve ni
 * revela los de las demás.
 */
@ApplicationScoped
public class ComprobanteConsultaService {

    private final ComprobanteRepository comprobanteRepository;
    private final ComprobanteResponseMapper mapper;

    @Inject
    public ComprobanteConsultaService(ComprobanteRepository comprobanteRepository, ComprobanteResponseMapper mapper) {
        this.comprobanteRepository = comprobanteRepository;
        this.mapper = mapper;
    }

    @WithSession
    public Uni<ComprobanteResponse> obtenerPorId(UUID id, String empresaRuc) {
        return obtenerDominioPorId(id, empresaRuc).map(mapper::toResponse);
    }

    @WithSession
    public Uni<Comprobante> obtenerDominioPorId(UUID id, String empresaRuc) {
        return comprobanteRepository.findById(id)
            .onItem().transform(c -> filtrarPorEmpresa(c, empresaRuc))
            .onItem().ifNull().failWith(() -> new NotFoundException(CodigoError.COMPROBANTE_NO_ENCONTRADO, id));
    }

    /** @param empresaRuc siempre el de la api-key: el listado nunca cruza empresas. */
    @WithSession
    public Uni<PagedResponse<ComprobanteResponse>> listarPaginado(int page, int limit, EstadoComprobante estado,
            String empresaRuc) {
        return comprobanteRepository.findPaged(page, limit, estado, empresaRuc)
            .map((PageResult<Comprobante> pageResult) -> {
                List<ComprobanteResponse> items = pageResult.items().stream()
                    .map(mapper::toResponse)
                    .collect(Collectors.toList());
                return PagedResponse.of(items, page, limit, pageResult.total());
            });
    }

    private static Comprobante filtrarPorEmpresa(Comprobante comprobante, String empresaRuc) {
        return comprobante != null && comprobante.getEmpresaRuc().equals(empresaRuc) ? comprobante : null;
    }
}
