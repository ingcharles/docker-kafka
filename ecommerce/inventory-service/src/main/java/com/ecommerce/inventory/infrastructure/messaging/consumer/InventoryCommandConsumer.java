package com.ecommerce.inventory.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;

import com.ecommerce.inventory.application.event.InventoryCommandMessage;
import com.ecommerce.inventory.application.service.StockReservationService;
import com.ecommerce.inventory.shared.constant.MessagingConstants;

@ApplicationScoped
public class InventoryCommandConsumer {

    private static final Logger LOG = Logger.getLogger(InventoryCommandConsumer.class);

    private final StockReservationService stockReservationService;

    @Inject
    public InventoryCommandConsumer(StockReservationService stockReservationService) {
        this.stockReservationService = stockReservationService;
    }

    @Incoming("inventory-commands-in")
    public Uni<Void> onCommand(InventoryCommandMessage command) {
        return switch (command.commandType()) {
            case MessagingConstants.COMMAND_TYPE_RESERVE_STOCK -> stockReservationService
                .reserveStock(command.eventId(), command.orderId(), command.items());
            case MessagingConstants.COMMAND_TYPE_RELEASE_STOCK -> stockReservationService
                .releaseStock(command.eventId(), command.orderId());
            default -> {
                LOG.warnf("Unknown inventory command type: %s", command.commandType());
                yield Uni.createFrom().voidItem();
            }
        };
    }
}
