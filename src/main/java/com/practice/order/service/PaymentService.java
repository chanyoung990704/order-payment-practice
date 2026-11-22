package com.practice.order.service;

import com.practice.order.domain.Money;
import com.practice.order.domain.Payment;
import com.practice.order.gateway.GatewayRetry;
import com.practice.order.gateway.PaymentGateway;
import com.practice.order.gateway.PaymentGatewayException;
import com.practice.order.dto.AccountResponse;
import com.practice.order.dto.PaymentRequest;
import com.practice.order.dto.OrderResponse;
import com.practice.order.exception.ErrorCode;
import com.practice.order.exception.OrderException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentTxService txService;
    private final PaymentGateway paymentGateway;
    private final GatewayRetry gatewayRetry;

    public PaymentService(PaymentTxService txService,
                          PaymentGateway paymentGateway,
                          GatewayRetry gatewayRetry) {
        this.txService = txService;
        this.paymentGateway = paymentGateway;
        this.gatewayRetry = gatewayRetry;
    }

    public OrderResponse pay(String orderId, PaymentRequest request) {
        Optional<Payment> existing = txService.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            Payment payment = existing.get();
            if (!orderId.equals(payment.getOrderId())
                    || payment.getAmount().amount() != request.amount()) {
                throw new OrderException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
            }
            return txService.getOrder(orderId);
        }

        Money amount = new Money(request.amount());
        txService.reserveAndWithdraw(orderId, request.accountId(), amount);

        String paymentKey = "pk-" + UUID.randomUUID();
        PaymentGateway.ApprovalResult approval;
        try {
            approval = gatewayRetry.execute(
                    () -> paymentGateway.approve(paymentKey, orderId, amount));
        } catch (PaymentGatewayException exception) {
            txService.compensate(orderId, amount,
                    paymentKey, request.idempotencyKey());
            throw new OrderException(ErrorCode.PAYMENT_GATEWAY_FAILED);
        }

        if (!approval.isSuccess() || approval.approvedAmount() != amount.amount()) {
            txService.compensate(orderId, amount,
                    paymentKey, request.idempotencyKey());
            throw new OrderException(ErrorCode.PAYMENT_GATEWAY_FAILED);
        }

        try {
            txService.complete(orderId, approval.transactionKey(), approval.paymentKey(),
                    amount, request.idempotencyKey());
        } catch (RuntimeException exception) {
            cancelApprovedPayment(approval.paymentKey());
            txService.compensate(orderId, amount,
                    approval.paymentKey(), request.idempotencyKey());
            throw exception;
        }

        return txService.getOrder(orderId);
    }

    public OrderResponse cancel(String orderId, String idempotencyKey) {
        Payment payment = txService.findPayment(idempotencyKey);
        OrderResponse response = txService.refundWithoutPg(orderId);

        if (payment != null) {
            cancelApprovedPayment(payment.getPaymentId());
        }
        return response;
    }

    private void cancelApprovedPayment(String paymentKey) {
        try {
            gatewayRetry.execute(() -> {
                paymentGateway.cancel(paymentKey, "보상 취소");
                return null;
            });
        } catch (PaymentGatewayException exception) {
            throw new OrderException(ErrorCode.PAYMENT_CANCEL_FAILED);
        }
    }

    public AccountResponse getAccount(String accountId) {
        return txService.getAccount(accountId);
    }
}
