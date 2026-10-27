package com.purpleloop.core.detector;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.purpleloop.core.model.DetectionResult;
import com.purpleloop.core.model.LoginEvent;

class BruteForceDetectorTest {

    @Test
    void detectsRepeatedFailedLogins() {

        List<LoginEvent> events = List.of(
                new LoginEvent("192.168.1.10", false),
                new LoginEvent("192.168.1.10", false),
                new LoginEvent("192.168.1.10", false),
                new LoginEvent("192.168.1.10", false),
                new LoginEvent("192.168.1.10", false)
        );

        BruteForceDetector detector = new BruteForceDetector(5);

        List<DetectionResult> results = detector.analyze(events);

        assertEquals(1, results.size());

        DetectionResult result = results.get(0);

        assertEquals("Repeated Failed Logins", result.getRuleName());
        assertEquals("192.168.1.10", result.getSourceIp());
        assertEquals(5, result.getFailedAttempts());
        assertEquals("HIGH", result.getSeverity());
        assertEquals("T1110", result.getMitreTechnique());
    }

    @Test
    void rejectsInvalidThreshold() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new BruteForceDetector(0)
        );
    }

    @Test
    void successfulLoginsDoNotTriggerAlert() {

        List<LoginEvent> events = List.of(
                new LoginEvent("192.168.1.10", true),
                new LoginEvent("192.168.1.10", true)
        );

        BruteForceDetector detector = new BruteForceDetector(3);

        List<DetectionResult> results = detector.analyze(events);

        assertEquals(0, results.size());
    }

    @Test
    void detectsFailedLoginsFromMultipleIPs() {

        List<LoginEvent> events = List.of(
                new LoginEvent("192.168.1.10", false),
                new LoginEvent("192.168.1.20", false),
                new LoginEvent("192.168.1.10", false),
                new LoginEvent("192.168.1.20", false),
                new LoginEvent("192.168.1.10", false),
                new LoginEvent("192.168.1.20", false)
        );

        BruteForceDetector detector = new BruteForceDetector(3);

        List<DetectionResult> results = detector.analyze(events);

        assertEquals(2, results.size());

        assertTrue(results.stream().anyMatch(
                result -> result.getSourceIp().equals("192.168.1.10")
        ));

        assertTrue(results.stream().anyMatch(
                result -> result.getSourceIp().equals("192.168.1.20")
        ));
    }
}
