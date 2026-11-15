package com.purpleloop.core;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.purpleloop.core.config.DetectionProperties;

class PurpleLoopServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void simulationRateLimitAndHistoryRetentionAreApplied() {
        DetectionProperties properties = new DetectionProperties();
        properties.setSimulationRateLimit(1);
        properties.setMaxEventHistorySize(8);
        properties.setMaxAlertHistorySize(1);
        PurpleLoopService service = new PurpleLoopService(properties, CLOCK);

        String oldAlertId = service.getAlerts().get(0).getId();
        service.simulateBruteForce();

        assertEquals(8, service.getEvents().size());
        assertEquals(1, service.getAlerts().size());
        assertNotEquals(oldAlertId, service.getAlerts().get(0).getId());
        assertThrows(ResponseStatusException.class, service::simulateDistributedBruteForce);
    }

    @Test
    void concurrentSimulationRequestsCannotExceedTheConfiguredRateLimit() throws Exception {
        DetectionProperties properties = new DetectionProperties();
        properties.setSimulationRateLimit(1);
        PurpleLoopService service = new PurpleLoopService(properties, CLOCK);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Boolean>> requests = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                requests.add(() -> {
                    try {
                        service.simulateBruteForce();
                        return true;
                    } catch (ResponseStatusException exception) {
                        return false;
                    }
                });
            }

            List<Future<Boolean>> results = executor.invokeAll(requests);
            int accepted = 0;
            for (Future<Boolean> result : results) {
                try {
                    if (result.get()) {
                        accepted++;
                    }
                } catch (ExecutionException exception) {
                    throw new AssertionError("Unexpected simulation failure", exception.getCause());
                }
            }
            assertEquals(1, accepted);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void rateLimitWindowCanBeConfigured() {
        DetectionProperties properties = new DetectionProperties();
        properties.setSimulationRateWindow(Duration.ofSeconds(30));

        assertEquals(Duration.ofSeconds(30), properties.getSimulationRateWindow());
    }

    @Test
    void expiredRateLimitWindowAllowsAnotherSimulation() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T12:00:00Z"));
        DetectionProperties properties = new DetectionProperties();
        properties.setSimulationRateLimit(1);
        properties.setSimulationRateWindow(Duration.ofSeconds(30));
        PurpleLoopService service = new PurpleLoopService(properties, clock);

        service.simulateBruteForce();
        clock.advance(Duration.ofSeconds(30));

        service.simulateDistributedBruteForce();
    }

    private static class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return Clock.fixed(now, zone);
        }

        @Override
        public Instant instant() {
            return now;
        }

        private void advance(Duration duration) {
            now = now.plus(duration);
        }
    }
}
