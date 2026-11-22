package com.practice.order.service;

import com.practice.order.domain.Account;
import com.practice.order.domain.Money;
import com.practice.order.domain.Order;
import com.practice.order.domain.Payment;
import com.practice.order.domain.PaymentStatus;
import com.practice.order.gateway.GatewayRetry;
import com.practice.order.gateway.PaymentGateway;
import com.practice.order.gateway.PaymentGatewayException;
import com.practice.order.dto.AccountResponse;
import com.practice.order.dto.PaymentRequest;
import com.practice.order.dto.OrderResponse;
import com.practice.order.exception.ErrorCode;
import com.practice.order.exception.OrderException;
import com.practice.order.repository.AccountRepository;
import com.practice.order.repository.OrderRepository;
import com.practice.order.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PaymentService {

    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final GatewayRetry gatewayRetry;

    public PaymentService(OrderRepository orderRepository,
                          AccountRepository accountRepository,
                          PaymentRepository paymentRepository,
                          PaymentGateway paymentGateway,
                          GatewayRetry gatewayRetry) {
        this.orderRepository = orderRepository;
        this.accountRepository = accountRepository;
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.gatewayRetry = gatewayRetry;
    }

    @Transactional
    public OrderResponse pay(String orderId, PaymentRequest request) {
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            Payment payment = existing.get();
            if (!orderId.equals(payment.getOrderId())
                    || payment.getAmount().amount() != request.amount()) {
                throw new OrderException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
            }
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND));
            return OrderResponse.from(order);
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND));

        Account account = accountRepository.findByUserIdForUpdate(order.getUserId())
                .orElseThrow(() -> new OrderException(ErrorCode.ACCOUNT_NOT_FOUND));
        if (!request.accountId().equals(account.getAccountId())) {
            throw new OrderException(ErrorCode.ACCOUNT_NOT_FOUND);
        }

        Money amount = new Money(request.amount());
        if (order.getAmount().amount() != amount.amount()) {
            throw new OrderException(ErrorCode.INVALID_AMOUNT);
        }

        order.markPaymentPending();
        try {
            account.withdraw(amount);
        } catch (IllegalArgumentException exception) {
            throw new OrderException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        String paymentKey = "pk-" + UUID.randomUUID();
        try {
            PaymentGateway.ApprovalResult approval = gatewayRetry.execute(
                    () -> paymentGateway.approve(paymentKey, orderId, amount));

            if (!approval.isSuccess() || approval.approvedAmount() != amount.amount()) {
                throw new PaymentGatewayException("PG 승인 금액 불일치", false);
            }

            Payment payment = new Payment(approval.paymentKey(), orderId, amount,
                    request.idempotencyKey(), PaymentStatus.SUCCESS);
            paymentRepository.save(payment);
            order.completePayment(approval.transactionKey());
        } catch (PaymentGatewayException exception) {
            compensate(order, account, amount, paymentKey, request.idempotencyKey());
            throw new OrderException(ErrorCode.PAYMENT_GATEWAY_FAILED);
        }

        return OrderResponse.from(order);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void compensate(Order order, Account account, Money amount,
                           String paymentKey, String idempotencyKey) {
        account.deposit(amount);
        paymentRepository.save(new Payment(paymentKey, order.getOrderId(), amount,
                idempotencyKey, PaymentStatus.FAILED));
    }

    @Transactional
    public OrderResponse cancel(String orderId, String idempotencyKey) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND));

        Account account = accountRepository.findByUserIdForUpdate(order.getUserId())
                .orElseThrow(() -> new OrderException(ErrorCode.ACCOUNT_NOT_FOUND));

        order.cancel();
        account.deposit(order.getAmount());

        paymentRepository.findByIdempotencyKey(idempotencyKey).ifPresent(payment -> {
            try {
                gatewayRetry.execute(() -> {
                    paymentGateway.cancel(payment.getPaymentId(), "고객 요청 취소");
                    return null;
                });
            } catch (PaymentGatewayException exception) {
                throw new OrderException(ErrorCode.PAYMENT_CANCEL_FAILED);
            }
        });

        return OrderResponse.from(order);
    }

    public AccountResponse getAccount(String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new OrderException(ErrorCode.ACCOUNT_NOT_FOUND));
        return AccountResponse.from(account);
    }
}
