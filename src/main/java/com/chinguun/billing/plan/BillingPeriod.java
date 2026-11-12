package com.chinguun.billing.plan;

import java.time.Instant;
import java.time.ZoneId;

public enum BillingPeriod {

    MONTHLY {
        @Override
        public Instant nextFrom(Instant start, ZoneId zone) {
            return start.atZone(zone).plusMonths(1).toInstant();
        }
    },
    YEARLY {
        @Override
        public Instant nextFrom(Instant start, ZoneId zone) {
            return start.atZone(zone).plusYears(1).toInstant();
        }
    };

    public abstract Instant nextFrom(Instant start, ZoneId zone);
}
