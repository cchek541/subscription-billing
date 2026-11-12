package com.chinguun.billing.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import com.chinguun.billing.common.BusinessException;
import com.chinguun.billing.common.TimeConfig;
import com.chinguun.billing.plan.BillingPeriod;
import com.chinguun.billing.plan.Plan;

class SubscriptionTest {

    private final Plan basic = new Plan("BASIC", "Basic", 9900, BillingPeriod.MONTHLY);
    private final Instant now = Instant.parse("2026-03-10T05:00:00Z");

    @Test
    void newSubscriptionIsPendingWithoutPeriod() {
        Subscription s = Subscription.start(1L, basic, now);

        assertThat(s.getStatus()).isEqualTo(SubscriptionStatus.PENDING);
        assertThat(s.getCurrentPeriodStart()).isNull();
        assertThat(s.getCurrentPeriodEnd()).isNull();
    }

    @Test
    void activateStartsOneMonthPeriod() {
        Subscription s = Subscription.start(1L, basic, now);

        s.activate(now);

        assertThat(s.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(s.getCurrentPeriodStart()).isEqualTo(now);
        assertThat(s.getCurrentPeriodEnd()).isEqualTo(Instant.parse("2026-04-10T05:00:00Z"));
    }

    @Test
    void periodEndIsCalculatedInKoreanTime() {
        // Jan 31 has no matching day in February, so the period should end on Feb 28 (KST).
        Instant lateJan31Kst = ZonedDateTime.of(2026, 1, 31, 23, 30, 0, 0, TimeConfig.BILLING_ZONE).toInstant();
        Subscription s = Subscription.start(1L, basic, lateJan31Kst);

        s.activate(lateJan31Kst);

        ZonedDateTime end = s.getCurrentPeriodEnd().atZone(TimeConfig.BILLING_ZONE);
        assertThat(end.getMonthValue()).isEqualTo(2);
        assertThat(end.getDayOfMonth()).isEqualTo(28);
        assertThat(end.getHour()).isEqualTo(23);
    }

    @Test
    void cannotActivateTwice() {
        Subscription s = Subscription.start(1L, basic, now);
        s.activate(now);

        assertThatThrownBy(() -> s.activate(now)).isInstanceOf(BusinessException.class);
    }

    @Test
    void cancelPendingEndsItImmediately() {
        Subscription s = Subscription.start(1L, basic, now);

        s.cancel(now);

        assertThat(s.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        assertThat(s.getCanceledAt()).isEqualTo(now);
    }

    @Test
    void cancelActiveKeepsItRunningUntilPeriodEnd() {
        Subscription s = Subscription.start(1L, basic, now);
        s.activate(now);

        s.cancel(now.plusSeconds(60));

        assertThat(s.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(s.isCancelAtPeriodEnd()).isTrue();
    }

    @Test
    void cannotCancelTwice() {
        Subscription s = Subscription.start(1L, basic, now);
        s.cancel(now);

        assertThatThrownBy(() -> s.cancel(now)).isInstanceOf(BusinessException.class);
    }
}
