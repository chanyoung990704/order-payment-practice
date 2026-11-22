package com.practice.order;

import com.practice.order.domain.OrderStatus;
import com.practice.order.dto.OrderCreateRequest;
import com.practice.order.dto.OrderResponse;
import com.practice.order.dto.PaymentRequest;
import com.practice.order.exception.ErrorCode;
import com.practice.order.repository.AccountRepository;
import com.practice.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        accountRepository.deleteAll();
        accountRepository.save(new com.practice.order.domain.Account(
                "acct-user-1", "user-1", new com.practice.order.domain.Money(100_000)));
    }

    @Test
    void 같은_멱등키로_중복_결제해도_잔액은_한_번만_차감된다() {
        createOrder("order-1", 30_000);
        PaymentRequest request = new PaymentRequest("acct-user-1", 30_000, "idem-pay-1");

        ResponseEntity<OrderResponse> first = restTemplate.postForEntity(
                "/api/orders/order-1/payments", request, OrderResponse.class);
        ResponseEntity<OrderResponse> second = restTemplate.postForEntity(
                "/api/orders/order-1/payments", request, OrderResponse.class);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(first.getBody().status()).isEqualTo(OrderStatus.PAID.name());
        assertThat(second.getBody().paymentId()).isEqualTo(first.getBody().paymentId());
        assertThat(accountRepository.findById("acct-user-1").orElseThrow().getBalance().amount())
                .isEqualTo(70_000);
    }

    @Test
    void 다른_주문에_같은_멱등키를_쓰면_충돌이_발생한다() {
        createOrder("order-1", 30_000);
        createOrder("order-2", 30_000);
        PaymentRequest request = new PaymentRequest("acct-user-1", 30_000, "idem-shared");

        restTemplate.postForEntity("/api/orders/order-1/payments", request, OrderResponse.class);
        ResponseEntity<String> conflict = restTemplate.postForEntity(
                "/api/orders/order-2/payments", request, String.class);

        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(conflict.getBody()).contains(ErrorCode.IDEMPOTENCY_KEY_CONFLICT.name());
    }

    @Test
    void 잔액이_부족하면_결제가_실패하고_주문은_결제대기로_남는다() {
        createOrder("order-1", 200_000);
        PaymentRequest request = new PaymentRequest("acct-user-1", 200_000, "idem-fail");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/orders/order-1/payments", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(orderRepository.findById("order-1").orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CREATED);
        assertThat(accountRepository.findById("acct-user-1").orElseThrow().getBalance().amount())
                .isEqualTo(100_000);
    }

    @Test
    void 결제_취소시_잔액이_복구된다() {
        createOrder("order-1", 30_000);
        restTemplate.postForEntity("/api/orders/order-1/payments",
                new PaymentRequest("acct-user-1", 30_000, "idem-pay-2"), OrderResponse.class);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", "idem-cancel-1");
        ResponseEntity<OrderResponse> cancelled = restTemplate.exchange(
                "/api/orders/order-1/cancel", HttpMethod.POST,
                new HttpEntity<>(headers), OrderResponse.class);

        assertThat(cancelled.getBody().status()).isEqualTo(OrderStatus.CANCELLED.name());
        assertThat(accountRepository.findById("acct-user-1").orElseThrow().getBalance().amount())
                .isEqualTo(100_000);
    }

    private void createOrder(String orderId, long amount) {
        restTemplate.postForEntity("/api/orders",
                new OrderCreateRequest(orderId, "user-1", "상품", amount), OrderResponse.class);
    }
}
