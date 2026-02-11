package com.arsiwooqq.paymentservice.handler;

import com.arsiwooqq.paymentservice.event.OrderCreatedEvent;
import com.arsiwooqq.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedEventHandler {

    private final PaymentService paymentService;

    @KafkaListener(topics = "ORDER_CREATED")
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.debug("Received OrderCreatedEvent: {}", event);
        paymentService.create(event);
    }
}
