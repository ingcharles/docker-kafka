package com.ecommerce.saga.application.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;

import com.ecommerce.saga.application.dto.SagaResponse;
import com.ecommerce.saga.application.dto.SagaStepResponse;
import com.ecommerce.saga.domain.model.entity.SagaInstance;
import com.ecommerce.saga.domain.model.entity.SagaStep;
import com.ecommerce.saga.domain.model.enums.SagaState;
import com.ecommerce.saga.domain.model.vo.PageResult;
import com.ecommerce.saga.domain.repository.SagaInstanceRepository;
import com.ecommerce.saga.domain.repository.SagaStepRepository;
import com.ecommerce.saga.shared.exception.NotFoundException;
import com.ecommerce.saga.shared.responses.PagedResponse;

@ApplicationScoped
public class SagaQueryService {

    private final SagaInstanceRepository sagaInstanceRepository;
    private final SagaStepRepository sagaStepRepository;

    @Inject
    public SagaQueryService(SagaInstanceRepository sagaInstanceRepository,
        SagaStepRepository sagaStepRepository) {
        this.sagaInstanceRepository = sagaInstanceRepository;
        this.sagaStepRepository = sagaStepRepository;
    }

    @WithSession
    public Uni<SagaResponse> getByOrderId(UUID orderId) {
        return sagaInstanceRepository.findById(orderId)
            .onItem().ifNull().failWith(() -> new NotFoundException("Saga not found for order: " + orderId))
            .onItem().transformToUni(saga -> sagaStepRepository.findBySagaId(orderId)
                .map(steps -> toResponse(saga, steps)));
    }

    @WithSession
    public Uni<PagedResponse<SagaResponse>> listPaged(int page, int limit, SagaState state) {
        return sagaInstanceRepository.findPaged(page, limit, state)
            .map((PageResult<SagaInstance> pageResult) -> {
                List<SagaResponse> items = pageResult.items().stream()
                    .map(saga -> toResponse(saga, List.of()))
                    .collect(Collectors.toList());
                return PagedResponse.of(items, page, limit, pageResult.total());
            });
    }

    private SagaResponse toResponse(SagaInstance saga, List<SagaStep> steps) {
        SagaResponse response = new SagaResponse();
        response.setOrderId(saga.getId());
        response.setState(saga.getState().name());
        response.setCustomerId(saga.getCustomerId());
        response.setTotalAmount(saga.getTotalAmount());
        response.setCurrency(saga.getCurrency());
        response.setFailureReason(saga.getFailureReason());
        response.setCreatedAt(saga.getCreatedAt());
        response.setUpdatedAt(saga.getUpdatedAt());
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
