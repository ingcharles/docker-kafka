package com.ecommerce.inventory.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import com.ecommerce.inventory.application.dto.ProductResponse;
import com.ecommerce.inventory.domain.model.entity.Product;
import com.ecommerce.inventory.infrastructure.persistence.entity.ProductEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface ProductMapper {

    ProductResponse toResponse(Product domain);

    Product toDomain(ProductEntity entity);

    ProductEntity toEntity(Product domain);
}
