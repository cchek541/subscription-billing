package com.chinguun.billing.subscription;

import static com.chinguun.billing.common.ErrorCode.INVALID_SUBSCRIPTION_STATE;
import static com.chinguun.billing.common.TimeConfig.BILLING_ZONE;

import java.time.Instant;

import com.chinguun.billing.common.BusinessException;
import com.chinguun.billing.plan.Plan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id")
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    @Column(name = "current_period_start")
    private Instant currentPeriodStart;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodEnd;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Subscription() {
    }

    private Subscription(Long memberId, Plan plan, Instant now) {
        this.memberId = memberId;
        this.plan = plan;
        this.status = SubscriptionStatus.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Subscription start(Long memberId, Plan plan, Instant now) {
        return new Subscription(memberId, plan, now);
    }

    /** Called once the first payment is confirmed. */
    public void activate(Instant now) {
        requireStatus(SubscriptionStatus.PENDING);
        this.status = SubscriptionStatus.ACTIVE;
        this.currentPeriodStart = now;
        this.currentPeriodEnd = plan.getBillingPeriod().nextFrom(now, BILLING_ZONE);
        this.updatedAt = now;
    }

    /**
     * A pending subscription is canceled right away. An active one keeps working until the
     * period the member already paid for is over, and is then not renewed.
     */
    public void cancel(Instant now) {
        switch (status) {
            case PENDING -> {
                this.status = SubscriptionStatus.CANCELED;
                this.canceledAt = now;
            }
            case ACTIVE, PAST_DUE -> {
                if (cancelAtPeriodEnd) {
                    throw new BusinessException(INVALID_SUBSCRIPTION_STATE, "Subscription is already set to cancel");
                }
                this.cancelAtPeriodEnd = true;
                this.canceledAt = now;
            }
            case CANCELED, EXPIRED -> throw new BusinessException(
                    INVALID_SUBSCRIPTION_STATE, "Subscription is already " + status);
        }
        this.updatedAt = now;
    }

    private void requireStatus(SubscriptionStatus expected) {
        if (status != expected) {
            throw new BusinessException(INVALID_SUBSCRIPTION_STATE,
                    "Expected status " + expected + " but was " + status);
        }
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Plan getPlan() {
        return plan;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public Instant getCurrentPeriodStart() {
        return currentPeriodStart;
    }

    public Instant getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public boolean isCancelAtPeriodEnd() {
        return cancelAtPeriodEnd;
    }

    public Instant getCanceledAt() {
        return canceledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
