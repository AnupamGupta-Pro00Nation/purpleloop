package com.purpleloop.core.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "purpleloop.detection")
public class DetectionProperties {

    public static final int MAX_DISTRIBUTED_SOURCE_IPS = 65_536;

    private int bruteForceFailureThreshold = 3;
    private Duration detectionWindow = Duration.ofMinutes(5);
    private int distributedFailureThreshold = 6;
    private int minimumDistinctSourceIps = 3;
    private boolean bruteForceEnabled = true;
    private boolean distributedEnabled = true;
    private int simulationRateLimit = 20;
    private Duration simulationRateWindow = Duration.ofMinutes(1);
    private int maxEventHistorySize = 10_000;
    private int maxAlertHistorySize = 5_000;

    public int getBruteForceFailureThreshold() {
        return bruteForceFailureThreshold;
    }

    public void setBruteForceFailureThreshold(int value) {
        if (value < 1 || value > 100_000) {
            throw new IllegalArgumentException("Brute-force failure threshold must be between 1 and 100000");
        }
        bruteForceFailureThreshold = value;
    }

    public Duration getDetectionWindow() {
        return detectionWindow;
    }

    public void setDetectionWindow(Duration value) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("Detection window must be a positive duration");
        }
        detectionWindow = value;
    }

    public int getDistributedFailureThreshold() {
        return distributedFailureThreshold;
    }

    public void setDistributedFailureThreshold(int value) {
        if (value < 1 || value > 100_000) {
            throw new IllegalArgumentException("Distributed failure threshold must be between 1 and 100000");
        }
        distributedFailureThreshold = value;
    }

    public int getMinimumDistinctSourceIps() {
        return minimumDistinctSourceIps;
    }

    public void setMinimumDistinctSourceIps(int value) {
        if (value < 2 || value > MAX_DISTRIBUTED_SOURCE_IPS) {
            throw new IllegalArgumentException("Minimum distinct source IPs must be between 2 and 65536");
        }
        minimumDistinctSourceIps = value;
    }

    public boolean isBruteForceEnabled() {
        return bruteForceEnabled;
    }

    public void setBruteForceEnabled(boolean value) {
        bruteForceEnabled = value;
    }

    public boolean isDistributedEnabled() {
        return distributedEnabled;
    }

    public void setDistributedEnabled(boolean value) {
        distributedEnabled = value;
    }

    public int getSimulationRateLimit() {
        return simulationRateLimit;
    }

    public void setSimulationRateLimit(int value) {
        if (value < 1 || value > 10_000) {
            throw new IllegalArgumentException("Simulation rate limit must be between 1 and 10000");
        }
        simulationRateLimit = value;
    }

    public Duration getSimulationRateWindow() {
        return simulationRateWindow;
    }

    public void setSimulationRateWindow(Duration value) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("Simulation rate window must be a positive duration");
        }
        simulationRateWindow = value;
    }

    public int getMaxEventHistorySize() {
        return maxEventHistorySize;
    }

    public void setMaxEventHistorySize(int value) {
        if (value < 1 || value > 1_000_000) {
            throw new IllegalArgumentException("Maximum event history size must be between 1 and 1000000");
        }
        maxEventHistorySize = value;
    }

    public int getMaxAlertHistorySize() {
        return maxAlertHistorySize;
    }

    public void setMaxAlertHistorySize(int value) {
        if (value < 1 || value > 1_000_000) {
            throw new IllegalArgumentException("Maximum alert history size must be between 1 and 1000000");
        }
        maxAlertHistorySize = value;
    }
}
