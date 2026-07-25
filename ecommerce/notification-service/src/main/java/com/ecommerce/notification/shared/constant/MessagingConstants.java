package com.ecommerce.notification.shared.constant;

/** Nombres de tópicos y discriminadores de tipo de evento/comando que este servicio produce o consume. */
public final class MessagingConstants {

    public static final String NOTIFICATION_EVENTS_TOPIC = "ecommerce.notification.events.v1";
    public static final String NOTIFICATION_COMMANDS_TOPIC = "ecommerce.notification.commands.v1";

    public static final String COMMAND_TYPE_SEND_NOTIFICATION = "SendNotification";

    public static final String EVENT_TYPE_NOTIFICATION_SENT = "NotificationSent";
    public static final String EVENT_TYPE_NOTIFICATION_FAILED = "NotificationFailed";

    /** Disparador determinista de fallo para la pasarela de notificación simulada. */
    public static final String FAILURE_SENTINEL_RECIPIENT = "fail@example.com";

    private MessagingConstants() {
    }
}
