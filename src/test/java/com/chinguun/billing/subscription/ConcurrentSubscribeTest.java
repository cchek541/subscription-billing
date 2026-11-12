package com.chinguun.billing.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.chinguun.billing.TestcontainersConfig;
import com.chinguun.billing.common.BusinessException;
import com.chinguun.billing.common.ErrorCode;

/**
 * Same member hits "subscribe" many times at once (double click, client retry, etc).
 * Only one subscription should be created no matter how the requests interleave.
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
class ConcurrentSubscribeTest {

    @Autowired
    SubscriptionService subscriptionService;

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @BeforeEach
    void clean() {
        subscriptionRepository.deleteAllInBatch();
    }

    @Test
    void onlyOneSubscriptionIsCreatedForParallelRequests() throws Exception {
        int threads = 10;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            for (int i = 0; i < threads; i++) {
                results.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    try {
                        subscriptionService.create(new CreateSubscriptionRequest(42L, "BASIC"));
                        return true;
                    } catch (BusinessException e) {
                        assertThat(e.errorCode()).isEqualTo(ErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
                        return false;
                    }
                }));
            }
            ready.await();
            go.countDown();
        }

        long succeeded = 0;
        for (Future<Boolean> f : results) {
            if (f.get()) {
                succeeded++;
            }
        }
        assertThat(succeeded).isEqualTo(1);
        assertThat(subscriptionRepository.count()).isEqualTo(1);
    }
}
