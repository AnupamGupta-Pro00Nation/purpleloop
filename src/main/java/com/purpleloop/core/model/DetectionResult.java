package com.purpleloop.core.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class DetectionResult {

    private final String id;
    private final String ruleId;
    private final String ruleName;
    private final String sourceIp;
    private final List<String> sourceIps;
    private final String username;
    private final int failedAttempts;
    private final String severity;
    private final String mitreTechnique;
    private final LocalDateTime detectedAt;
    private final LocalDateTime attackStartedAt;
    private final String message;

    public DetectionResult(String ruleName, String sourceIp, int failedAttempts, String severity, String mitreTechnique) {
        this(UUID.randomUUID().toString(), "PL-BF-001", ruleName, sourceIp,
                sourceIp == null ? List.of() : List.of(sourceIp), null,
                failedAttempts, severity, mitreTechnique, LocalDateTime.now(), null,
                ruleName + " detected from " + sourceIp);
    }

    public DetectionResult(String ruleId, String ruleName, String sourceIp, List<String> sourceIps,
            String username, int failedAttempts, String severity, String mitreTechnique,
            LocalDateTime detectedAt, LocalDateTime attackStartedAt, String message) {
        this(UUID.randomUUID().toString(), ruleId, ruleName, sourceIp, sourceIps, username,
                failedAttempts, severity, mitreTechnique, detectedAt, attackStartedAt, message);
    }

    public DetectionResult(String id, String ruleId, String ruleName, String sourceIp, List<String> sourceIps,
            String username, int failedAttempts, String severity, String mitreTechnique,
            LocalDateTime detectedAt, LocalDateTime attackStartedAt, String message) {
        this.id = id;
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.sourceIp = sourceIp;
        this.sourceIps = sourceIps == null ? List.of() : List.copyOf(sourceIps);
        this.username = username;
        this.failedAttempts = failedAttempts;
        this.severity = severity;
        this.mitreTechnique = mitreTechnique;
        this.detectedAt = detectedAt;
        this.attackStartedAt = attackStartedAt;
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public List<String> getSourceIps() {
        return sourceIps;
    }

    public String getUsername() {
        return username;
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

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public LocalDateTime getAttackStartedAt() {
        return attackStartedAt;
    }

    public String getMessage() {
        return message;
    }
}