package com.chinguun.billing.subscription;

import static com.chinguun.billing.common.ErrorCode.PLAN_NOT_AVAILABLE;
import static com.chinguun.billing.common.ErrorCode.PLAN_NOT_FOUND;
import static com.chinguun.billing.common.ErrorCode.SUBSCRIPTION_ALREADY_EXISTS;
import static com.chinguun.billing.common.ErrorCode.SUBSCRIPTION_NOT_FOUND;

import java.time.Clock;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chinguun.billing.common.BusinessException;
import com.chinguun.billing.plan.Plan;
import com.chinguun.billing.plan.PlanRepository;

@Service
public class SubscriptionService {

    private static final Set<SubscriptionStatus> LIVE_STATUSES =
            EnumSet.of(SubscriptionStatus.PENDING, SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE);

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final Clock clock;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               PlanRepository planRepository,
                               Clock clock) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.clock = clock;
    }

    @Transactional
    public SubscriptionResponse create(CreateSubscriptionRequest request) {
        Plan plan = planRepository.findByCode(request.planCode())
                .orElseThrow(() -> new BusinessException(PLAN_NOT_FOUND));
        if (!plan.isActive()) {
            throw new BusinessException(PLAN_NOT_AVAILABLE);
        }
        if (subscriptionRepository.existsByMemberIdAndStatusIn(request.memberId(), LIVE_STATUSES)) {
            throw new BusinessException(SUBSCRIPTION_ALREADY_EXISTS);
        }

        Subscription subscription = Subscription.start(request.memberId(), plan, clock.instant());
        try {
            // flush now so a unique index violation surfaces here, not at commit time
            subscriptionRepository.saveAndFlush(subscription);
        } catch (DataIntegrityViolationException e) {
            // two requests for the same member got past the exists check at the same time
            throw new BusinessException(SUBSCRIPTION_ALREADY_EXISTS);
        }
        return SubscriptionResponse.from(subscription);
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse get(Long id) {
        return SubscriptionResponse.from(load(id));
    }

    @Transactional
    public SubscriptionResponse cancel(Long id) {
        Subscription subscription = load(id);
        subscription.cancel(clock.instant());
        return SubscriptionResponse.from(subscription);
    }

    private Subscription load(Long id) {
        return subscriptionRepository.findWithPlanById(id)
                .orElseThrow(() -> new BusinessException(SUBSCRIPTION_NOT_FOUND));
    }
}
