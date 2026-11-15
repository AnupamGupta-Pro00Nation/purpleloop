package com.purpleloop.core.detector;

import java.time.Duration;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import com.purpleloop.core.model.DetectionResult;
import com.purpleloop.core.model.LoginEvent;

public class DistributedBruteForceDetector {

    public static final String RULE_ID = "PL-DBF-001";
    public static final String RULE_NAME = "Distributed Brute Force";
    private static final String SEVERITY = "HIGH";
    private static final String MITRE_TECHNIQUE = "T1110";

    private final int failureThreshold;
    private final int minimumDistinctSourceIps;
    private final Duration detectionWindow;
    private final Duration maximumFutureSkew;
    private final Clock clock;
    private final Map<String, AttemptState> attemptsByUsername = new HashMap<>();
    private final Map<String, LocalDateTime> seenEventIds = new HashMap<>();
    private final NavigableMap<LocalDateTime, Set<String>> seenIdsByTimestamp = new TreeMap<>();
    private LocalDateTime latestEventTimestamp;

    public DistributedBruteForceDetector(int failureThreshold, int minimumDistinctSourceIps,
            Duration detectionWindow) {
        this(failureThreshold, minimumDistinctSourceIps, detectionWindow, Clock.systemUTC());
    }

    public DistributedBruteForceDetector(int failureThreshold, int minimumDistinctSourceIps,
            Duration detectionWindow, Clock clock) {
        if (failureThreshold < 1) {
            throw new IllegalArgumentException("Distributed failure threshold must be at least 1");
        }
        if (minimumDistinctSourceIps < 2) {
            throw new IllegalArgumentException("Minimum distinct source IPs must be at least 2");
        }
        if (detectionWindow == null || detectionWindow.isZero() || detectionWindow.isNegative()) {
            throw new IllegalArgumentException("Detection window must be a positive duration");
        }
        if (clock == null) {
            throw new IllegalArgumentException("Clock must not be null");
        }
        this.failureThreshold = failureThreshold;
        this.minimumDistinctSourceIps = minimumDistinctSourceIps;
        this.detectionWindow = detectionWindow;
        this.maximumFutureSkew = Duration.ofMinutes(5).compareTo(detectionWindow) < 0
                ? Duration.ofMinutes(5) : detectionWindow;
        this.clock = clock;
    }

    public synchronized List<DetectionResult> analyze(List<LoginEvent> events) {
        List<DetectionResult> results = new ArrayList<>();
        if (events == null) {
            return results;
        }

        for (LoginEvent event : events) {
            if (!isValid(event)) {
                continue;
            }
            LocalDateTime timestamp = event.getTimestamp();
            if (timestamp.isAfter(LocalDateTime.now(clock).plus(maximumFutureSkew))) {
                continue;
            }
            if (latestEventTimestamp == null || timestamp.isAfter(latestEventTimestamp)) {
                latestEventTimestamp = timestamp;
            }
            expireAttempts(latestEventTimestamp);
            if (timestamp.isBefore(latestEventTimestamp.minus(detectionWindow))) {
                continue;
            }
            if (isDuplicate(event, timestamp)) {
                continue;
            }
            if (event.isSuccess() || !event.hasValidTargetAccount()) {
                continue;
            }

            AttemptState state = attemptsByUsername.computeIfAbsent(
                    event.getUsername(), ignored -> new AttemptState());
            state.events.computeIfAbsent(timestamp, ignored -> new ArrayList<>()).add(event);

            Set<String> sourceIps = sourceIps(state);
            if (!state.alerted && state.count() >= failureThreshold
                    && sourceIps.size() >= minimumDistinctSourceIps) {
                results.add(new DetectionResult(
                        RULE_ID,
                        RULE_NAME,
                        "Multiple source IPs",
                        List.copyOf(sourceIps),
                        event.getUsername(),
                        state.count(),
                        SEVERITY,
                        MITRE_TECHNIQUE,
                        LocalDateTime.now(clock),
                        state.firstEvent().getTimestamp(),
                        "Detected " + state.count() + " failed login attempts against "
                                + event.getUsername() + " from " + sourceIps.size() + " source IPs"
                ));
                state.alerted = true;
            }
        }
        return results;
    }

    private boolean isValid(LoginEvent event) {
        return event != null
                && event.getIp() != null
                && !event.getIp().isBlank()
                && event.getTimestamp() != null;
    }

    private boolean isDuplicate(LoginEvent event, LocalDateTime timestamp) {
        if (seenEventIds.putIfAbsent(event.getId(), timestamp) != null) {
            return true;
        }
        seenIdsByTimestamp.computeIfAbsent(timestamp, ignored -> new HashSet<>()).add(event.getId());
        return false;
    }

    private void expireAttempts(LocalDateTime timestamp) {
        LocalDateTime cutoff = timestamp.minus(detectionWindow);
        while (!seenIdsByTimestamp.isEmpty() && seenIdsByTimestamp.firstKey().isBefore(cutoff)) {
            Set<String> expiredIds = seenIdsByTimestamp.pollFirstEntry().getValue();
            expiredIds.forEach(seenEventIds::remove);
        }
        attemptsByUsername.entrySet().removeIf(entry -> {
            AttemptState state = entry.getValue();
            while (!state.events.isEmpty() && state.events.firstKey().isBefore(cutoff)) {
                state.events.pollFirstEntry();
            }
            Set<String> sourceIps = sourceIps(state);
            if (state.count() < failureThreshold || sourceIps.size() < minimumDistinctSourceIps) {
                state.alerted = false;
            }
            return state.events.isEmpty();
        });
    }

    private Set<String> sourceIps(AttemptState state) {
        Set<String> sourceIps = new LinkedHashSet<>();
        state.events.values().forEach(attempts ->
                attempts.forEach(event -> sourceIps.add(event.getIp())));
        return sourceIps;
    }

    private static class AttemptState {
        private final NavigableMap<LocalDateTime, List<LoginEvent>> events = new TreeMap<>();
        private boolean alerted;

        private int count() {
            return events.values().stream().mapToInt(List::size).sum();
        }

        private LoginEvent firstEvent() {
            return events.firstEntry().getValue().get(0);
        }
    }
}
