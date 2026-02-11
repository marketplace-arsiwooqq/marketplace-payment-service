package com.arsiwooqq.paymentservice.service;

import com.arsiwooqq.paymentservice.dto.PaymentResponse;
import com.arsiwooqq.paymentservice.event.OrderCreatedEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface PaymentService {

    Page<PaymentResponse> search(Pageable pageable, String orderId, String userId, List<String> statuses);

    Long getTotalAmountOfPaidInPeriod(Instant from, Instant to);

    void create(OrderCreatedEvent event);
}
