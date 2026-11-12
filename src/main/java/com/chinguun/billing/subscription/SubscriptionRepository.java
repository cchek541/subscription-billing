package com.chinguun.billing.subscription;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsByMemberIdAndStatusIn(Long memberId, Collection<SubscriptionStatus> statuses);

    @EntityGraph(attributePaths = "plan")
    Optional<Subscription> findWithPlanById(Long id);
}
