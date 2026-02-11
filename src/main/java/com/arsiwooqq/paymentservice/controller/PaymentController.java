package com.arsiwooqq.paymentservice.controller;

import com.arsiwooqq.paymentservice.dto.ApiResponse;
import com.arsiwooqq.paymentservice.dto.PaymentResponse;
import com.arsiwooqq.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> search(
            Pageable pageable,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) List<String> statuses
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Payments found",
                paymentService.search(pageable, orderId, userId, statuses))
        );
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Long>> getTotalAmountOfPaidInPeriod(
            @RequestParam Instant from,
            @RequestParam Instant to
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Total amount of paid payments in period",
                paymentService.getTotalAmountOfPaidInPeriod(from, to)
        ));
    }
}
