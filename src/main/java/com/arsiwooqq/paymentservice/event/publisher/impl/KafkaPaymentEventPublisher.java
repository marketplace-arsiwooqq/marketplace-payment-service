package com.arsiwooqq.paymentservice.event.publisher.impl;

import com.arsiwooqq.paymentservice.event.PaymentCreatedEvent;
import com.arsiwooqq.paymentservice.event.publisher.PaymentEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaPaymentEventPublisher implements PaymentEventPublisher {

    @Value("${kafka.producer.topics.payment-created.name}")
    private String topicName;

    private final KafkaTemplate<String, PaymentCreatedEvent> kafkaTemplate;

    @Override
    public void publishPaymentCreated(PaymentCreatedEvent event) {
        var future = kafkaTemplate.send(topicName, event.orderId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Error sending message to Kafka: {}", ex.getMessage());
            } else {
                log.debug("Message sent to Kafka: {}", result);
            }
        });
    }
}
