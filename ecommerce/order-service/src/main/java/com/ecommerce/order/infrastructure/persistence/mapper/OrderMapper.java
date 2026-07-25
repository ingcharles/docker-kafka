package com.ecommerce.order.infrastructure.persistence.mapper;

import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import com.ecommerce.order.application.dto.AddressResponse;
import com.ecommerce.order.application.dto.OrderItemResponse;
import com.ecommerce.order.application.dto.OrderResponse;
import com.ecommerce.order.domain.model.entity.Order;
import com.ecommerce.order.domain.model.entity.OrderItem;
import com.ecommerce.order.domain.model.enums.OrderStatus;
import com.ecommerce.order.domain.model.vo.Address;
import com.ecommerce.order.infrastructure.persistence.entity.OrderEntity;
import com.ecommerce.order.infrastructure.persistence.entity.OrderItemEntity;

/**
 * Mapeo entre dominio y respuesta REST, y entre dominio y entidad de
 * persistencia. El campo {@code shippingAddress} (un value object) se
 * "aplana" hacia las cinco columnas correspondientes de {@code OrderEntity}
 * usando {@code source} con notación de punto (MapStruct soporta este
 * aplanamiento de forma nativa); para el camino inverso ("desaplanar", es
 * decir reconstruir el value object a partir de las columnas sueltas) hace
 * falta una expresión Java explícita, ya que ahí MapStruct no puede
 * inferirlo solo. La referencia inversa de {@code OrderItemEntity} hacia su
 * orden padre tampoco es algo que MapStruct pueda deducir a partir de los
 * nombres de los campos, así que el método {@link #linkItemsToOrder} la
 * completa manualmente justo después de que corre el mapeo principal.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface OrderMapper {

    @Mapping(target = "totalAmount", expression = "java(domain.totalAmount())")
    OrderResponse toResponse(Order domain);

    AddressResponse toResponse(Address address);

    List<OrderItemResponse> toItemResponses(List<OrderItem> items);

    @Mapping(target = "subtotal", expression = "java(item.subtotal())")
    OrderItemResponse toResponse(OrderItem item);

    @Mapping(target = "shippingStreet", source = "shippingAddress.street")
    @Mapping(target = "shippingCity", source = "shippingAddress.city")
    @Mapping(target = "shippingState", source = "shippingAddress.state")
    @Mapping(target = "shippingZip", source = "shippingAddress.zip")
    @Mapping(target = "shippingCountry", source = "shippingAddress.country")
    @Mapping(target = "totalAmount", expression = "java(domain.totalAmount())")
    OrderEntity toEntity(Order domain);

    List<OrderItemEntity> toItemEntities(List<OrderItem> items);

    @AfterMapping
    default void linkItemsToOrder(@MappingTarget OrderEntity entity) {
        if (entity.items != null) {
            entity.items.forEach(item -> item.order = entity);
        }
    }

    @Mapping(target = "shippingAddress", expression = "java(new Address(entity.shippingStreet, "
        + "entity.shippingCity, entity.shippingState, entity.shippingZip, entity.shippingCountry))")
    Order toDomain(OrderEntity entity);

    List<OrderItem> toItems(List<OrderItemEntity> entities);

    default String map(OrderStatus status) {
        return status == null ? null : status.name();
    }
}
