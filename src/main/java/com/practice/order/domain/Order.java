package com.practice.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @Column(name = "order_id", nullable = false, updatable = false, length = 64)
    private String orderId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "product_name", nullable = false, length = 128)
    private String productName;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "payment_id", length = 128)
    private String paymentId;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Order() {
    }

    private Order(String orderId, String userId, String productName, Money amount,
                  String idempotencyKey, Instant now) {
        this.orderId = Objects.requireNonNull(orderId);
        this.userId = Objects.requireNonNull(userId);
        this.productName = Objects.requireNonNull(productName);
        this.amount = amount.amount();
        this.status = OrderStatus.CREATED;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Order create(String orderId, String userId, String productName,
                               Money amount, String idempotencyKey) {
        return new Order(orderId, userId, productName, amount, idempotencyKey, Instant.now());
    }

    public void markPaymentPending() {
        if (!status.canPay()) {
            throw new IllegalStateException("결제 가능한 상태가 아닙니다: " + status);
        }
        this.status = OrderStatus.PAYMENT_PENDING;
        this.updatedAt = Instant.now();
    }

    public void completePayment(String paymentId) {
        if (!status.canPay()) {
            throw new IllegalStateException("결제 완료 처리할 수 없는 상태입니다: " + status);
        }
        this.status = OrderStatus.PAID;
        this.paymentId = Objects.requireNonNull(paymentId);
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        if (!status.canCancel()) {
            throw new IllegalStateException("취소할 수 없는 상태입니다: " + status);
        }
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public String getOrderId() {
        return orderId;
    }

    public String getUserId() {
        return userId;
    }

    public String getProductName() {
        return productName;
    }

    public Money getAmount() {
        return new Money(amount);
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public long getVersion() {
        return version;
    }
}
