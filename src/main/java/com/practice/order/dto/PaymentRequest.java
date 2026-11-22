package com.practice.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record PaymentRequest(
        @NotBlank String accountId,
        @Min(1) long amount,
        @NotBlank String idempotencyKey
) {
}
