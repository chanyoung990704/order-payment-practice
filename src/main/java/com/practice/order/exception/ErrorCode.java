package com.practice.order.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "계좌를 찾을 수 없습니다."),
    INSUFFICIENT_BALANCE(HttpStatus.CONFLICT, "잔액이 부족합니다."),
    INVALID_ORDER_STATE(HttpStatus.CONFLICT, "주문 상태가 유효하지 않습니다."),
    IDEMPOTENCY_KEY_CONFLICT(HttpStatus.CONFLICT, "이미 처리된 요청입니다."),
    PAYMENT_GATEWAY_FAILED(HttpStatus.BAD_GATEWAY, "결제 승인에 실패했습니다."),
    PAYMENT_CANCEL_FAILED(HttpStatus.BAD_GATEWAY, "PG 취소 요청에 실패했습니다."),
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "금액이 유효하지 않습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
