package com.arsiwooqq.paymentservice.service.impl;

import com.arsiwooqq.paymentservice.dto.PaymentRequest;
import com.arsiwooqq.paymentservice.dto.PaymentResponse;
import com.arsiwooqq.paymentservice.entity.Payment;
import com.arsiwooqq.paymentservice.enums.PaymentStatus;
import com.arsiwooqq.paymentservice.event.OrderCreatedEvent;
import com.arsiwooqq.paymentservice.mapper.PaymentMapper;
import com.arsiwooqq.paymentservice.repository.PaymentRepository;
import com.arsiwooqq.paymentservice.service.PaymentHandlerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private PaymentHandlerService paymentHandlerService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    @DisplayName("Should return all payments when no filters are provided")
    void givenNoFilters_whenSearch_thenReturnsAllPayments() {
        // Given
        var payment = getPayment();
        var response = getPaymentResponse(payment);
        var pageable = PageRequest.of(0, 10);
        var paymentList = List.of(payment);

        // When
        when(mongoTemplate.count(any(Query.class), eq(Payment.class))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(paymentList);
        when(paymentMapper.toResponse(payment)).thenReturn(response);

        var result = paymentService.search(pageable, null, null, null);

        // Then
        Assertions.assertAll(
                () -> assertEquals(1, result.getTotalElements()),
                () -> assertEquals(1, result.getContent().size())
        );
        verify(mongoTemplate, times(1)).count(any(Query.class), eq(Payment.class));
        verify(mongoTemplate, times(1)).find(any(Query.class), eq(Payment.class));
        verify(paymentMapper, times(1)).toResponse(payment);
    }

    @Test
    @DisplayName("Should filter payments by orderId when only orderId is provided")
    void givenOrderId_whenSearch_thenReturnsFilteredPayments() {
        // Given
        var orderId = UUID.randomUUID().toString();
        var pageable = PageRequest.of(0, 10);
        var captor = ArgumentCaptor.forClass(Query.class);

        // When
        when(mongoTemplate.count(any(Query.class), eq(Payment.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(List.of());

        var result = paymentService.search(pageable, orderId, null, null);

        // Then
        verify(mongoTemplate).count(captor.capture(), eq(Payment.class));
        var queryObj = captor.getValue().getQueryObject();

        Assertions.assertAll(
                () -> assertEquals(0, result.getTotalElements()),
                () -> assertEquals(orderId, queryObj.get("orderId"))
        );
    }

    @Test
    @DisplayName("Should filter payments by userId when only userId is provided")
    void givenUserId_whenSearch_thenReturnsFilteredPayments() {
        // Given
        var userId = UUID.randomUUID().toString();
        var pageable = PageRequest.of(0, 10);
        var captor = ArgumentCaptor.forClass(Query.class);

        // When
        when(mongoTemplate.count(any(Query.class), eq(Payment.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(List.of());

        var result = paymentService.search(pageable, null, userId, List.of());

        // Then
        verify(mongoTemplate).count(captor.capture(), eq(Payment.class));
        var queryObj = captor.getValue().getQueryObject();

        Assertions.assertAll(
                () -> assertEquals(0, result.getTotalElements()),
                () -> assertEquals(userId, queryObj.get("userId"))
        );
    }

    @Test
    @DisplayName("Should filter payments by statuses when list of statuses is provided")
    void givenStatusesList_whenSearch_thenReturnsPaymentsWithCorrectStatus() {
        // Given
        var statuses = List.of("PAID");
        var pageable = PageRequest.of(0, 10);
        var captor = ArgumentCaptor.forClass(Query.class);

        // When
        when(mongoTemplate.count(any(Query.class), eq(Payment.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(List.of());

        var result = paymentService.search(pageable, null, null, statuses);

        // Then
        verify(mongoTemplate).count(captor.capture(), eq(Payment.class));
        var queryObject = captor.getValue().getQueryObject();

        Assertions.assertAll(
                () -> assertEquals(0, result.getTotalElements()),
                () -> assertTrue(queryObject.containsKey("status")),
                () -> assertTrue(queryObject.get("status").toString().contains("$in"))
        );
    }

    @Test
    @DisplayName("Should apply all filters combined when all parameters are provided")
    void givenAllFilters_whenSearch_thenReturnsHighlyRefinedResults() {
        // Given
        var orderId = UUID.randomUUID().toString();
        var userId = UUID.randomUUID().toString();
        var statuses = List.of("PAID");
        var pageable = PageRequest.of(0, 10);
        var captor = ArgumentCaptor.forClass(Query.class);

        // When
        when(mongoTemplate.count(any(Query.class), eq(Payment.class))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(List.of());

        var result = paymentService.search(pageable, orderId, userId, statuses);

        // Then
        verify(mongoTemplate).count(captor.capture(), eq(Payment.class));
        var queryString = captor.getValue().toString();

        Assertions.assertAll(
                () -> assertEquals(0, result.getTotalElements()),
                () -> assertTrue(queryString.contains("orderId")),
                () -> assertTrue(queryString.contains("userId")),
                () -> assertTrue(queryString.contains("status"))
        );
    }

    @Test
    @DisplayName("Should return total amount of paid payments in period")
    void givenPeriod_whenGetTotalAmountOfPaidInPeriod_thenReturnsTotalAmount() {
        // Given
        var from = Instant.now().minus(1, ChronoUnit.DAYS);
        var to = Instant.now();

        // When
        when(paymentRepository.sumOfPayments(from, to, PaymentStatus.PAID)).thenReturn(100L);

        var result = paymentService.getTotalAmountOfPaidInPeriod(from, to);

        // Then
        assertEquals(100L, result);
        verify(paymentRepository, times(1)).sumOfPayments(from, to, PaymentStatus.PAID);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    @DisplayName("Should create payment when event is provided")
    void givenEvent_whenCreate_thenCreatesPayment() {
        // Given
        var event = new OrderCreatedEvent(UUID.randomUUID().toString(), UUID.randomUUID().toString(), 10L);
        var request = new PaymentRequest(event.orderId(), event.userId(), event.paymentAmount());
        var capture = ArgumentCaptor.forClass(Payment.class);

        // When
        when(paymentHandlerService.handlePayment(request)).thenReturn(PaymentStatus.PAID);

        paymentService.create(event);

        // Then
        verify(paymentHandlerService, times(1)).handlePayment(request);
        verify(paymentRepository, times(1)).save(capture.capture());
        var payment = capture.getValue();
        assertAll(
                () -> assertEquals(event.orderId(), payment.getOrderId()),
                () -> assertEquals(event.userId(), payment.getUserId()),
                () -> assertEquals(PaymentStatus.PAID, payment.getStatus()),
                () -> assertNotNull(payment.getTimestamp()),
                () -> assertEquals(event.paymentAmount(), payment.getPaymentAmount())
        );
    }

    private Payment getPayment() {
        return Payment.builder()
                .id(UUID.randomUUID())
                .orderId("orderId")
                .userId("userId")
                .status(PaymentStatus.PAID)
                .timestamp(Instant.now())
                .paymentAmount(100L)
                .build();
    }

    private PaymentResponse getPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getStatus(),
                payment.getTimestamp(),
                payment.getPaymentAmount()
        );
    }
}
