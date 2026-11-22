package com.practice.order.gateway;

import com.practice.order.domain.Money;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("!real")
public class MockPaymentGateway implements PaymentGateway {

    private final Map<String, ApprovalResult> approvals = new ConcurrentHashMap<>();
    private int approveCallCount;
    private int cancelCallCount;
    private boolean forceFailure;

    @Value("${mock.pg.failure-rate:0.0}")
    private double failureRate;

    @Value("${mock.pg.delay-ms:0}")
    private long delayMs;

    @Override
    public ApprovalResult approve(String paymentKey, String orderId, Money amount) {
        simulateDelay();
        approveCallCount++;
        if (forceFailure || Math.random() < failureRate) {
            throw new PaymentGatewayException("PG 승인 일시 실패 (모의)", true);
        }

        ApprovalResult result = new ApprovalResult(
                paymentKey,
                "txn-" + UUID.randomUUID(),
                "DONE",
                amount.amount()
        );
        approvals.put(paymentKey, result);
        return result;
    }

    @Override
    public void cancel(String paymentKey, String cancelReason) {
        simulateDelay();
        cancelCallCount++;
        ApprovalResult result = approvals.get(paymentKey);
        if (result == null) {
            throw new PaymentGatewayException("승인 내역 없음 (모의)", false);
        }
        approvals.put(paymentKey, new ApprovalResult(
                result.paymentKey(), result.transactionKey(), "CANCELED", result.approvedAmount()));
    }

    public ApprovalResult getApproval(String paymentKey) {
        return approvals.get(paymentKey);
    }

    public void reset() {
        approvals.clear();
        approveCallCount = 0;
        cancelCallCount = 0;
        forceFailure = false;
    }

    public void forceFailure(boolean forceFailure) {
        this.forceFailure = forceFailure;
    }

    public int getApproveCallCount() {
        return approveCallCount;
    }

    public int getCancelCallCount() {
        return cancelCallCount;
    }

    private void simulateDelay() {
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new PaymentGatewayException("모의 지연 중단", true);
            }
        }
    }
}
