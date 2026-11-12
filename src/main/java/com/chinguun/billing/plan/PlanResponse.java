package com.chinguun.billing.plan;

public record PlanResponse(String code, String name, long price, BillingPeriod billingPeriod) {

    public static PlanResponse from(Plan plan) {
        return new PlanResponse(plan.getCode(), plan.getName(), plan.getPrice(), plan.getBillingPeriod());
    }
}
