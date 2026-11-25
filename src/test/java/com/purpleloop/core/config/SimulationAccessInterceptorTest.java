package com.purpleloop.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

class SimulationAccessInterceptorTest {

    private static final String TOKEN = "test-simulation-access-token-123";

    @Test
    void missingOrWeakServerConfigurationFailsClosed() {
        SimulationAccessInterceptor interceptor = new SimulationAccessInterceptor("too-short");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> interceptor.preHandle(request(null), new MockHttpServletResponse(), new Object()));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
    }

    @Test
    void validBearerTokenAuthorizesSimulationRequest() throws Exception {
        SimulationAccessInterceptor interceptor = new SimulationAccessInterceptor(TOKEN);

        assertTrue(interceptor.preHandle(
                request("Bearer " + TOKEN),
                new MockHttpServletResponse(),
                new Object()));
    }

    @Test
    void missingTokenIsRejectedWithBearerChallenge() {
        SimulationAccessInterceptor interceptor = new SimulationAccessInterceptor(TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> interceptor.preHandle(request(null), response, new Object()));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Bearer", response.getHeader("WWW-Authenticate"));
    }

    private MockHttpServletRequest request(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setServletPath("/api/simulate/brute-force");
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        return request;
    }
}
