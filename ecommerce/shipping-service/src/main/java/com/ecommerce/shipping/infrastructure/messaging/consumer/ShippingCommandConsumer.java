package com.ecommerce.shipping.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;

import com.ecommerce.shipping.application.event.ShippingCommandMessage;
import com.ecommerce.shipping.application.service.ShipmentProcessingService;
import com.ecommerce.shipping.domain.model.vo.Address;
import com.ecommerce.shipping.shared.constant.MessagingConstants;

@ApplicationScoped
public class ShippingCommandConsumer {

    private static final Logger LOG = Logger.getLogger(ShippingCommandConsumer.class);

    private final ShipmentProcessingService shipmentProcessingService;

    @Inject
    public ShippingCommandConsumer(ShipmentProcessingService shipmentProcessingService) {
        this.shipmentProcessingService = shipmentProcessingService;
    }

    @Incoming("shipping-commands-in")
    public Uni<Void> onCommand(ShippingCommandMessage command) {
        return switch (command.commandType()) {
            case MessagingConstants.COMMAND_TYPE_CREATE_SHIPMENT -> shipmentProcessingService
                .createShipment(command.eventId(), command.orderId(), toAddress(command));
            case MessagingConstants.COMMAND_TYPE_CANCEL_SHIPMENT -> shipmentProcessingService
                .cancelShipment(command.eventId(), command.orderId());
            default -> {
                LOG.warnf("Unknown shipping command type: %s", command.commandType());
                yield Uni.createFrom().voidItem();
            }
        };
    }

    private Address toAddress(ShippingCommandMessage command) {
        var payload = command.shippingAddress();
        return new Address(payload.street(), payload.city(), payload.state(), payload.zip(),
            payload.country());
    }
}
