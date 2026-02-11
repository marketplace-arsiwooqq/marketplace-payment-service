package com.arsiwooqq.paymentservice.service;

import com.arsiwooqq.paymentservice.dto.PaymentRequest;
import com.arsiwooqq.paymentservice.enums.PaymentStatus;

public interface PaymentHandlerService {
    PaymentStatus handlePayment(PaymentRequest paymentCompleted);
}
