package com.purpleloop.core.model;

import java.time.LocalDateTime;

public class LoginEvent {

    private final String ip;
    private final boolean success;
    private final LocalDateTime timestamp;

    public LoginEvent(String ip, boolean success) {
        this.ip = ip;
        this.success = success;
        this.timestamp = LocalDateTime.now();
    }

    public String getIp() {
        return ip;
    }

    public boolean isSuccess() {
        return success;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}