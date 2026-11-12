package com.chinguun.billing.plan;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "plans")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    // KRW has no minor unit, so the price is stored as whole won.
    @Column(nullable = false)
    private long price;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_period", nullable = false)
    private BillingPeriod billingPeriod;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Plan() {
    }

    public Plan(String code, String name, long price, BillingPeriod billingPeriod) {
        this.code = code;
        this.name = name;
        this.price = price;
        this.billingPeriod = billingPeriod;
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public long getPrice() {
        return price;
    }

    public BillingPeriod getBillingPeriod() {
        return billingPeriod;
    }

    public boolean isActive() {
        return active;
    }
}
