package io.commercedna.api.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that enforces W3C Trace Context and distributed tracing identifiers across all HTTP requests.
 * Injects 'X-Trace-Id' into MDC and HTTP response headers for auditability.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceContextFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String MDC_TRACE_KEY = "traceId";

    // Only accept standard UUID-format trace IDs from callers
    private static final java.util.regex.Pattern SAFE_TRACE_PATTERN =
            java.util.regex.Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String inbound = request.getHeader(TRACE_ID_HEADER);
        String traceId;
        if (inbound != null && SAFE_TRACE_PATTERN.matcher(inbound.trim()).matches()) {
            traceId = inbound.trim();
        } else {
            traceId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_TRACE_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_KEY);
        }
    }
}
