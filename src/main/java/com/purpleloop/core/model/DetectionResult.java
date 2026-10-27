package com.purpleloop.core.model;

public class DetectionResult {

    private final String ruleName;
    private final String sourceIp;
    private final int failedAttempts;
    private final String severity;
    private final String mitreTechnique;

    // TODO: Create a constructor that accepts all five fields.
    public DetectionResult(String ruleName, String sourceIp, int failedAttempts, String severity, String mitreTechnique) {
        this.ruleName = ruleName;
        this.sourceIp = sourceIp;
        this.failedAttempts = failedAttempts;
        this.severity = severity;
        this.mitreTechnique = mitreTechnique;
    }

    // TODO: Create getter methods for all five fields.
    public String getRuleName() {
        return ruleName;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public String getSeverity() {
        return severity;
    }

    public String getMitreTechnique() {
        return mitreTechnique;
    }
}
