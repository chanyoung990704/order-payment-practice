package com.practice.order.service;

import com.practice.order.domain.Account;
import com.practice.order.domain.Money;
import com.practice.order.domain.Order;
import com.practice.order.domain.Payment;
import com.practice.order.domain.PaymentStatus;
import com.practice.order.dto.AccountResponse;
import com.practice.order.dto.OrderResponse;
import com.practice.order.exception.ErrorCode;
import com.practice.order.exception.OrderException;
import com.practice.order.repository.AccountRepository;
import com.practice.order.repository.OrderRepository;
import com.practice.order.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PaymentTxService {

    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final PaymentRepository paymentRepository;

    public PaymentTxService(OrderRepository orderRepository,
                            AccountRepository accountRepository,
                            PaymentRepository paymentRepository) {
        this.orderRepository = orderRepository;
        this.accountRepository = accountRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Payment> findByIdempotencyKey(String key) {
        return paymentRepository.findByIdempotencyKey(key);
    }

    @Transactional(readOnly = true)
    public OrderLoad loadOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND));
        Account account = accountRepository.findByUserIdForUpdate(order.getUserId())
                .orElseThrow(() -> new OrderException(ErrorCode.ACCOUNT_NOT_FOUND));
        return new OrderLoad(order, account);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(String orderId) {
        return OrderResponse.from(orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND)));
    }

    @Transactional
    public OrderResponse reserveAndWithdraw(String orderId, String accountId, Money amount) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND));
        Account account = accountRepository.findByUserIdForUpdate(order.getUserId())
                .orElseThrow(() -> new OrderException(ErrorCode.ACCOUNT_NOT_FOUND));
        if (!accountId.equals(account.getAccountId())) {
            throw new OrderException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        if (order.getAmount().amount() != amount.amount()) {
            throw new OrderException(ErrorCode.INVALID_AMOUNT);
        }

        order.markPaymentPending();
        try {
            account.withdraw(amount);
        } catch (IllegalArgumentException exception) {
            throw new OrderException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        return OrderResponse.from(order);
    }

    @Transactional
    public void complete(String orderId, String transactionKey, String paymentKey,
                         Money amount, String idempotencyKey) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND));
        paymentRepository.save(new Payment(paymentKey, orderId, amount,
                idempotencyKey, PaymentStatus.SUCCESS));
        order.completePayment(transactionKey);
    }

    @Transactional
    public void compensate(String orderId, Money amount,
                           String paymentKey, String idempotencyKey) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND));
        Account account = accountRepository.findByUserIdForUpdate(order.getUserId())
                .orElseThrow(() -> new OrderException(ErrorCode.ACCOUNT_NOT_FOUND));
        account.deposit(amount);
        order.failPayment();
        paymentRepository.save(new Payment(paymentKey, orderId, amount,
                idempotencyKey, PaymentStatus.FAILED));
    }

    @Transactional
    public OrderResponse refundWithoutPg(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(ErrorCode.ORDER_NOT_FOUND));
        Account account = accountRepository.findByUserIdForUpdate(order.getUserId())
                .orElseThrow(() -> new OrderException(ErrorCode.ACCOUNT_NOT_FOUND));
        order.cancel();
        account.deposit(order.getAmount());
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public Payment findPayment(String idempotencyKey) {
        return paymentRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountId) {
        return AccountResponse.from(accountRepository.findById(accountId)
                .orElseThrow(() -> new OrderException(ErrorCode.ACCOUNT_NOT_FOUND)));
    }

    public record OrderLoad(Order order, Account account) {
    }
}
