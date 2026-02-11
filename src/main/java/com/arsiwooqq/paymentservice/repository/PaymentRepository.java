package com.arsiwooqq.paymentservice.repository;

import com.arsiwooqq.paymentservice.entity.Payment;
import com.arsiwooqq.paymentservice.enums.PaymentStatus;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends MongoRepository<Payment, UUID> {

    @Aggregation(pipeline = {
            "{ $match: { timestamp: { $gte: ?0, $lte: ?1 }, status: ?2 } }",
            "{ $group: { _id: null, total: { $sum: '$paymentAmount' } } }"
    })
    Long sumOfPayments(Instant from, Instant to, PaymentStatus status);

    List<Payment> findByOrderId(String orderId);
}
