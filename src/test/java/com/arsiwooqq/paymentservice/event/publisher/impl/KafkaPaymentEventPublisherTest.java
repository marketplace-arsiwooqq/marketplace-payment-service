package com.arsiwooqq.paymentservice.event.publisher.impl;

import com.arsiwooqq.paymentservice.enums.PaymentStatus;
import com.arsiwooqq.paymentservice.event.PaymentCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KafkaPaymentEventPublisherTest {
    @Mock
    private KafkaTemplate<String, PaymentCreatedEvent> kafkaTemplate;

    @Mock
    private SendResult<String, PaymentCreatedEvent> sendResult;

    @InjectMocks
    private KafkaPaymentEventPublisher kafkaPaymentEventPublisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(kafkaPaymentEventPublisher, "topicName", "PAYMENT_CREATED");
    }

    @Test
    @DisplayName("Should publish payment created event when no exceptions occurred")
    void givenNoExceptions_whenPublishPaymentCreated_thenPublish() {
        // Given
        var event = new PaymentCreatedEvent(UUID.randomUUID().toString(), PaymentStatus.PAID);
        var future = new CompletableFuture<SendResult<String, PaymentCreatedEvent>>();

        // When
        when(kafkaTemplate.send(anyString(), eq(event.orderId()), eq(event))).thenReturn(future);

        kafkaPaymentEventPublisher.publishPaymentCreated(event);

        future.complete(sendResult);

        // Then
        verify(kafkaTemplate, times(1)).send(anyString(), eq(event.orderId()), eq(event));
    }

    @Test
    @DisplayName("Should publish payment created event when no exceptions occurred")
    void givenException_whenPublishPaymentCreated_thenNotPublish() {
        // Given
        var event = new PaymentCreatedEvent(UUID.randomUUID().toString(), PaymentStatus.PAID);
        var future = new CompletableFuture<SendResult<String, PaymentCreatedEvent>>();
        var exception = new RuntimeException();

        // When
        when(kafkaTemplate.send(anyString(), eq(event.orderId()), eq(event))).thenReturn(future);

        kafkaPaymentEventPublisher.publishPaymentCreated(event);

        future.completeExceptionally(exception);

        // Then
        verify(kafkaTemplate, times(1)).send(anyString(), eq(event.orderId()), eq(event));
    }
}
