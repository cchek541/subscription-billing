package com.chinguun.billing.plan;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanRepository planRepository;

    public PlanController(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<PlanResponse> list() {
        return planRepository.findAllByActiveTrueOrderByPriceAsc().stream()
                .map(PlanResponse::from)
                .toList();
    }
}
