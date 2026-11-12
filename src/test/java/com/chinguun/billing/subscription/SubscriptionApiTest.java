package com.chinguun.billing.subscription;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.chinguun.billing.TestcontainersConfig;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class SubscriptionApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @BeforeEach
    void clean() {
        subscriptionRepository.deleteAllInBatch();
    }

    @Test
    void listsActivePlansCheapestFirst() throws Exception {
        mvc.perform(get("/api/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("BASIC"))
                .andExpect(jsonPath("$[0].price").value(9900))
                .andExpect(jsonPath("$[1].code").value("PRO"));
    }

    @Test
    void createsPendingSubscription() throws Exception {
        mvc.perform(createRequest(1L, "PRO"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.planCode").value("PRO"))
                .andExpect(jsonPath("$.price").value(19900));
    }

    @Test
    void rejectsSecondSubscriptionForSameMember() throws Exception {
        mvc.perform(createRequest(1L, "BASIC")).andExpect(status().isCreated());

        mvc.perform(createRequest(1L, "PRO"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SUBSCRIPTION_ALREADY_EXISTS"));
    }

    @Test
    void memberCanSubscribeAgainAfterCanceling() throws Exception {
        long id = createAndGetId(1L, "BASIC");
        mvc.perform(post("/api/subscriptions/{id}/cancel", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));

        mvc.perform(createRequest(1L, "PRO")).andExpect(status().isCreated());
    }

    @Test
    void unknownPlanIsNotFound() throws Exception {
        mvc.perform(createRequest(1L, "ENTERPRISE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLAN_NOT_FOUND"));
    }

    @Test
    void validatesRequestBody() throws Exception {
        mvc.perform(post("/api/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\": -1, \"planCode\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.fields.memberId").exists())
                .andExpect(jsonPath("$.fields.planCode").exists());
    }

    @Test
    void cancelingTwiceIsConflict() throws Exception {
        long id = createAndGetId(1L, "BASIC");
        mvc.perform(post("/api/subscriptions/{id}/cancel", id)).andExpect(status().isOk());

        mvc.perform(post("/api/subscriptions/{id}/cancel", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_SUBSCRIPTION_STATE"));
    }

    @Test
    void missingSubscriptionIsNotFound() throws Exception {
        mvc.perform(get("/api/subscriptions/{id}", 999_999))
                .andExpect(status().isNotFound());
    }

    private long createAndGetId(long memberId, String planCode) throws Exception {
        MvcResult result = mvc.perform(createRequest(memberId, planCode))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private static org.springframework.test.web.servlet.RequestBuilder createRequest(long memberId, String planCode) {
        return post("/api/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\": %d, \"planCode\": \"%s\"}".formatted(memberId, planCode));
    }
}
