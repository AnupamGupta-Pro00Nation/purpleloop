package com.purpleloop.core;

import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.purpleloop.core.config.DetectionProperties;
import com.purpleloop.core.detector.BruteForceDetector;
import com.purpleloop.core.detector.DistributedBruteForceDetector;
import com.purpleloop.core.model.DetectionResult;
import com.purpleloop.core.model.DetectionRule;
import com.purpleloop.core.model.LoginEvent;

@Service
public class PurpleLoopService {

    private static final String DEFAULT_USERNAME = "analyst@example.test";
    private static final String DISTRIBUTED_TEST_USERNAME = "service-admin@example.test";
    private static final List<String> SYNTHETIC_SOURCE_IPS = List.of(
            "192.168.1.10",
            "192.168.1.20",
            "192.168.1.30",
            "192.168.1.40"
    );
    private static final int SYNTHETIC_IP_CAPACITY = 65_536;

    private final DetectionProperties properties;
    private final Clock clock;
    private final BruteForceDetector bruteForceDetector;
    private final DistributedBruteForceDetector distributedDetector;
    private final List<LoginEvent> events = new ArrayList<>();
    private final List<DetectionResult> alerts = new ArrayList<>();
    private final Deque<Instant> simulationRequests = new ArrayDeque<>();
    private long nextSourceIpIndex;

    @Autowired
    public PurpleLoopService(DetectionProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.bruteForceDetector = new BruteForceDetector(
                properties.getBruteForceFailureThreshold(), properties.getDetectionWindow(), clock);
        this.distributedDetector = new DistributedBruteForceDetector(
                properties.getDistributedFailureThreshold(),
                properties.getMinimumDistinctSourceIps(),
                properties.getDetectionWindow(),
                clock);
        seedInitialEvents();
    }

    public PurpleLoopService() {
        this(new DetectionProperties(), Clock.systemUTC());
    }

    public PurpleLoopService(DetectionProperties properties) {
        this(properties, Clock.systemUTC());
    }

    private void seedInitialEvents() {
        LocalDateTime start = LocalDateTime.now(clock).minusSeconds(10);
        List<LoginEvent> initialEvents = List.of(
                new LoginEvent("192.168.1.10", false, DEFAULT_USERNAME, start),
                new LoginEvent("192.168.1.10", false, DEFAULT_USERNAME, start.plusSeconds(1)),
                new LoginEvent("192.168.1.10", false, DEFAULT_USERNAME, start.plusSeconds(2)),
                new LoginEvent("192.168.1.10", false, DEFAULT_USERNAME, start.plusSeconds(3)),
                new LoginEvent("192.168.1.10", false, DEFAULT_USERNAME, start.plusSeconds(4)),
                new LoginEvent("192.168.1.20", true, DEFAULT_USERNAME, start.plusSeconds(5))
        );
        acceptEvents(initialEvents);
    }

    public synchronized List<LoginEvent> generateEvents() {
        return new ArrayList<>(events);
    }

    public synchronized List<LoginEvent> getEvents() {
        return new ArrayList<>(events);
    }

    public synchronized List<DetectionResult> getAlerts() {
        return new ArrayList<>(alerts);
    }

    public synchronized List<DetectionResult> simulateBruteForce() {
        checkSimulationRateLimit();
        return acceptEvents(generateBruteForceEvents());
    }

    public synchronized List<DetectionResult> simulateDistributedBruteForce() {
        checkSimulationRateLimit();
        return acceptEvents(generateDistributedBruteForceEvents());
    }

    public synchronized List<DetectionRule> getRules() {
        return List.of(
                new DetectionRule(
                        BruteForceDetector.RULE_ID,
                        BruteForceDetector.RULE_NAME,
                        "Detects repeated failed logins from one source IP within the configured time window.",
                        "HIGH",
                        "T1110",
                        properties.isBruteForceEnabled()),
                new DetectionRule(
                        DistributedBruteForceDetector.RULE_ID,
                        DistributedBruteForceDetector.RULE_NAME,
                        "Detects repeated failed logins against one account from multiple source IPs within the configured time window.",
                        "HIGH",
                        "T1110",
                        properties.isDistributedEnabled())
        );
    }

