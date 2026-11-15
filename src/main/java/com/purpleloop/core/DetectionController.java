package com.purpleloop.core;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.purpleloop.core.model.DetectionResult;
import com.purpleloop.core.model.DetectionRule;
import com.purpleloop.core.model.LoginEvent;

@RestController
@RequestMapping("/api")
public class DetectionController {

    private final PurpleLoopService purpleLoopService;

    public DetectionController(PurpleLoopService purpleLoopService) {
        this.purpleLoopService = purpleLoopService;
    }

    @GetMapping("/events")
    public List<LoginEvent> getEvents() {
        return purpleLoopService.getEvents();
    }

    @GetMapping("/alerts")
    public List<DetectionResult> getAlerts() {
        return purpleLoopService.getAlerts();
    }

    @PostMapping("/simulate/brute-force")
    public List<DetectionResult> simulateBruteForce() {
        return purpleLoopService.simulateBruteForce();
    }

    @PostMapping("/simulate/distributed-brute-force")
    public List<DetectionResult> simulateDistributedBruteForce() {
        return purpleLoopService.simulateDistributedBruteForce();
    }

    @GetMapping("/metrics")
    public DetectionMetrics getMetrics() {
        return purpleLoopService.getMetrics();
    }

    @GetMapping("/rules")
    public List<DetectionRule> getRules() {
        return purpleLoopService.getRules();
    }
}
