package com.purpleloop.core.detector;

import java.time.Duration;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

import com.purpleloop.core.model.DetectionResult;
import com.purpleloop.core.model.LoginEvent;

public class BruteForceDetector {

    public static final String RULE_ID = "PL-BF-001";
    public static final String RULE_NAME = "Repeated Failed Logins";
    private static final String SEVERITY = "HIGH";
    private static final String MITRE_TECHNIQUE = "T1110";

    private final int threshold;
    private final Duration detectionWindow;
    private final Duration maximumFutureSkew;
    private final Clock clock;
    private final Map<String, AttemptState> attemptsByIp = new HashMap<>();
    private final Map<String, LocalDateTime> seenEventIds = new HashMap<>();
    private final NavigableMap<LocalDateTime, Set<String>> seenIdsByTimestamp = new TreeMap<>();
    private LocalDateTime latestEventTimestamp;

    public BruteForceDetector(int threshold) {
        this(threshold, Duration.ofMinutes(5), Clock.systemUTC());
    }

    public BruteForceDetector(int threshold, Duration detectionWindow) {
        this(threshold, detectionWindow, Clock.systemUTC());
    }

    public BruteForceDetector(int threshold, Duration detectionWindow, Clock clock) {
        if (threshold < 1) {
            throw new IllegalArgumentException("Threshold must be at least 1");
        }
        if (detectionWindow == null || detectionWindow.isZero() || detectionWindow.isNegative()) {
            throw new IllegalArgumentException("Detection window must be a positive duration");
        }
        if (clock == null) {
            throw new IllegalArgumentException("Clock must not be null");
        }
        this.threshold = threshold;
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
            if (event.isSuccess()) {
                continue;
            }

            AttemptState state = attemptsByIp.computeIfAbsent(event.getIp(), ignored -> new AttemptState());
            state.events.computeIfAbsent(timestamp, ignored -> new ArrayList<>()).add(event);
            state.failedAttempts++;

            if (!state.alerted && state.failedAttempts >= threshold) {
                List<String> sources = List.of(event.getIp());
                results.add(new DetectionResult(
                        RULE_ID,
                        RULE_NAME,
                        event.getIp(),
                        sources,
                        event.getUsername(),
                        state.failedAttempts,
                        SEVERITY,
                        MITRE_TECHNIQUE,
                        LocalDateTime.now(clock),
                        state.firstEvent().getTimestamp(),
                        "Detected " + state.failedAttempts + " failed login attempts from " + event.getIp()
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
        attemptsByIp.entrySet().removeIf(entry -> {
            AttemptState state = entry.getValue();
            while (!state.events.isEmpty() && state.events.firstKey().isBefore(cutoff)) {
                state.failedAttempts -= state.events.pollFirstEntry().getValue().size();
            }
            if (state.failedAttempts < threshold) {
                state.alerted = false;
            }
            return state.events.isEmpty();
        });
    }

    private static class AttemptState {
        private final NavigableMap<LocalDateTime, List<LoginEvent>> events = new TreeMap<>();
        private boolean alerted;
        private int failedAttempts;

        private LoginEvent firstEvent() {
            return events.firstEntry().getValue().get(0);
        }
    }
}
