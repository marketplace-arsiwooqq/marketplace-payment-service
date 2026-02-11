package com.arsiwooqq.paymentservice.entity;

import com.arsiwooqq.paymentservice.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document
@Builder
@Getter
@Setter
public class Payment {
    @Id
    private UUID id;

    @Indexed
    private String orderId;

    @Indexed
    private String userId;

    @Indexed
    private PaymentStatus status;

    @Indexed
    private Instant timestamp;

    private Long paymentAmount;
}
