package com.practice.order;

import com.practice.order.domain.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void 잔액보다_큰_금액을_차감하면_예외가_발생한다() {
        Money balance = new Money(1000);

        assertThatThrownBy(() -> balance.minus(new Money(1001)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잔액이 부족");
    }

    @Test
    void 차감과_추가가_정확하다() {
        Money balance = new Money(1000);

        assertThat(balance.minus(new Money(300)).amount()).isEqualTo(700);
        assertThat(balance.plus(new Money(300)).amount()).isEqualTo(1300);
    }
}
