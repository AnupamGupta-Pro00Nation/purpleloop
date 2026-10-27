package com.purpleloop.core.detector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.purpleloop.core.model.DetectionResult;
import com.purpleloop.core.model.LoginEvent;

public class BruteForceDetector {

    private final int threshold;

    public BruteForceDetector(int threshold) {
        if (threshold < 1) {
            throw new IllegalArgumentException(
                    "Threshold must be at least 1"
            );
        }

        this.threshold = threshold;
    }

    public List<DetectionResult> analyze(List<LoginEvent> events) {
        List<DetectionResult> results = new ArrayList<>();
        Map<String, Integer> failedAttempts = new HashMap<>();

        for (LoginEvent event : events) {

            if (!event.isSuccess()) {

                String ip = event.getIp();

                int count = failedAttempts.getOrDefault(ip, 0) + 1;

                failedAttempts.put(ip, count);

                if (count == threshold) {
                    DetectionResult result = new DetectionResult(
                            "Repeated Failed Logins",
                            ip,
                            count,
                            "HIGH",
                            "T1110"
                    );
                    results.add(result);
                }
            }
        }
        return results;
    }
}
