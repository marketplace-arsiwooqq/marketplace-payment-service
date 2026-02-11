package com.arsiwooqq.paymentservice.enums;

public enum PaymentStatus {
    PAID,
    FAILED;

    public static PaymentStatus fromString(String status) {
        for (PaymentStatus value : PaymentStatus.values()) {
            if (value.name().equalsIgnoreCase(status)) {
                return value;
            }
        }
        return null;
    }
}
