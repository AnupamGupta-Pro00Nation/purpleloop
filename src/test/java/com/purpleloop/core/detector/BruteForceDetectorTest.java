package com.purpleloop.core.detector;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.purpleloop.core.PurpleLoopService;
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

    @Test
    void serviceHasInitialEventHistoryAndMetrics() {

        PurpleLoopService service = new PurpleLoopService();

        assertFalse(service.getEvents().isEmpty());
        assertEquals(service.getEvents().size(), service.getMetrics().getTotalEvents());
        assertEquals(service.getAlerts().size(), service.getMetrics().getTotalAlerts());
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
        assertTrue(service.getAlerts().size() <= initialAlerts + 2);
    }
}
