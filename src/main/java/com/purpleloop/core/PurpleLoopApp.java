package com.purpleloop.core;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.purpleloop.core.detector.BruteForceDetector;
import com.purpleloop.core.model.DetectionResult;
import com.purpleloop.core.model.LoginEvent;

public class PurpleLoopApp {

    public static void main(String[] args) {

        // Simulated authentication telemetry
        List<LoginEvent> events = List.of(
                new LoginEvent("192.168.1.10", false),
                new LoginEvent("192.168.1.10", false),
                new LoginEvent("192.168.1.20", true),
                new LoginEvent("192.168.1.10", false),
                new LoginEvent("192.168.1.10", false),
                new LoginEvent("192.168.1.10", false)
        );

        System.out.println("PurpleLoop Detection Engine");
        System.out.println("---------------------------");
        System.out.println(
                "Events received: " + events.size()
        );

        BruteForceDetector detector = new BruteForceDetector(3);
        List<DetectionResult> results = detector.analyze(events);
        for (DetectionResult result : results) {
            System.out.println("=== SECURITY ALERT ===");
            System.out.println("Rule: " + result.getRuleName());
            System.out.println("Source IP: " + result.getSourceIp());
            System.out.println("Failed Attempts: " + result.getFailedAttempts());
            System.out.println("Severity: " + result.getSeverity());
            System.out.println("MITRE ATT&CK: " + result.getMitreTechnique());
            System.out.println("======================");
        }
        System.out.println();
        Set<String> uniqueIps = results.stream()
            .map(DetectionResult::getSourceIp)
            .collect(Collectors.toSet());

        DetectionMetrics metrics = new DetectionMetrics(
                events.size(),
            results.size(),
            uniqueIps.size()
        );

        System.out.println();
        System.out.println("=== Detection Summary ===");
        System.out.println("Total events analyzed: " + metrics.getTotalEvents());
        System.out.println("Total alerts generated: " + metrics.getTotalAlerts());
        System.out.println("Unique source IPs detected: " + metrics.getUniqueSourceIps());
    }
}
