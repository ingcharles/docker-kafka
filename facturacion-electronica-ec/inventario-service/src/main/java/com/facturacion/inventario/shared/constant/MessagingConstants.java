package com.facturacion.inventario.shared.constant;

public final class MessagingConstants {

    public static final String INVENTARIO_EVENTS_TOPIC = "facturacion.inventario.eventos.v1";

    public static final String COMANDO_RESERVAR = "RESERVAR";
    public static final String COMANDO_CONFIRMAR = "CONFIRMAR";
    public static final String COMANDO_LIBERAR = "LIBERAR";

    public static final String EVENT_TYPE_STOCK_RESERVADO = "StockReservado";
    public static final String EVENT_TYPE_STOCK_NO_DISPONIBLE = "StockNoDisponible";
    public static final String EVENT_TYPE_STOCK_CONFIRMADO = "StockConfirmado";
    public static final String EVENT_TYPE_STOCK_LIBERADO = "StockLiberado";

    // Tópico separado del de stock a propósito: el catálogo (con su
    // clasificación tributaria) y las reservas de stock son dos flujos de
    // datos completamente distintos con consumidores distintos —
    // comprobantes-service solo necesita el catálogo, nunca las reservas.
    public static final String ITEM_EVENTS_TOPIC = "facturacion.inventario.item.eventos.v1";
    public static final String EVENT_TYPE_ITEM_CREADO = "ItemCreado";
    public static final String EVENT_TYPE_ITEM_ACTUALIZADO = "ItemActualizado";

    private MessagingConstants() {
    }
}
