package com.purpleloop.core;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import com.purpleloop.core.config.DetectionProperties;

@SpringBootTest(
        classes = PurpleLoopApplication.class,
        properties = {
                "purpleloop.detection.brute-force-failure-threshold=4",
                "purpleloop.detection.max-event-history-size=100",
                "purpleloop.detection.max-alert-history-size=50",
                "purpleloop.detection.simulation-rate-limit=1"
        })
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class DetectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DetectionProperties detectionProperties;

    @Test
    void integrationTestsUseProductionApplicationAndBindDetectionProperties() {
        org.junit.jupiter.api.Assertions.assertEquals(4, detectionProperties.getBruteForceFailureThreshold());
        org.junit.jupiter.api.Assertions.assertEquals(100, detectionProperties.getMaxEventHistorySize());
        org.junit.jupiter.api.Assertions.assertEquals(50, detectionProperties.getMaxAlertHistorySize());
        org.junit.jupiter.api.Assertions.assertEquals(1, detectionProperties.getSimulationRateLimit());
    }

    @Test
    void healthEndpointReturnsApplicationStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project", is("PurpleLoop")))
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    void eventsEndpointReturnsStoredHistory() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test
    void metricsEndpointReflectsStoredHistory() throws Exception {
        mockMvc.perform(get("/api/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvents", greaterThan(0)))
                .andExpect(jsonPath("$.totalAlerts", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.uniqueSourceIps", greaterThan(0)))
                .andExpect(jsonPath("$.totalFailedLogins", greaterThan(0)))
                .andExpect(jsonPath("$.totalSuccessfulLogins", greaterThan(0)))
                .andExpect(jsonPath("$.bruteForceAlerts", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.distributedAttackAlerts", greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.meanTimeToDetectMs", greaterThanOrEqualTo(0)));
    }

    @Test
    void rulesEndpointExposesDetectionCoverage() throws Exception {
        mockMvc.perform(get("/api/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].ruleId", is("PL-BF-001")))
                .andExpect(jsonPath("$[0].mitreTechnique", is("T1110")))
                .andExpect(jsonPath("$[0].detectionStatus", is("ENABLED")))
                .andExpect(jsonPath("$[1].ruleId", is("PL-DBF-001")));
    }

    @Test
    void simulateEndpointGeneratesNewAlertsAndAddsEvents() throws Exception {
        mockMvc.perform(post("/api/simulate/brute-force"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(6))));
    }

    @Test
    void distributedSimulationGeneratesAnAccountBasedDetection() throws Exception {
        mockMvc.perform(post("/api/simulate/distributed-brute-force"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ruleId", is("PL-DBF-001")))
                .andExpect(jsonPath("$[0].sourceIps", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$[0].mitreTechnique", is("T1110")));

        mockMvc.perform(get("/api/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distributedAttackAlerts", is(1)));
    }

    @Test
    void simulationRateLimitReturnsTooManyRequests() throws Exception {
        mockMvc.perform(post("/api/simulate/brute-force"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/simulate/distributed-brute-force"))
                .andExpect(status().isTooManyRequests());
    }
}
