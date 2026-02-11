package com.arsiwooqq.paymentservice.service.impl;

import com.arsiwooqq.paymentservice.dto.PaymentRequest;
import com.arsiwooqq.paymentservice.enums.PaymentStatus;
import com.arsiwooqq.paymentservice.event.PaymentCreatedEvent;
import com.arsiwooqq.paymentservice.event.publisher.PaymentEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RandomPaymentHandlerServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient webClient;

    @Mock(answer = Answers.RETURNS_SELF)
    private WebClient.Builder webClientBuilder;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    private RandomPaymentHandlerService paymentHandlerService;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        paymentHandlerService = new RandomPaymentHandlerService(webClientBuilder, paymentEventPublisher);
        ReflectionTestUtils.setField(paymentHandlerService, "url", "http://test-api.com");
    }

    @Test
    @DisplayName("Should return PAID Payment Status when random number is even")
    void givenEvenNumber_whenHandlePayment_thenReturnPaid() {
        // Given
        var request = new PaymentRequest(UUID.randomUUID().toString(), UUID.randomUUID().toString(), 10L);
        var evenResult = List.of(0L);
        var captor = ArgumentCaptor.forClass(PaymentCreatedEvent.class);

        // When
        when(webClient.get()
                .uri(anyString())
                .retrieve()
                .bodyToMono(ArgumentMatchers.<ParameterizedTypeReference<List<Long>>>any())
        ).thenReturn(Mono.just(evenResult));

        var result = paymentHandlerService.handlePayment(request);

        // Then
        assertEquals(PaymentStatus.PAID, result);
        verify(paymentEventPublisher, times(1)).publishPaymentCreated(captor.capture());
        assertAll(
                () -> assertEquals(request.orderId(), captor.getValue().orderId()),
                () -> assertEquals(PaymentStatus.PAID, captor.getValue().status())
        );
    }

    @Test
    @DisplayName("Should return FAILED Payment Status when random number is odd")
    void givenOddNumber_whenHandlePayment_thenReturnFailed() {
        // Given
        var request = new PaymentRequest(UUID.randomUUID().toString(), UUID.randomUUID().toString(), 10L);
        var oddResult = List.of(1L);
        var captor = ArgumentCaptor.forClass(PaymentCreatedEvent.class);

        // When
        when(webClient.get()
                .uri(anyString())
                .retrieve()
                .bodyToMono(ArgumentMatchers.<ParameterizedTypeReference<List<Long>>>any())
        ).thenReturn(Mono.just(oddResult));

        var result = paymentHandlerService.handlePayment(request);

        // Then
        assertEquals(PaymentStatus.FAILED, result);
        verify(paymentEventPublisher, times(1)).publishPaymentCreated(captor.capture());
        assertAll(
                () -> assertEquals(request.orderId(), captor.getValue().orderId()),
                () -> assertEquals(PaymentStatus.FAILED, captor.getValue().status())
        );
    }
}
