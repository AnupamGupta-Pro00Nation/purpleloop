package com.purpleloop.core.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SimulationAccessInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final int MINIMUM_TOKEN_BYTES = 32;

    private final byte[] configuredToken;

    public SimulationAccessInterceptor(
            @Value("${purpleloop.simulation.access-token:}") String configuredToken) {
        this.configuredToken = configuredToken == null
                ? new byte[0]
                : configuredToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)
                || "GET".equalsIgnoreCase(method)
                || "HEAD".equalsIgnoreCase(method)) {
            return true;
        }

        if (configuredToken.length < MINIMUM_TOKEN_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Simulation access is not configured");
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null
                || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return reject(response);
        }

        String presentedToken = authorization.substring(BEARER_PREFIX.length());
        if (presentedToken.isBlank()
                || !MessageDigest.isEqual(
                        configuredToken,
                        presentedToken.getBytes(StandardCharsets.UTF_8))) {
            return reject(response);
        }

        return true;
    }

    private boolean reject(HttpServletResponse response) {
        response.setHeader("WWW-Authenticate", "Bearer");
        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "A valid simulation access token is required");
    }
}
