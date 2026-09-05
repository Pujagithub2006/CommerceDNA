package io.commercedna.api.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitingFilterTest {

    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() {
        rateLimitingFilter = new RateLimitingFilter();
    }

    @Test
    @DisplayName("Should allow requests within limit and return rate limit headers")
    void shouldAllowRequestsWithinLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/negotiate/propose");
        request.setRemoteAddr("192.168.1.100");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        rateLimitingFilter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        assertEquals("120", response.getHeader("X-RateLimit-Limit"));
        assertEquals("119", response.getHeader("X-RateLimit-Remaining"));
    }

    @Test
    @DisplayName("Should bypass rate limiting for static assets and health probes")
    void shouldBypassStaticAssetsAndHealth() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/health");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        rateLimitingFilter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("Should reject requests exceeding burst limit with RFC 7807 429 status")
    void shouldRejectWhenRateLimitExceeded() throws Exception {
        String clientIp = "10.0.0.55";

        for (int i = 0; i < 120; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/negotiate/chat");
            request.setRemoteAddr(clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain filterChain = new MockFilterChain();
            rateLimitingFilter.doFilter(request, response, filterChain);
            assertEquals(200, response.getStatus());
        }

        // 121st request exceeds limit
        MockHttpServletRequest blockedRequest = new MockHttpServletRequest();
        blockedRequest.setRequestURI("/api/v1/negotiate/chat");
        blockedRequest.setRemoteAddr(clientIp);
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        MockFilterChain blockedChain = new MockFilterChain();

        rateLimitingFilter.doFilter(blockedRequest, blockedResponse, blockedChain);

        assertEquals(429, blockedResponse.getStatus());
        assertEquals("0", blockedResponse.getHeader("X-RateLimit-Remaining"));
        assertEquals("60", blockedResponse.getHeader("Retry-After"));
        assertTrue(blockedResponse.getContentAsString().contains("Too Many Requests"));
    }
}
