package com.practice.order.controller;

import com.practice.order.dto.AccountResponse;
import com.practice.order.dto.OrderResponse;
import com.practice.order.dto.PaymentRequest;
import com.practice.order.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/orders/{orderId}/payments")
    public OrderResponse pay(@PathVariable String orderId,
                             @Valid @RequestBody PaymentRequest request) {
        return paymentService.pay(orderId, request);
    }

    @PostMapping("/orders/{orderId}/cancel")
    public OrderResponse cancel(@PathVariable String orderId,
                                @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return paymentService.cancel(orderId, idempotencyKey);
    }

    @GetMapping("/accounts/{accountId}")
    public AccountResponse getAccount(@PathVariable String accountId) {
        return paymentService.getAccount(accountId);
    }
}
