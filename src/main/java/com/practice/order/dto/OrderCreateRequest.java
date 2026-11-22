package com.practice.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record OrderCreateRequest(
        @NotBlank String orderId,
        @NotBlank String userId,
        @NotBlank String productName,
        @Min(1) long amount
) {
}
