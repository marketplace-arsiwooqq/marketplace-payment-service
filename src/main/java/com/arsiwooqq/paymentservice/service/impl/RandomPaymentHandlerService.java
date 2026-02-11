package com.arsiwooqq.paymentservice.service.impl;

import com.arsiwooqq.paymentservice.dto.PaymentRequest;
import com.arsiwooqq.paymentservice.enums.PaymentStatus;
import com.arsiwooqq.paymentservice.event.PaymentCreatedEvent;
import com.arsiwooqq.paymentservice.event.publisher.PaymentEventPublisher;
import com.arsiwooqq.paymentservice.service.PaymentHandlerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class RandomPaymentHandlerService implements PaymentHandlerService {

    private final WebClient webClient;
    private final PaymentEventPublisher paymentEventPublisher;

    public RandomPaymentHandlerService(WebClient.Builder builder, PaymentEventPublisher paymentEventPublisher) {
        this.webClient = builder.build();
        this.paymentEventPublisher = paymentEventPublisher;
    }

    @Value("${random-number-api.url}")
    private String url;

    @Override
    public PaymentStatus handlePayment(PaymentRequest request) {
        log.debug("Handling payment for order ID: {}", request.orderId());
        var status = getStatus();
        log.debug("Payment status determined: {}", status);
        
        if (status == PaymentStatus.PAID) {
            log.debug("Publishing PAID payment event for order ID: {}", request.orderId());
            paymentEventPublisher.publishPaymentCreated(
                    new PaymentCreatedEvent(
                            request.orderId(),
                            PaymentStatus.PAID)
            );
        } else {
            log.debug("Publishing FAILED payment event for order ID: {}", request.orderId());
            paymentEventPublisher.publishPaymentCreated(
                    new PaymentCreatedEvent(
                            request.orderId(),
                            PaymentStatus.FAILED)
            );
        }
        log.debug("Payment handling completed for order ID: {} with status: {}", request.orderId(), status);
        return status;
    }

    private PaymentStatus getStatus() {
        log.debug("Fetching random number from API: {}", url);
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Long>>() {
                })
                .doOnError(e -> log.error("Error fetching random number from API: {}", e.getMessage()))
                .map(randomNumber -> {
                    log.debug("Received random number: {}", randomNumber.get(0));
                    if (randomNumber.get(0) % 2 == 0) {
                        log.debug("Random number is even, returning PAID status");
                        return PaymentStatus.PAID;
                    } else {
                        log.debug("Random number is odd, returning FAILED status");
                        return PaymentStatus.FAILED;
                    }
                })
                .timeout(Duration.ofSeconds(5))
                .block();
    }
}
