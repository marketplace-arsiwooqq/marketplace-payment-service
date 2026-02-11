package com.arsiwooqq.paymentservice.dto;

import com.arsiwooqq.paymentservice.enums.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String orderId,
        String userId,
        PaymentStatus status,
        Instant timestamp,
        Long paymentAmount
) {
}
