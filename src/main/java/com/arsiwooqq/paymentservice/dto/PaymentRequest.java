package com.arsiwooqq.paymentservice.dto;

public record PaymentRequest(
        String orderId,
        String userId,
        Long paymentAmount
) {
}
