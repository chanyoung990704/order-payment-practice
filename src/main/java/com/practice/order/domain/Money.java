package com.practice.order.domain;

import java.util.Objects;

public record Money(long amount) {

    public Money {
        if (amount < 0) {
            throw new IllegalArgumentException("금액은 음수일 수 없습니다.");
        }
    }

    public static Money zero() {
        return new Money(0);
    }

    public Money plus(Money other) {
        return new Money(Math.addExact(amount, other.amount));
    }

    public Money minus(Money other) {
        if (amount < other.amount) {
            throw new IllegalArgumentException("잔액이 부족합니다.");
        }
        return new Money(amount - other.amount);
    }

    public boolean isLessThan(Money other) {
        return amount < other.amount;
    }
}
