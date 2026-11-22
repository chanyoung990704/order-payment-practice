package com.practice.order;

import com.practice.order.domain.Order;
import com.practice.order.domain.OrderStatus;
import com.practice.order.domain.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStatusTest {

    @Test
    void 주문은_생성_후_결제를_거쳐_완료된다() {
        Order order = Order.create("order-1", "user-1", "노트북",
                new Money(100_000), "idem-1");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        order.markPaymentPending();
        order.completePayment("pay-1");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPaymentId()).isEqualTo("pay-1");
    }

    @Test
    void 결제_완료된_주문은_다시_결제할_수_없다() {
        Order order = Order.create("order-1", "user-1", "노트북",
                new Money(100_000), "idem-1");
        order.completePayment("pay-1");

        assertThatThrownBy(() -> order.completePayment("pay-2"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 결제된_주문만_취소할_수_있다() {
        Order order = Order.create("order-1", "user-1", "노트북",
                new Money(100_000), "idem-1");

        assertThatThrownBy(order::cancel).isInstanceOf(IllegalStateException.class);

        order.completePayment("pay-1");
        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}
