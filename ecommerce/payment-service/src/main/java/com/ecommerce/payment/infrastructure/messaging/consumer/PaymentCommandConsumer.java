package com.ecommerce.payment.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;

import com.ecommerce.payment.application.event.PaymentCommandMessage;
import com.ecommerce.payment.application.service.PaymentProcessingService;
import com.ecommerce.payment.shared.constant.MessagingConstants;

@ApplicationScoped
public class PaymentCommandConsumer {

    private static final Logger LOG = Logger.getLogger(PaymentCommandConsumer.class);

    private final PaymentProcessingService paymentProcessingService;

    @Inject
    public PaymentCommandConsumer(PaymentProcessingService paymentProcessingService) {
        this.paymentProcessingService = paymentProcessingService;
    }

    @Incoming("payment-commands-in")
    public Uni<Void> onCommand(PaymentCommandMessage command) {
        return switch (command.commandType()) {
            case MessagingConstants.COMMAND_TYPE_CHARGE_PAYMENT -> paymentProcessingService
                .chargePayment(command.eventId(), command.orderId(), command.amount(), command.currency());
            case MessagingConstants.COMMAND_TYPE_REFUND_PAYMENT -> paymentProcessingService
                .refundPayment(command.eventId(), command.orderId());
            default -> {
                LOG.warnf("Unknown payment command type: %s", command.commandType());
                yield Uni.createFrom().voidItem();
            }
        };
    }
}
