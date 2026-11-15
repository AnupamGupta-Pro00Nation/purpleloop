package com.purpleloop.core.model;

public class DetectionRule {

    private final String ruleId;
    private final String ruleName;
    private final String description;
    private final String severity;
    private final String mitreTechnique;
    private final String detectionStatus;

    public DetectionRule(String ruleId, String ruleName, String description, String severity,
            String mitreTechnique, boolean enabled) {
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.description = description;
        this.severity = severity;
        this.mitreTechnique = mitreTechnique;
        this.detectionStatus = enabled ? "ENABLED" : "DISABLED";
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getDescription() {
        return description;
    }

    public String getSeverity() {
        return severity;
    }

    public String getMitreTechnique() {
        return mitreTechnique;
    }

    public String getDetectionStatus() {
        return detectionStatus;
    }

    public boolean isEnabled() {
        return "ENABLED".equals(detectionStatus);
    }
}
