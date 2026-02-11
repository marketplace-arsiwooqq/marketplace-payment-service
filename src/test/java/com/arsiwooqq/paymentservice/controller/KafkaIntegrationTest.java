package com.arsiwooqq.paymentservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.arsiwooqq.paymentservice.enums.PaymentStatus;
import com.arsiwooqq.paymentservice.event.OrderCreatedEvent;
import com.arsiwooqq.paymentservice.event.PaymentCreatedEvent;
import com.arsiwooqq.paymentservice.repository.PaymentRepository;
import com.arsiwooqq.paymentservice.service.PaymentService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@WireMockTest
public class KafkaIntegrationTest extends AbstractIntegrationTest {
    @Container
    private static final KafkaContainer kafka = new KafkaContainer("apache/kafka:latest");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("kafka.producer.topics.payment-created.replicas", () -> 1);
        registry.add("kafka.producer.topics.payment-created.min-insync", () -> 1);
        registry.add("kafka.consumer.trusted-packages", () -> "*");
        registry.add("kafka.consumer.auto-offset-reset", () -> "earliest");

        registry.add("random-number-api.url", () -> wireMockServer.baseUrl() + "/api/v1.0/random");
    }

    @RegisterExtension
    static WireMockExtension wireMockServer = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private PaymentRepository repository;

    @Autowired
    private ConsumerFactory<String, Object> consumerFactory;

    @Autowired
    private PaymentService paymentService;

    @Test
    @DisplayName("Should handle ORDER_CREATED event")
    void givenOrderCreatedEvent_whenHandle_thenCreatePayment() throws JsonProcessingException {
        String topic = "ORDER_CREATED";
        var orderId = UUID.randomUUID().toString();
        kafkaTemplate.send(topic, new OrderCreatedEvent(orderId, UUID.randomUUID().toString(), 100L));

        wireMockServer.stubFor(WireMock.get(urlPathMatching("/api/v1.0/random.*")).willReturn(
                aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(List.of(0)))
        ));

        Awaitility.await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    assert !repository.findByOrderId(orderId).isEmpty();
                    var entity = repository.findByOrderId(orderId).get(0);
                    assertThat(entity).isNotNull();
                    assertThat(entity.getOrderId()).isEqualTo(orderId);
                    assertThat(entity.getStatus()).isEqualTo(PaymentStatus.PAID);
                });
    }

    @Test
    @DisplayName("Should produce PAYMENT_CREATED event")
    void givenPaymentCreatedEvent_whenProduce_thenSendsToKafka() throws JsonProcessingException {
        Consumer<String, Object> consumer = consumerFactory.createConsumer("TEST_GROUP", "TEST_CLIENT");
        consumer.subscribe(Collections.singleton("PAYMENT_CREATED"));

        wireMockServer.stubFor(WireMock.get(urlPathMatching("/api/v1.0/random.*")).willReturn(
                aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(List.of(0)))
        ));

        var event = new OrderCreatedEvent(UUID.randomUUID().toString(), UUID.randomUUID().toString(), 100L);
        paymentService.create(event);

        ConsumerRecord<String, Object> record =
                KafkaTestUtils.getSingleRecord(consumer, "PAYMENT_CREATED", Duration.ofSeconds(10));

        assertEquals(event.orderId(), ((PaymentCreatedEvent) record.value()).orderId());
    }
}
