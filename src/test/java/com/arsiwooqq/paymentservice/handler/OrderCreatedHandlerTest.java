package com.arsiwooqq.paymentservice.handler;

import com.arsiwooqq.paymentservice.event.OrderCreatedEvent;
import com.arsiwooqq.paymentservice.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderCreatedHandlerTest {
    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private OrderCreatedEventHandler orderCreatedEventHandler;

    @Test
    @DisplayName("Should create payment when OrderCreatedEvent is provided")
    void givenOrderCreatedEvent_whenHandleOrderCreatedEvent_thenCreatesPayment() {
        // Given
        var event = new OrderCreatedEvent(UUID.randomUUID().toString(), UUID.randomUUID().toString(), 10L);
        var capture = ArgumentCaptor.forClass(OrderCreatedEvent.class);

        // When
        orderCreatedEventHandler.handleOrderCreatedEvent(event);

        // Then
        verify(paymentService, times(1)).create(capture.capture());
        assertAll(
                () -> assertEquals(event, capture.getValue())
        );
    }
}
