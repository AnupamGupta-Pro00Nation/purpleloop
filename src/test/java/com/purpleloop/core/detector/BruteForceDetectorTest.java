package com.purpleloop.core.detector;

import java.time.Duration;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.purpleloop.core.PurpleLoopService;
import com.purpleloop.core.config.DetectionProperties;
import com.purpleloop.core.model.DetectionResult;
import com.purpleloop.core.model.LoginEvent;

class BruteForceDetectorTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 1, 1, 12, 0);

    @Test
    void successfulLoginDoesNotGenerateAlert() {
        BruteForceDetector detector = new BruteForceDetector(3);

        assertTrue(detector.analyze(List.of(event("192.168.1.10", true, 0))).isEmpty());
    }

    @Test
    void twoFailedAttemptsDoNotTriggerDetection() {
        BruteForceDetector detector = new BruteForceDetector(3);

        assertTrue(detector.analyze(events("192.168.1.10", 2, 0)).isEmpty());
    }

    @Test
    void threeFailedAttemptsFromOneIpTriggerDetection() {
        Clock clock = Clock.fixed(START.plusSeconds(10).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        BruteForceDetector detector = new BruteForceDetector(3, Duration.ofMinutes(5), clock);

        List<DetectionResult> results = detector.analyze(events("192.168.1.10", 3, 0));

        assertEquals(1, results.size());
        assertEquals("PL-BF-001", results.get(0).getRuleId());
        assertEquals("Repeated Failed Logins", results.get(0).getRuleName());
        assertEquals("192.168.1.10", results.get(0).getSourceIp());
        assertEquals(List.of("192.168.1.10"), results.get(0).getSourceIps());
        assertEquals("user@example.test", results.get(0).getUsername());
        assertEquals(3, results.get(0).getFailedAttempts());
        assertEquals("HIGH", results.get(0).getSeverity());
        assertEquals("T1110", results.get(0).getMitreTechnique());
        assertTrue(results.get(0).getMessage().contains("3 failed login attempts"));
        assertFalse(results.get(0).getId().isBlank());
        assertEquals(START, results.get(0).getAttackStartedAt());
        assertEquals(START.plusSeconds(10), results.get(0).getDetectedAt());
    }

    @Test
    void eventAtWindowBoundaryCountsButEventJustOutsideDoesNot() {
        Clock boundaryClock = Clock.fixed(START.plusSeconds(300).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        BruteForceDetector boundaryDetector = new BruteForceDetector(
                2, Duration.ofMinutes(5), boundaryClock);

        List<DetectionResult> boundaryResults = boundaryDetector.analyze(List.of(
                event("192.168.1.10", false, 0),
                event("192.168.1.10", false, 300)
        ));

        assertEquals(1, boundaryResults.size());
        assertEquals(2, boundaryResults.get(0).getFailedAttempts());
        assertEquals(START, boundaryResults.get(0).getAttackStartedAt());

        Clock outsideClock = Clock.fixed(START.plusSeconds(301).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        BruteForceDetector outsideDetector = new BruteForceDetector(
                2, Duration.ofMinutes(5), outsideClock);
        assertTrue(outsideDetector.analyze(List.of(
                event("192.168.1.10", false, 0),
                event("192.168.1.10", false, 301)
        )).isEmpty());
    }

    @Test
    void fiveFailedAttemptsGenerateOnlyOneAlertForTheEpisode() {
        BruteForceDetector detector = new BruteForceDetector(3);

        List<DetectionResult> results = detector.analyze(events("192.168.1.10", 5, 0));

        assertEquals(1, results.size());
        assertEquals(3, results.get(0).getFailedAttempts());
    }

    @Test
    void differentSourceIpsAreTrackedIndependently() {
        BruteForceDetector detector = new BruteForceDetector(3);
        List<LoginEvent> events = new ArrayList<>();
        events.addAll(events("192.168.1.10", 2, 0));
        events.addAll(events("192.168.1.20", 2, 0));
        events.add(event("192.168.1.10", false, 2));
        events.add(event("192.168.1.20", false, 3));

        List<DetectionResult> results = detector.analyze(events);

        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(alert -> alert.getSourceIp().equals("192.168.1.10")));
        assertTrue(results.stream().anyMatch(alert -> alert.getSourceIp().equals("192.168.1.20")));
    }

    @Test
    void attemptsSplitAcrossBatchesAreDetected() {
        BruteForceDetector detector = new BruteForceDetector(3);

        assertTrue(detector.analyze(events("192.168.1.10", 2, 0)).isEmpty());
        List<DetectionResult> results = detector.analyze(List.of(event("192.168.1.10", false, 2)));

        assertEquals(1, results.size());
        assertEquals(3, results.get(0).getFailedAttempts());
    }

    @Test
    void delayedEventsWithinTheWindowAreOrderedByTheirTimestamps() {
        Clock clock = Clock.fixed(START.plusSeconds(120).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        BruteForceDetector detector = new BruteForceDetector(3, Duration.ofMinutes(5), clock);

        assertTrue(detector.analyze(List.of(
                event("192.168.1.10", false, 0),
                event("192.168.1.10", false, 120)
        )).isEmpty());
        List<DetectionResult> results = detector.analyze(
                List.of(event("192.168.1.10", false, 60)));

        assertEquals(1, results.size());
        assertEquals(START, results.get(0).getAttackStartedAt());
        assertEquals(START.plusSeconds(120), results.get(0).getDetectedAt());
    }

    @Test
    void successfulEventsDoNotIncrementFailureCounter() {
        BruteForceDetector detector = new BruteForceDetector(3);
        List<LoginEvent> initialEvents = List.of(
                event("192.168.1.10", false, 0),
                event("192.168.1.10", false, 1),
                event("192.168.1.10", true, 2)
        );

        assertTrue(detector.analyze(initialEvents).isEmpty());
        assertEquals(1, detector.analyze(List.of(event("192.168.1.10", false, 4))).size());
    }

    @Test
    void invalidEventsAreIgnoredSafely() {
        BruteForceDetector detector = new BruteForceDetector(1);
        List<LoginEvent> events = Arrays.asList((LoginEvent) null);

        assertTrue(detector.analyze(events).isEmpty());
        assertTrue(detector.analyze(null).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new LoginEvent(null, false, "user", START));
        assertThrows(IllegalArgumentException.class, () -> new LoginEvent("not-an-ip", false, "user", START));
        assertThrows(IllegalArgumentException.class,
                () -> new LoginEvent("192.168.001.10", false, "user", START));
        assertThrows(IllegalArgumentException.class, () -> new LoginEvent("192.168.1.10", false, " ", START));
        assertThrows(IllegalArgumentException.class, () -> new LoginEvent("192.168.1.10", false, "user", null));
        assertThrows(IllegalArgumentException.class, () -> new LoginEvent(
                "192.168.1.10", false, "u".repeat(LoginEvent.MAX_USERNAME_LENGTH + 1), START));
        assertEquals(
                new LoginEvent("2001:db8::1", false, "user", START).getIp(),
                new LoginEvent("2001:0db8:0:0:0:0:0:1", false, "user", START).getIp());
    }

    @Test
    void futureDatedEventsBeyondAllowedSkewAreIgnored() {
        Clock clock = Clock.fixed(START.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        BruteForceDetector detector = new BruteForceDetector(1, Duration.ofMinutes(5), clock);

        assertTrue(detector.analyze(List.of(event("192.168.1.10", false, 301))).isEmpty());
    }

    @Test
    void replayingTheSameBatchDoesNotCountEventsTwice() {
        BruteForceDetector detector = new BruteForceDetector(3);
        List<LoginEvent> originalBatch = events("192.168.1.10", 2, 0);
        List<LoginEvent> replayedBatch = originalBatch.stream()
                .map(event -> new LoginEvent(
                        event.getId(), event.getIp(), event.isSuccess(),
                        event.getUsername(), event.getTimestamp()))
                .toList();

        assertTrue(detector.analyze(originalBatch).isEmpty());
        assertTrue(detector.analyze(replayedBatch).isEmpty());
        assertEquals(1, detector.analyze(List.of(event("192.168.1.10", false, 2))).size());
    }

    @Test
    void eventsOutsideDetectionWindowDoNotContribute() {
        BruteForceDetector detector = new BruteForceDetector(3, Duration.ofMinutes(5));
        List<LoginEvent> events = List.of(
                event("192.168.1.10", false, 0),
                event("192.168.1.10", false, 1),
                event("192.168.1.10", false, 600)
        );

        assertTrue(detector.analyze(events).isEmpty());
    }

    @Test
    void expiredEpisodeCanGenerateANewAlert() {
        BruteForceDetector detector = new BruteForceDetector(3, Duration.ofMinutes(5));
        List<DetectionResult> firstEpisode = detector.analyze(events("192.168.1.10", 3, 0));
        List<DetectionResult> secondEpisode = detector.analyze(events("192.168.1.10", 3, 600));

        assertEquals(1, firstEpisode.size());
        assertEquals(1, secondEpisode.size());
        assertFalse(firstEpisode.get(0).getId().equals(secondEpisode.get(0).getId()));
    }

    @Test
    void rejectsInvalidThresholdAndDetectionWindow() {
        assertThrows(IllegalArgumentException.class, () -> new BruteForceDetector(0));
        assertThrows(IllegalArgumentException.class,
                () -> new BruteForceDetector(3, Duration.ZERO));
    }

    @Test
    void serviceHasInitialEventHistoryAndMeaningfulMetrics() {
        PurpleLoopService service = new PurpleLoopService();

        assertFalse(service.getEvents().isEmpty());
        assertEquals(service.getEvents().size(), service.getMetrics().getTotalEvents());
        assertEquals(service.getAlerts().size(), service.getMetrics().getTotalAlerts());
        assertEquals(5, service.getMetrics().getTotalFailedLogins());
        assertEquals(1, service.getMetrics().getTotalSuccessfulLogins());
        assertEquals(1, service.getMetrics().getBruteForceAlerts());
        assertTrue(service.getMetrics().getMeanTimeToDetectMs() >= 0);
    }

    @Test
    void meanTimeToDetectUsesProcessingTimeInsteadOfEventTime() {
        Clock clock = Clock.fixed(START.plusSeconds(10).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        PurpleLoopService service = new PurpleLoopService(new DetectionProperties(), clock);

        assertEquals(10_000, service.getMetrics().getMeanTimeToDetectMs());
    }

    @Test
    void serviceAddsNewFailedEventsPerSimulation() {
        PurpleLoopService service = new PurpleLoopService();
        int initialCount = service.getEvents().size();

        List<DetectionResult> newAlerts = service.simulateBruteForce();

        assertEquals(1, newAlerts.size());
        assertEquals(initialCount + 3, service.getEvents().size());
        assertEquals(service.getEvents().size(), service.getMetrics().getTotalEvents());
    }

    @Test
    void servicePersistsHistoryAcrossRequests() {
        PurpleLoopService service = new PurpleLoopService();
        int beforeSimulation = service.getEvents().size();

        service.simulateBruteForce();

        assertTrue(service.getEvents().size() > beforeSimulation);
        assertEquals(service.getEvents().size(), service.getMetrics().getTotalEvents());
    }

    @Test
    void serviceDoesNotDuplicateAlertsForTheSameDetectionCondition() {
        PurpleLoopService service = new PurpleLoopService();
        int initialAlerts = service.getAlerts().size();

        service.simulateBruteForce();
        service.simulateBruteForce();

        assertTrue(service.getAlerts().size() >= initialAlerts + 1);
        assertTrue(service.getAlerts().size() <= initialAlerts + 3);
        assertEquals(1, service.getMetrics().getDistributedAttackAlerts());
    }

    @Test
    void detectionPropertiesRejectInvalidValues() {
        DetectionProperties properties = new DetectionProperties();

        assertThrows(IllegalArgumentException.class, () -> properties.setBruteForceFailureThreshold(0));
        assertThrows(IllegalArgumentException.class, () -> properties.setDetectionWindow(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> properties.setDistributedFailureThreshold(0));
        assertThrows(IllegalArgumentException.class, () -> properties.setMinimumDistinctSourceIps(1));
        assertThrows(IllegalArgumentException.class, () -> properties.setSimulationRateLimit(0));
        assertThrows(IllegalArgumentException.class, () -> properties.setSimulationRateWindow(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> properties.setMaxEventHistorySize(0));
        assertThrows(IllegalArgumentException.class, () -> properties.setMaxAlertHistorySize(0));
    }

    @Test
    void serviceUsesConfiguredThresholdAndDetectionEnablement() {
        DetectionProperties thresholdProperties = new DetectionProperties();
        thresholdProperties.setBruteForceFailureThreshold(4);
        PurpleLoopService thresholdService = new PurpleLoopService(thresholdProperties);

        assertEquals(4, thresholdService.getAlerts().get(0).getFailedAttempts());

        DetectionProperties disabledProperties = new DetectionProperties();
        disabledProperties.setBruteForceEnabled(false);
        disabledProperties.setDistributedEnabled(false);
        PurpleLoopService disabledService = new PurpleLoopService(disabledProperties);

        assertTrue(disabledService.getAlerts().isEmpty());
        assertEquals("DISABLED", disabledService.getRules().get(0).getDetectionStatus());
        assertEquals("DISABLED", disabledService.getRules().get(1).getDetectionStatus());
    }

    private List<LoginEvent> events(String ip, int count, int startOffsetSeconds) {
        List<LoginEvent> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(event(ip, false, startOffsetSeconds + i));
        }
        return result;
    }

    private LoginEvent event(String ip, boolean success, int offsetSeconds) {
        return new LoginEvent(ip, success, "user@example.test", START.plusSeconds(offsetSeconds));
    }
}
