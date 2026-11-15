package com.purpleloop.core;

public class DetectionMetrics {

    private final int totalEvents;
    private final int totalAlerts;
    private final int uniqueSourceIps;
    private final int totalFailedLogins;
    private final int totalSuccessfulLogins;
    private final int bruteForceAlerts;
    private final int distributedAttackAlerts;
    private final long meanTimeToDetectMs;

    public DetectionMetrics(int totalEvents, int totalAlerts, int uniqueSourceIps) {
        this(totalEvents, totalAlerts, uniqueSourceIps, 0, 0, 0, 0, 0);
    }

    public DetectionMetrics(int totalEvents, int totalAlerts, int uniqueSourceIps,
            int totalFailedLogins, int totalSuccessfulLogins, int bruteForceAlerts,
            int distributedAttackAlerts, long meanTimeToDetectMs) {
        this.totalEvents = totalEvents;
        this.totalAlerts = totalAlerts;
        this.uniqueSourceIps = uniqueSourceIps;
        this.totalFailedLogins = totalFailedLogins;
        this.totalSuccessfulLogins = totalSuccessfulLogins;
        this.bruteForceAlerts = bruteForceAlerts;
        this.distributedAttackAlerts = distributedAttackAlerts;
        this.meanTimeToDetectMs = meanTimeToDetectMs;
    }

    public int getTotalEvents() {
        return totalEvents;
    }

    public int getTotalAlerts() {
        return totalAlerts;
    }

    public int getUniqueSourceIps() {
        return uniqueSourceIps;
    }

    public int getTotalFailedLogins() {
        return totalFailedLogins;
    }

    public int getTotalSuccessfulLogins() {
        return totalSuccessfulLogins;
    }

    public int getBruteForceAlerts() {
        return bruteForceAlerts;
    }

    public int getDistributedAttackAlerts() {
        return distributedAttackAlerts;
    }

    public long getMeanTimeToDetectMs() {
        return meanTimeToDetectMs;
    }
}
