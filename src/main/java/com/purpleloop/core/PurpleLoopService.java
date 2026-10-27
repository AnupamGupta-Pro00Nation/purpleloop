package com.purpleloop.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.purpleloop.core.detector.BruteForceDetector;
import com.purpleloop.core.model.DetectionResult;
import com.purpleloop.core.model.LoginEvent;

@Service
public class PurpleLoopService {

    private final BruteForceDetector detector = new BruteForceDetector(3);

    public List<LoginEvent> generateEvents() {
        List<LoginEvent> events = new ArrayList<>();

        events.add(new LoginEvent("192.168.1.10", false));
        events.add(new LoginEvent("192.168.1.10", false));
        events.add(new LoginEvent("192.168.1.10", false));
        events.add(new LoginEvent("192.168.1.10", false));
        events.add(new LoginEvent("192.168.1.10", false));
        events.add(new LoginEvent("192.168.1.20", true));

        return events;
    }

    public List<LoginEvent> getEvents() {
        return generateEvents();
    }

    public List<DetectionResult> simulateBruteForce() {
        return detector.analyze(generateEvents());
    }

    public DetectionMetrics getMetrics() {
        List<LoginEvent> events = generateEvents();
        List<DetectionResult> alerts = detector.analyze(events);

        Set<String> sourceIps = new HashSet<>();

        for (LoginEvent event : events) {
            sourceIps.add(event.getIp());
        }

        return new DetectionMetrics(
                events.size(),
                alerts.size(),
                sourceIps.size()
        );
    }
}
