package com.purpleloop.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.purpleloop.core.detector.BruteForceDetector;
import com.purpleloop.core.model.DetectionResult;
import com.purpleloop.core.model.LoginEvent;

@Service
public class PurpleLoopService {

    private static final int THRESHOLD = 3;
    private static final List<String> SYNTHETIC_SOURCE_IPS = List.of(
            "192.168.1.10",
            "192.168.1.20",
            "192.168.1.30",
            "192.168.1.40"
    );

    private final BruteForceDetector detector = new BruteForceDetector(THRESHOLD);
    private final List<LoginEvent> events = new ArrayList<>();
    private final List<DetectionResult> alerts = new ArrayList<>();
    private int nextSourceIpIndex = 0;

    public PurpleLoopService() {
        seedInitialEvents();
    }

    private void seedInitialEvents() {
        events.add(new LoginEvent("192.168.1.10", false));
        events.add(new LoginEvent("192.168.1.10", false));
        events.add(new LoginEvent("192.168.1.10", false));
        events.add(new LoginEvent("192.168.1.10", false));
        events.add(new LoginEvent("192.168.1.10", false));
        events.add(new LoginEvent("192.168.1.20", true));

        addUniqueAlerts(detector.analyze(events));
    }

    public List<LoginEvent> generateEvents() {
        return new ArrayList<>(events);
    }

    public List<LoginEvent> getEvents() {
        return new ArrayList<>(events);
    }

    public List<DetectionResult> getAlerts() {
        return new ArrayList<>(alerts);
    }

    public List<DetectionResult> simulateBruteForce() {
        List<LoginEvent> newEvents = generateSimulationEvents();
        events.addAll(newEvents);

        List<DetectionResult> generatedAlerts = detector.analyze(newEvents);
        List<DetectionResult> retainedAlerts = new ArrayList<>(alerts);

        addUniqueAlerts(generatedAlerts);

        return generatedAlerts.stream()
                .filter(alert -> retainedAlerts.stream()
                        .noneMatch(existingAlert -> sameAlert(existingAlert, alert)))
                .collect(Collectors.toList());
    }

    public DetectionMetrics getMetrics() {
        Set<String> sourceIps = events.stream()
                .map(LoginEvent::getIp)
                .collect(Collectors.toSet());

        return new DetectionMetrics(
                events.size(),
                alerts.size(),
                sourceIps.size()
        );
    }

    private List<LoginEvent> generateSimulationEvents() {
        String sourceIp = nextSyntheticSourceIp();
        List<LoginEvent> newEvents = new ArrayList<>();

        for (int i = 0; i < THRESHOLD; i++) {
            newEvents.add(new LoginEvent(sourceIp, false));
        }

        return newEvents;
    }

    private String nextSyntheticSourceIp() {
        for (int offset = 0; offset < SYNTHETIC_SOURCE_IPS.size(); offset++) {
            String candidate = SYNTHETIC_SOURCE_IPS.get(
                    (nextSourceIpIndex + offset) % SYNTHETIC_SOURCE_IPS.size()
            );

            if (alerts.stream().noneMatch(alert -> alert.getSourceIp().equals(candidate))) {
                nextSourceIpIndex = (nextSourceIpIndex + offset + 1) % SYNTHETIC_SOURCE_IPS.size();
                return candidate;
            }
        }

        String fallbackSourceIp = SYNTHETIC_SOURCE_IPS.get(
                nextSourceIpIndex % SYNTHETIC_SOURCE_IPS.size()
        );
        nextSourceIpIndex = (nextSourceIpIndex + 1) % SYNTHETIC_SOURCE_IPS.size();
        return fallbackSourceIp;
    }

    private void addUniqueAlerts(List<DetectionResult> generatedAlerts) {
        for (DetectionResult generatedAlert : generatedAlerts) {
            if (alerts.stream().noneMatch(existingAlert -> sameAlert(existingAlert, generatedAlert))) {
                alerts.add(generatedAlert);
            }
        }
    }

    private boolean sameAlert(DetectionResult left, DetectionResult right) {
        return Objects.equals(left.getRuleName(), right.getRuleName())
                && Objects.equals(left.getSourceIp(), right.getSourceIp())
                && left.getFailedAttempts() == right.getFailedAttempts();
    }
}
