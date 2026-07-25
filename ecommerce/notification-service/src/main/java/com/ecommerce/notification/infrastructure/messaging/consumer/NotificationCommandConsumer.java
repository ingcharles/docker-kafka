package com.ecommerce.notification.infrastructure.messaging.consumer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;

import com.ecommerce.notification.application.event.NotificationCommandMessage;
import com.ecommerce.notification.application.service.NotificationSendingService;
import com.ecommerce.notification.shared.constant.MessagingConstants;

@ApplicationScoped
public class NotificationCommandConsumer {

    private static final Logger LOG = Logger.getLogger(NotificationCommandConsumer.class);

    private final NotificationSendingService notificationSendingService;

    @Inject
    public NotificationCommandConsumer(NotificationSendingService notificationSendingService) {
        this.notificationSendingService = notificationSendingService;
    }

    @Incoming("notification-commands-in")
    public Uni<Void> onCommand(NotificationCommandMessage command) {
        if (!MessagingConstants.COMMAND_TYPE_SEND_NOTIFICATION.equals(command.commandType())) {
            LOG.warnf("Unknown notification command type: %s", command.commandType());
            return Uni.createFrom().voidItem();
        }
        return notificationSendingService.sendNotification(command.eventId(), command.orderId(),
            command.notificationType(), command.recipient());
    }
}
