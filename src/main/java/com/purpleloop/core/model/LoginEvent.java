package com.purpleloop.core.model;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

public class LoginEvent {

    public static final int MAX_USERNAME_LENGTH = 128;
    private static final int MAX_IP_LENGTH = 45;
    private static final int MAX_ID_LENGTH = 128;

    private final String id;
    private final String ip;
    private final boolean success;
    private final String username;
    private final LocalDateTime timestamp;

    public LoginEvent(String ip, boolean success) {
        this(UUID.randomUUID().toString(), ip, success, null, LocalDateTime.now());
    }

    public LoginEvent(String ip, boolean success, String username) {
        this(UUID.randomUUID().toString(), ip, success, username, LocalDateTime.now());
    }

    public LoginEvent(String ip, boolean success, String username, LocalDateTime timestamp) {
        this(UUID.randomUUID().toString(), ip, success, username, timestamp);
    }

    public LoginEvent(String id, String ip, boolean success, String username, LocalDateTime timestamp) {
        if (id == null || id.isBlank() || id.length() > MAX_ID_LENGTH) {
            throw new IllegalArgumentException("Event ID must be nonblank and at most 128 characters");
        }
        String normalizedIp = normalizeIp(ip);
        if (normalizedIp == null) {
            throw new IllegalArgumentException("IP address must be a valid IPv4 or IPv6 address");
        }
        if (username != null && username.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException("Username must not exceed 128 characters");
        }
        if (username != null && username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank; use the two-argument constructor when unknown");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Event timestamp must not be null");
        }
        this.id = id;
        this.ip = normalizedIp;
        this.success = success;
        this.username = username;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getUsername() {
        return username;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public boolean hasValidTargetAccount() {
        return username != null
                && !username.isBlank()
                && !username.equalsIgnoreCase("unknown")
                && username.length() <= MAX_USERNAME_LENGTH;
    }

    private static String normalizeIp(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_IP_LENGTH) {
            return null;
        }
        if (value.indexOf(':') >= 0) {
            if (!value.matches("[0-9a-fA-F:.]+")) {
                return null;
            }
            try {
                InetAddress address = InetAddress.getByName(value);
                return address instanceof Inet6Address
                        ? address.getHostAddress().toLowerCase(Locale.ROOT) : null;
            } catch (UnknownHostException exception) {
                return null;
            }
        }

        String[] octets = value.split("\\.", -1);
        if (octets.length != 4) {
            return null;
        }
        StringBuilder normalized = new StringBuilder();
        for (String octet : octets) {
            if (octet.isEmpty() || !octet.matches("\\d{1,3}")
                    || (octet.length() > 1 && octet.charAt(0) == '0')) {
                return null;
            }
            int number = Integer.parseInt(octet);
            if (number > 255) {
                return null;
            }
            if (!normalized.isEmpty()) {
                normalized.append('.');
            }
            normalized.append(number);
        }
        return normalized.toString();
    }
}
