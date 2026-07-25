package com.ecommerce.payment.shared.constant;

public final class MessagingConstants {

    public static final String PAYMENT_EVENTS_TOPIC = "ecommerce.payment.events.v1";
    public static final String PAYMENT_COMMANDS_TOPIC = "ecommerce.payment.commands.v1";

    public static final String COMMAND_TYPE_CHARGE_PAYMENT = "ChargePayment";
    public static final String COMMAND_TYPE_REFUND_PAYMENT = "RefundPayment";

    public static final String EVENT_TYPE_PAYMENT_APPROVED = "PaymentApproved";
    public static final String EVENT_TYPE_PAYMENT_DECLINED = "PaymentDeclined";
    public static final String EVENT_TYPE_PAYMENT_REFUNDED = "PaymentRefunded";

    private MessagingConstants() {
    }
}
