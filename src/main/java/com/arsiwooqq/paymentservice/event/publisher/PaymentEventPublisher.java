package com.arsiwooqq.paymentservice.event.publisher;

import com.arsiwooqq.paymentservice.event.PaymentCreatedEvent;

public interface PaymentEventPublisher {
    void publishPaymentCreated(PaymentCreatedEvent event);
}
