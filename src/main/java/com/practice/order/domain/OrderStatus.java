package com.practice.order.domain;

public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAID,
    CANCELLED;

    public boolean canPay() {
        return this == CREATED || this == PAYMENT_PENDING;
    }

    public boolean canCancel() {
        return this == PAID;
    }
}
