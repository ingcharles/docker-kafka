package com.facturacion.orquestacion.application.service;

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
import com.facturacion.orquestacion.application.dto.SagaResponse;
import com.facturacion.orquestacion.application.dto.SagaStepResponse;
import com.facturacion.orquestacion.domain.model.entity.SagaInstance;
import com.facturacion.orquestacion.domain.model.entity.SagaStep;
import com.facturacion.orquestacion.domain.model.enums.SagaState;
import com.facturacion.orquestacion.domain.repository.SagaInstanceRepository;
import com.facturacion.orquestacion.domain.repository.SagaStepRepository;

@ApplicationScoped
public class SagaQueryService {

    private final SagaInstanceRepository sagaInstanceRepository;
    private final SagaStepRepository sagaStepRepository;

    @Inject
    public SagaQueryService(SagaInstanceRepository sagaInstanceRepository, SagaStepRepository sagaStepRepository) {
        this.sagaInstanceRepository = sagaInstanceRepository;
        this.sagaStepRepository = sagaStepRepository;
    }

    /**
     * @param empresaRuc RUC de la api-key: una saga de otra empresa responde igual que una inexistente (404), para no
     *                   revelar que existe.
     */
    @WithSession
    public Uni<SagaResponse> getByComprobanteId(UUID comprobanteId, String empresaRuc) {
        return sagaInstanceRepository.findById(comprobanteId)
            .onItem().transform(saga -> saga != null && saga.getEmpresaRuc().equals(empresaRuc) ? saga : null)
            .onItem().ifNull().failWith(() -> new NotFoundException("No hay saga para el comprobante " + comprobanteId))
            .onItem().transformToUni(saga -> sagaStepRepository.findBySagaId(comprobanteId)
                .map(steps -> toResponse(saga, steps)));
    }

    @WithSession
    public Uni<PagedResponse<SagaResponse>> listPaged(int page, int limit, SagaState state, String empresaRuc) {
        return sagaInstanceRepository.findPaged(page, limit, state, empresaRuc)
            .map((PageResult<SagaInstance> pageResult) -> {
                List<SagaResponse> items = pageResult.items().stream()
                    .map(saga -> toResponse(saga, List.of()))
                    .collect(Collectors.toList());
                return PagedResponse.of(items, page, limit, pageResult.total());
            });
    }

    private SagaResponse toResponse(SagaInstance saga, List<SagaStep> steps) {
        SagaResponse response = new SagaResponse();
        response.setComprobanteId(saga.getId());
        response.setState(saga.getState().name());
        response.setEmpresaRuc(saga.getEmpresaRuc());
        response.setClaveAcceso(saga.getClaveAcceso());
        response.setAmbiente(saga.getAmbiente());
        response.setFailureReason(saga.getFailureReason());
        response.setCreatedAt(saga.getCreatedAt());
        response.setUpdatedAt(saga.getUpdatedAt());
        response.setReintentos(saga.getReintentos());
        response.setReintentosNotificacion(saga.getReintentosNotificacion());
        response.setSteps(steps.stream().map(this::toStepResponse).collect(Collectors.toList()));
        return response;
    }

    private SagaStepResponse toStepResponse(SagaStep step) {
        SagaStepResponse response = new SagaStepResponse();
        response.setStepName(step.getStepName());
        response.setStatus(step.getStatus());
        response.setCreatedAt(step.getCreatedAt());
        return response;
    }
}
