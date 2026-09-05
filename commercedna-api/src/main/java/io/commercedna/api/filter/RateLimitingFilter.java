package io.commercedna.api.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Token Bucket Rate Limiting Filter.
 * Protects Agentic Commerce negotiation and settlement ingress points from DDoS,
 * bot flooding, and LLM token exhaustion attacks.
 * Conforms to RFC 7807 Problem Details for 429 Too Many Requests.
 */
@Component
@Order(10)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private static final int MAX_REQUESTS_PER_MINUTE = 120;
    private static final long WINDOW_MS = 60_000L;

    private static final class ClientBucket {
        long windowStartMs;
        final AtomicInteger count = new AtomicInteger(0);

        ClientBucket(long startMs) {
            this.windowStartMs = startMs;
        }
    }

    private final ConcurrentHashMap<String, ClientBucket> clientBuckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/api/v1/health")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/")
                || path.endsWith(".html")
                || path.endsWith(".css")
                || path.endsWith(".js")
                || path.endsWith(".ico");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String clientIdentifier = resolveClientKey(request);
        long now = System.currentTimeMillis();

        ClientBucket bucket = clientBuckets.compute(clientIdentifier, (key, existing) -> {
            if (existing == null || (now - existing.windowStartMs) >= WINDOW_MS) {
                ClientBucket newBucket = new ClientBucket(now);
                newBucket.count.set(1);
                return newBucket;
            } else {
                existing.count.incrementAndGet();
                return existing;
            }
        });

        int currentCount = bucket.count.get();
        int remaining = Math.max(0, MAX_REQUESTS_PER_MINUTE - currentCount);

        response.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS_PER_MINUTE));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

        if (currentCount > MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate limit exceeded for client: {}. Requests in window: {}", clientIdentifier, currentCount);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setHeader("Retry-After", "60");

            Map<String, Object> problemDetail = Map.of(
                    "type", "https://api.commercedna.io/errors/rate-limit-exceeded",
                    "title", "Too Many Requests",
                    "status", 429,
                    "detail", "Exceeded maximum allowed rate of " + MAX_REQUESTS_PER_MINUTE + " requests per minute. Retry in 60 seconds.",
                    "instance", request.getRequestURI(),
                    "timestamp", Instant.now().toString()
            );

            response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientKey(HttpServletRequest request) {
        String buyerDid = request.getHeader("X-Buyer-Agent-DID");
        if (buyerDid != null && !buyerDid.isBlank()) {
            return "did:" + buyerDid.trim();
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
