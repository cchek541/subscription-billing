package com.chinguun.billing.subscription;

public enum SubscriptionStatus {
    /** Created, waiting for the first payment. */
    PENDING,
    ACTIVE,
    /** Renewal payment failed, still inside the grace period. */
    PAST_DUE,
    CANCELED,
    EXPIRED
}
