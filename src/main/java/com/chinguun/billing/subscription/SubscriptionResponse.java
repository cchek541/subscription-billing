package com.chinguun.billing.subscription;

import java.time.Instant;

public record SubscriptionResponse(
        Long id,
        Long memberId,
        String planCode,
        long price,
        SubscriptionStatus status,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        boolean cancelAtPeriodEnd,
        Instant createdAt) {

    public static SubscriptionResponse from(Subscription s) {
        return new SubscriptionResponse(
                s.getId(),
                s.getMemberId(),
                s.getPlan().getCode(),
                s.getPlan().getPrice(),
                s.getStatus(),
                s.getCurrentPeriodStart(),
                s.getCurrentPeriodEnd(),
                s.isCancelAtPeriodEnd(),
                s.getCreatedAt());
    }
}
