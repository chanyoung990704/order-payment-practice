package com.practice.order;

import com.practice.order.domain.Account;
import com.practice.order.domain.Money;
import com.practice.order.domain.OrderStatus;
import com.practice.order.dto.OrderCreateRequest;
import com.practice.order.dto.OrderResponse;
import com.practice.order.dto.PaymentRequest;
import com.practice.order.exception.ErrorCode;
import com.practice.order.gateway.MockPaymentGateway;
import com.practice.order.repository.AccountRepository;
import com.practice.order.repository.OrderRepository;
import com.practice.order.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentGatewayIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private MockPaymentGateway mockPaymentGateway;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        accountRepository.deleteAll();
        accountRepository.save(new Account("acct-user-1", "user-1", new Money(100_000)));
        mockPaymentGateway.reset();
    }

    @Test
    void PG_승인이_성공하면_거래키가_주문에_기록된다() {
        createOrder("order-1", 30_000);

        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
                "/api/orders/order-1/payments",
                new PaymentRequest("acct-user-1", 30_000, "idem-gw-1"),
                OrderResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().status()).isEqualTo(OrderStatus.PAID.name());
        assertThat(response.getBody().paymentId()).startsWith("txn-");
        assertThat(mockPaymentGateway.getApproveCallCount()).isEqualTo(1);
    }

    @Test
    void PG_가_계속_실패하면_잔액이_복구되고_실패_이력이_남는다() {
        mockPaymentGateway.forceFailure(true);
        createOrder("order-1", 30_000);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/orders/order-1/payments",
                new PaymentRequest("acct-user-1", 30_000, "idem-gw-fail"),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).contains(ErrorCode.PAYMENT_GATEWAY_FAILED.name());
        assertThat(accountRepository.findById("acct-user-1").orElseThrow().getBalance().amount())
                .isEqualTo(100_000);
        assertThat(orderRepository.findById("order-1").orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CREATED);
        assertThat(mockPaymentGateway.getApproveCallCount()).isEqualTo(3);
    }

    @Test
    void 취소시_PG_취소까지_호출된다() {
        createOrder("order-1", 30_000);
        restTemplate.postForEntity("/api/orders/order-1/payments",
                new PaymentRequest("acct-user-1", 30_000, "idem-gw-cancel"), OrderResponse.class);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("Idempotency-Key", "idem-gw-cancel");
        int cancelCountBefore = mockPaymentGateway.getCancelCallCount();
        ResponseEntity<OrderResponse> cancelled = restTemplate.exchange(
                "/api/orders/order-1/cancel", org.springframework.http.HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(headers), OrderResponse.class);

        assertThat(cancelled.getBody().status()).isEqualTo(OrderStatus.CANCELLED.name());
        assertThat(mockPaymentGateway.getCancelCallCount()).isEqualTo(cancelCountBefore + 1);
        assertThat(accountRepository.findById("acct-user-1").orElseThrow().getBalance().amount())
                .isEqualTo(100_000);
    }

    private void createOrder(String orderId, long amount) {
        restTemplate.postForEntity("/api/orders",
                new OrderCreateRequest(orderId, "user-1", "상품", amount), OrderResponse.class);
    }
}
