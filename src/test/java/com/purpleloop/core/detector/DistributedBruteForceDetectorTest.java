package com.purpleloop.core.detector;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.purpleloop.core.PurpleLoopService;
import com.purpleloop.core.config.DetectionProperties;
import com.purpleloop.core.model.DetectionResult;
import com.purpleloop.core.model.LoginEvent;

class DistributedBruteForceDetectorTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 1, 1, 12, 0);

    @Test
    void detectsFailuresAgainstOneAccountFromMultipleIps() {
        DistributedBruteForceDetector detector = new DistributedBruteForceDetector(
                5, 3, Duration.ofMinutes(5));
        List<DetectionResult> results = detector.analyze(List.of(
                event("192.168.1.10", "admin", 0),
                event("192.168.1.20", "admin", 1),
                event("192.168.1.30", "admin", 2),
                event("192.168.1.10", "admin", 3),
                event("192.168.1.20", "admin", 4)
        ));

        assertEquals(1, results.size());
        assertEquals("Distributed Brute Force", results.get(0).getRuleName());
        assertEquals("admin", results.get(0).getUsername());
        assertEquals(3, results.get(0).getSourceIps().size());
        assertEquals(5, results.get(0).getFailedAttempts());
        assertEquals("T1110", results.get(0).getMitreTechnique());
    }

    @Test
    void tracksAccountAttemptsAcrossBatchesAndAvoidsDuplicateAlerts() {
        DistributedBruteForceDetector detector = new DistributedBruteForceDetector(
                4, 3, Duration.ofMinutes(5));
        List<LoginEvent> firstBatch = List.of(
                event("192.168.1.10", "admin", 0),
                event("192.168.1.20", "admin", 1)
        );
        List<LoginEvent> secondBatch = List.of(
                event("192.168.1.30", "admin", 2),
                event("192.168.1.10", "admin", 3),
                event("192.168.1.20", "admin", 4)
        );

        assertTrue(detector.analyze(firstBatch).isEmpty());
        assertEquals(1, detector.analyze(secondBatch).size());
        assertTrue(detector.analyze(List.of(event("192.168.1.30", "admin", 5))).isEmpty());
    }

    @Test
    void distinctAccountsAreTrackedIndependentlyAndSuccessesAreIgnored() {
        DistributedBruteForceDetector detector = new DistributedBruteForceDetector(
                3, 2, Duration.ofMinutes(5));
        List<LoginEvent> events = List.of(
                event("192.168.1.10", "admin", 0),
                event("192.168.1.20", "operator", 1),
                event("192.168.1.30", "admin", 2, true),
                event("192.168.1.20", "operator", 3),
                event("192.168.1.30", "admin", 4),
                event("192.168.1.10", "admin", 5)
        );

        assertEquals(1, detector.analyze(events).size());
    }

    @Test
    void attemptsOutsideTheWindowDoNotContributeToDistributedDetection() {
        DistributedBruteForceDetector detector = new DistributedBruteForceDetector(
                3, 3, Duration.ofMinutes(5));
        List<LoginEvent> events = new ArrayList<>(List.of(
                event("192.168.1.10", "admin", 0),
                event("192.168.1.20", "admin", 1),
                event("192.168.1.30", "admin", 600)
        ));

        assertTrue(detector.analyze(events).isEmpty());
    }

    @Test
    void missingAndUnknownUsernamesAreNotGroupedAsAnAccount() {
        DistributedBruteForceDetector detector = new DistributedBruteForceDetector(
                2, 2, Duration.ofMinutes(5));

        assertTrue(detector.analyze(List.of(
                new LoginEvent("192.168.1.10", false),
                new LoginEvent("192.168.1.20", false)
        )).isEmpty());
        assertTrue(detector.analyze(List.of(
                event("192.168.1.10", "unknown", 1),
                event("192.168.1.20", "unknown", 2)
        )).isEmpty());
    }

    @Test
    void configuredDistinctIpThresholdAboveFourIsSupportedBySimulation() {
        DetectionProperties properties = new DetectionProperties();
        properties.setDistributedFailureThreshold(5);
        properties.setMinimumDistinctSourceIps(5);

        List<DetectionResult> results = new PurpleLoopService(properties).simulateDistributedBruteForce();

        assertEquals(1, results.size());
        assertTrue(results.get(0).getSourceIps().size() >= 5);
    }

    @Test
    void replayingDistributedBatchDoesNotCountEventsTwice() {
        DistributedBruteForceDetector detector = new DistributedBruteForceDetector(
                3, 2, Duration.ofMinutes(5));
        List<LoginEvent> replayedBatch = List.of(
                event("192.168.1.10", "admin", 0),
                event("192.168.1.20", "admin", 1)
        );

        assertTrue(detector.analyze(replayedBatch).isEmpty());
        assertTrue(detector.analyze(replayedBatch).isEmpty());
        assertEquals(1, detector.analyze(List.of(event("192.168.1.30", "admin", 2))).size());
    }

    private LoginEvent event(String ip, String username, int offsetSeconds) {
        return event(ip, username, offsetSeconds, false);
    }

    private LoginEvent event(String ip, String username, int offsetSeconds, boolean success) {
        return new LoginEvent(ip, success, username, START.plusSeconds(offsetSeconds));
    }
}
