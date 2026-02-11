package com.arsiwooqq.paymentservice.controller;

import com.arsiwooqq.paymentservice.entity.Payment;
import com.arsiwooqq.paymentservice.enums.PaymentStatus;
import com.arsiwooqq.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc(addFilters = false)
public class PaymentControllerTest extends AbstractIntegrationTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    protected KafkaAdmin kafkaAdmin;

    @MockitoBean
    protected KafkaListenerEndpointRegistry registry;

    @BeforeEach
    void clearRepositories() {
        paymentRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return all payments when no filters are provided")
    void givenNoFilters_whenSearch_thenReturnsAllPayments() throws Exception {
        createAndSavePayment(UUID.randomUUID().toString(), UUID.randomUUID().toString(), PaymentStatus.PAID);
        createAndSavePayment(UUID.randomUUID().toString(), UUID.randomUUID().toString(), PaymentStatus.FAILED);
        createAndSavePayment(UUID.randomUUID().toString(), UUID.randomUUID().toString(), PaymentStatus.PAID);

        mockMvc.perform(get(URI.SEARCH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(3)))
                .andExpect(jsonPath("$.data.totalElements").value(3));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should filter payments by orderId")
    void givenOrderId_whenSearch_thenReturnsFilteredPayments() throws Exception {
        var targetOrderId = UUID.randomUUID().toString();
        createAndSavePayment(targetOrderId, UUID.randomUUID().toString(), PaymentStatus.PAID);
        createAndSavePayment(UUID.randomUUID().toString(), UUID.randomUUID().toString(), PaymentStatus.PAID);

        mockMvc.perform(get(URI.SEARCH)
                        .param("orderId", targetOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].orderId").value(targetOrderId));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should filter payments by userId")
    void givenUserId_whenSearch_thenReturnsFilteredPayments() throws Exception {
        String targetUser = UUID.randomUUID().toString();
        createAndSavePayment(UUID.randomUUID().toString(), targetUser, PaymentStatus.PAID);
        createAndSavePayment(UUID.randomUUID().toString(), targetUser, PaymentStatus.FAILED);
        createAndSavePayment(UUID.randomUUID().toString(), UUID.randomUUID().toString(), PaymentStatus.PAID);

        mockMvc.perform(get(URI.SEARCH)
                        .param("userId", targetUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].userId").value(targetUser))
                .andExpect(jsonPath("$.data.content[1].userId").value(targetUser));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should filter payments by status")
    void givenStatus_whenSearch_thenReturnsPaymentsWithStatus() throws Exception {
        createAndSavePayment(UUID.randomUUID().toString(), UUID.randomUUID().toString(), PaymentStatus.PAID);
        createAndSavePayment(UUID.randomUUID().toString(), UUID.randomUUID().toString(), PaymentStatus.FAILED);
        createAndSavePayment(UUID.randomUUID().toString(), UUID.randomUUID().toString(), PaymentStatus.PAID);

        mockMvc.perform(get(URI.SEARCH)
                        .param("statuses", "PAID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return empty page if nothing matches filters")
    void givenFilters_whenSearch_thenReturnsEmpty() throws Exception {
        createAndSavePayment(UUID.randomUUID().toString(), UUID.randomUUID().toString(), PaymentStatus.PAID);

        mockMvc.perform(get(URI.SEARCH)
                        .param("orderId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should apply complex filter")
    void givenUserAndStatus_whenSearch_thenReturnsExactMatch() throws Exception {
        String targetUser = UUID.randomUUID().toString();
        createAndSavePayment(UUID.randomUUID().toString(), targetUser, PaymentStatus.PAID);
        createAndSavePayment(UUID.randomUUID().toString(), targetUser, PaymentStatus.FAILED);
        createAndSavePayment(UUID.randomUUID().toString(), UUID.randomUUID().toString(), PaymentStatus.PAID);

        mockMvc.perform(get(URI.SEARCH)
                        .param("userId", targetUser)
                        .param("statuses", "PAID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].userId").value(targetUser))
                .andExpect(jsonPath("$.data.content[0].status").value("PAID"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return summary for date period")
    void givenDatePeriod_whenGetSummary_thenReturnsSummary() throws Exception {
        var from = Instant.now().minus(1, ChronoUnit.DAYS);
        var to = Instant.now();

        createAndSavePayment(UUID.randomUUID().toString(), UUID.randomUUID().toString(), PaymentStatus.PAID,
                Instant.now().minus(2, ChronoUnit.DAYS));
        createAndSavePayment(UUID.randomUUID().toString(), UUID.randomUUID().toString(), PaymentStatus.PAID,
                to.minus(1, ChronoUnit.HOURS));
        createAndSavePayment(UUID.randomUUID().toString(), UUID.randomUUID().toString(), PaymentStatus.PAID,
                to.minus(23, ChronoUnit.HOURS));
        createAndSavePayment(UUID.randomUUID().toString(), UUID.randomUUID().toString(), PaymentStatus.PAID,
                to.plus(400, ChronoUnit.HOURS));
        createAndSavePayment(UUID.randomUUID().toString(), UUID.randomUUID().toString(), PaymentStatus.FAILED,
                to.minus(1, ChronoUnit.HOURS));

        mockMvc.perform(get(URI.SUMMARY)
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(200L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 0 if payments not found")
    void givenNoPayments_whenGetSummary_thenReturnsZero() throws Exception {
        var from = Instant.now().minus(1, ChronoUnit.DAYS);
        var to = Instant.now();

        createAndSavePayment(UUID.randomUUID().toString(), UUID.randomUUID().toString(), PaymentStatus.PAID,
                Instant.now().minus(2, ChronoUnit.DAYS));
        createAndSavePayment(UUID.randomUUID().toString(), UUID.randomUUID().toString(), PaymentStatus.PAID,
                to.minus(3, ChronoUnit.DAYS));
        createAndSavePayment(UUID.randomUUID().toString(), UUID.randomUUID().toString(), PaymentStatus.PAID,
                to.minus(400, ChronoUnit.HOURS));
        createAndSavePayment(UUID.randomUUID().toString(), UUID.randomUUID().toString(), PaymentStatus.PAID,
                to.plus(400, ChronoUnit.HOURS));
        createAndSavePayment(UUID.randomUUID().toString(), UUID.randomUUID().toString(), PaymentStatus.FAILED,
                to.minus(1, ChronoUnit.HOURS));

        mockMvc.perform(get(URI.SUMMARY)
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0L));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should handle AccessDeniedException when user has user role")
    void givenUserWithUserRole_whenAccessProtectedEndpoint_thenHandleAccessDeniedException() throws Exception {
        mockMvc.perform(get(URI.SEARCH))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should handle AuthenticationException when user is not authenticated")
    void givenNoUser_whenAccessProtectedEndpoint_thenHandleAuthenticationException() throws Exception {
        mockMvc.perform(get(URI.SEARCH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication failed"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should handle HttpRequestMethodNotSupportedException when using unsupported HTTP method")
    void givenUnsupportedHttpMethod_whenAccessEndpoint_thenHandleHttpRequestMethodNotSupportedException() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(URI.SEARCH))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should handle NoResourceFoundException when accessing non-existent endpoint")
    void givenNonExistentEndpoint_whenAccess_thenHandleNoResourceFoundException() throws Exception {
        mockMvc.perform(get("/api/v1/payments/non-existent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should handle MissingServletRequestParameterException when required parameter is missing")
    void givenMissingRequiredParameter_whenGetSummary_thenHandleMissingServletRequestParameterException() throws Exception {
        mockMvc.perform(get(URI.SUMMARY)
                        .param("from", Instant.now().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    private void createAndSavePayment(String orderId, String userId, PaymentStatus status) {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .userId(userId)
                .status(status)
                .paymentAmount(100L)
                .timestamp(Instant.now())
                .build();

        paymentRepository.save(payment);
    }

    private void createAndSavePayment(String orderId, String userId, PaymentStatus status, Instant instant) {
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .userId(userId)
                .status(status)
                .paymentAmount(100L)
                .timestamp(instant)
                .build();

        paymentRepository.save(payment);
    }

    private static class URI {
        static final String SEARCH = "/api/v1/payments/search";
        static final String SUMMARY = "/api/v1/payments/summary";
    }
}
