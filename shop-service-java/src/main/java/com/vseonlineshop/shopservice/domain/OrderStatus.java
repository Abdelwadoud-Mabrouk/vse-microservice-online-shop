package com.vseonlineshop.shopservice.domain;

public enum OrderStatus {
    PENDING,
    AWAITING_PAYMENT,
    PROCESSING,
    COMPLETED,
    PAYMENT_FAILED,        // Zahlung abgelehnt (Provider) ODER Billing technisch down/Timeout
    OUT_OF_STOCK,           // Lager nicht ausreichend beim Reservieren
    ROLLBACK_COMPLETED,     // Ausbuchung schlägt fehl, Refund erfolgreich
    REFUND_PENDING,         // Refund selbst schlägt fehl, braucht manuelle Prüfung
    CANCELLED               // Kunde bricht AKTIV/BEWUSST ab, bevor Zahlung versucht wurde
}