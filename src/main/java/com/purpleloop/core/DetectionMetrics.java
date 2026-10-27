package com.purpleloop.core;

public class DetectionMetrics {

    // Declare your two fields here.
    private final int totalEvents;
    private final int totalAlerts;
    private final int uniqueSourceIps;

    // Create your constructor here.
    public DetectionMetrics(int totalEvents, int totalAlerts, int uniqueSourceIps) {
        this.totalEvents = totalEvents;
        this.totalAlerts = totalAlerts;
        this.uniqueSourceIps = uniqueSourceIps;
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
}
