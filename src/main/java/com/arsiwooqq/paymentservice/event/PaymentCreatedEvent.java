package com.arsiwooqq.paymentservice.event;

import com.arsiwooqq.paymentservice.enums.PaymentStatus;

public record PaymentCreatedEvent(
        String orderId,
        PaymentStatus status
) {}
