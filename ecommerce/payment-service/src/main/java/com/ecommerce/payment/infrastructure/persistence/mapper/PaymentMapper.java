package com.ecommerce.payment.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.ecommerce.payment.application.dto.PaymentResponse;
import com.ecommerce.payment.domain.model.entity.Payment;
import com.ecommerce.payment.infrastructure.persistence.entity.PaymentEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface PaymentMapper {

    PaymentResponse toResponse(Payment domain);

    Payment toDomain(PaymentEntity entity);

    PaymentEntity toEntity(Payment domain);
}
