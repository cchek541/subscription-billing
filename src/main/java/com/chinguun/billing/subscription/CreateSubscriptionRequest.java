package com.chinguun.billing.subscription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// TODO: take memberId from the auth token once login exists
public record CreateSubscriptionRequest(
        @NotNull @Positive Long memberId,
        @NotBlank String planCode) {
}