    public synchronized DetectionMetrics getMetrics() {
        Set<String> sourceIps = events.stream()
                .filter(Objects::nonNull)
                .map(LoginEvent::getIp)
                .filter(ip -> ip != null && !ip.isBlank())
                .collect(Collectors.toSet());
        int failures = (int) events.stream().filter(Objects::nonNull).filter(event -> !event.isSuccess()).count();
        int successes = (int) events.stream().filter(Objects::nonNull).filter(LoginEvent::isSuccess).count();
        int bruteForceAlerts = (int) alerts.stream()
                .filter(alert -> BruteForceDetector.RULE_ID.equals(alert.getRuleId()))
                .count();
        int distributedAlerts = (int) alerts.stream()
                .filter(alert -> DistributedBruteForceDetector.RULE_ID.equals(alert.getRuleId()))
                .count();
        long meanTimeToDetectMs = Math.round(alerts.stream()
                .filter(alert -> alert.getAttackStartedAt() != null && alert.getDetectedAt() != null)
                .mapToLong(alert -> Duration.between(alert.getAttackStartedAt(), alert.getDetectedAt()).toMillis())
                .filter(duration -> duration >= 0)
                .average()
                .orElse(0.0));

        return new DetectionMetrics(
                events.size(),
                alerts.size(),
                sourceIps.size(),
                failures,
                successes,
                bruteForceAlerts,
                distributedAlerts,
                meanTimeToDetectMs
        );
    }

    private List<DetectionResult> acceptEvents(List<LoginEvent> newEvents) {
        events.addAll(newEvents);
        trimOldest(events, properties.getMaxEventHistorySize());
        List<DetectionResult> generatedAlerts = new ArrayList<>();
        if (properties.isBruteForceEnabled()) {
            generatedAlerts.addAll(bruteForceDetector.analyze(newEvents));
        }
        if (properties.isDistributedEnabled()) {
            generatedAlerts.addAll(distributedDetector.analyze(newEvents));
        }
        alerts.addAll(generatedAlerts);
        trimOldest(alerts, properties.getMaxAlertHistorySize());
        return generatedAlerts;
    }

    private List<LoginEvent> generateBruteForceEvents() {
        String sourceIp = nextSyntheticSourceIp();
        List<LoginEvent> newEvents = new ArrayList<>();
        for (int i = 0; i < properties.getBruteForceFailureThreshold(); i++) {
            newEvents.add(new LoginEvent(sourceIp, false, DEFAULT_USERNAME, LocalDateTime.now(clock)));
        }
        return newEvents;
    }

    private List<LoginEvent> generateDistributedBruteForceEvents() {
        List<LoginEvent> newEvents = new ArrayList<>();
        int failureCount = Math.max(
                properties.getDistributedFailureThreshold(),
                properties.getMinimumDistinctSourceIps());
        for (int i = 0; i < failureCount; i++) {
            String sourceIp = syntheticSourceIp(nextSourceIpIndex++);
            newEvents.add(new LoginEvent(sourceIp, false, DISTRIBUTED_TEST_USERNAME, LocalDateTime.now(clock)));
        }
        return newEvents;
    }

    private String nextSyntheticSourceIp() {
        for (int offset = 0; offset < SYNTHETIC_SOURCE_IPS.size(); offset++) {
            String candidate = syntheticSourceIp(nextSourceIpIndex + offset);
            if (alerts.stream().noneMatch(alert -> candidate.equals(alert.getSourceIp()))) {
                nextSourceIpIndex = (nextSourceIpIndex + offset + 1) % SYNTHETIC_IP_CAPACITY;
                return candidate;
            }
        }

        String fallbackSourceIp = syntheticSourceIp(nextSourceIpIndex);
        nextSourceIpIndex = (nextSourceIpIndex + 1) % SYNTHETIC_IP_CAPACITY;
        return fallbackSourceIp;
    }

    private String syntheticSourceIp(long index) {
        long boundedIndex = Math.floorMod(index, SYNTHETIC_IP_CAPACITY);
        if (boundedIndex < SYNTHETIC_SOURCE_IPS.size()) {
            return SYNTHETIC_SOURCE_IPS.get((int) boundedIndex);
        }
        long offset = boundedIndex - SYNTHETIC_SOURCE_IPS.size();
        return "198.18." + (offset / 256) + "." + (offset % 256);
    }

    private void checkSimulationRateLimit() {
        Instant now = clock.instant();
        Instant cutoff = now.minus(properties.getSimulationRateWindow());
        while (!simulationRequests.isEmpty() && !simulationRequests.peekFirst().isAfter(cutoff)) {
            simulationRequests.removeFirst();
        }
        if (simulationRequests.size() >= properties.getSimulationRateLimit()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Simulation rate limit exceeded; retry after the configured rate window");
        }
        simulationRequests.addLast(now);
    }

    private <T> void trimOldest(List<T> history, int maximumSize) {
        int excess = history.size() - maximumSize;
        if (excess > 0) {
            history.subList(0, excess).clear();
        }
    }
}
