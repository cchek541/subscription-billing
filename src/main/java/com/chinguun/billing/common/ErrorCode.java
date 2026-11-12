package com.chinguun.billing.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "Plan not found"),
    PLAN_NOT_AVAILABLE(HttpStatus.UNPROCESSABLE_CONTENT, "Plan is not available for new subscriptions"),
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Subscription not found"),
    SUBSCRIPTION_ALREADY_EXISTS(HttpStatus.CONFLICT, "Member already has a subscription in progress"),
    INVALID_SUBSCRIPTION_STATE(HttpStatus.CONFLICT, "Subscription cannot change to the requested state");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
