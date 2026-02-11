package com.arsiwooqq.paymentservice.service.impl;

import com.arsiwooqq.paymentservice.dto.PaymentRequest;
import com.arsiwooqq.paymentservice.dto.PaymentResponse;
import com.arsiwooqq.paymentservice.entity.Payment;
import com.arsiwooqq.paymentservice.enums.PaymentStatus;
import com.arsiwooqq.paymentservice.event.OrderCreatedEvent;
import com.arsiwooqq.paymentservice.mapper.PaymentMapper;
import com.arsiwooqq.paymentservice.repository.PaymentRepository;
import com.arsiwooqq.paymentservice.service.PaymentHandlerService;
import com.arsiwooqq.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final MongoTemplate mongoTemplate;
    private final PaymentHandlerService paymentHandlerService;

    @Override
    public Page<PaymentResponse> search(Pageable pageable, String orderId, String userId, List<String> statuses) {
        log.debug("Searching payments with params: pageable={}, orderId={}, userId={}, statuses={}", pageable, orderId, userId, statuses);
        Query query = new Query();
        List<Criteria> criteria = new ArrayList<>();

        if (orderId != null) {
            criteria.add(Criteria.where("orderId").is(orderId));
        }
        if (userId != null) {
            criteria.add(Criteria.where("userId").is(userId));
        }
        if (statuses != null && !statuses.isEmpty()) {
            var enumStatuses = statuses.stream()
                    .map(PaymentStatus::fromString)
                    .toList();
            criteria.add(Criteria.where("status").in(enumStatuses));
        }
        criteria.forEach(query::addCriteria);
        long totalFiltered = mongoTemplate.count(query, Payment.class);
        query.with(pageable);
        var payments = mongoTemplate.find(query, Payment.class).stream().map(paymentMapper::toResponse).toList();
        log.debug("Found {} payments", payments.size());
        return new PageImpl<>(payments, pageable, totalFiltered);
    }

    @Override
    public Long getTotalAmountOfPaidInPeriod(Instant from, Instant to) {
        log.debug("Calculating total amount of paid payments in period: from={} to={}", from, to);
        var total = paymentRepository.sumOfPayments(from, to, PaymentStatus.PAID);
        var result = total != null ? total : 0;
        log.debug("Total amount of paid payments in period: {}", result);
        return result;
    }

    @Override
    public void create(OrderCreatedEvent event) {
        log.debug("Saving payment: {}", event);
        var status = paymentHandlerService.handlePayment(
                new PaymentRequest(
                        event.orderId(),
                        event.userId(),
                        event.paymentAmount())
        );
        log.debug("Payment handled with status: {}", status);
        var payment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(event.orderId())
                .userId(event.userId())
                .paymentAmount(event.paymentAmount())
                .status(status)
                .timestamp(Instant.now())
                .build();

        paymentRepository.save(payment);
        log.debug("Payment saved successfully: {}", payment);
    }
}
