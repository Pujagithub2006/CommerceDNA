package io.commercedna.api.exception;

import io.commercedna.api.filter.TraceContextFilter;
import io.commercedna.core.exception.CryptoVerificationException;
import io.commercedna.core.exception.DomainException;
import io.commercedna.core.exception.IdempotencyConflictException;
import io.commercedna.core.exception.InventoryExhaustedException;
import io.commercedna.core.exception.MarginFloorViolationException;
import io.commercedna.core.exception.QuantityQuotaExceededException;
import io.commercedna.core.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized exception handler rendering RFC 7807 Problem Details for all HTTP API endpoints.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource Not Found");
        problem.setType(URI.create("https://commercedna.io/errors/not-found"));
        enrichProblemDetail(problem);
        return problem;
    }

    @ExceptionHandler(MarginFloorViolationException.class)
    public ProblemDetail handleMarginFloorViolation(MarginFloorViolationException ex) {
        log.warn("Margin floor violation: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Margin Floor Violation");
        problem.setType(URI.create("https://commercedna.io/errors/margin-floor-violation"));
        enrichProblemDetail(problem);
        return problem;
    }

    @ExceptionHandler(QuantityQuotaExceededException.class)
    public ProblemDetail handleQuantityQuotaExceeded(QuantityQuotaExceededException ex) {
        log.warn("Quantity quota exceeded: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Quantity Quota Exceeded");
        problem.setType(URI.create("https://commercedna.io/errors/quantity-quota-exceeded"));
        enrichProblemDetail(problem);
        return problem;
    }

    @ExceptionHandler(InventoryExhaustedException.class)
    public ProblemDetail handleInventoryExhausted(InventoryExhaustedException ex) {
        log.warn("Inventory exhausted: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Inventory Exhausted");
        problem.setType(URI.create("https://commercedna.io/errors/inventory-exhausted"));
        enrichProblemDetail(problem);
        return problem;
    }

    @ExceptionHandler(CryptoVerificationException.class)
    public ProblemDetail handleCryptoVerification(CryptoVerificationException ex) {
        log.error("Cryptographic verification failed: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Cryptographic Verification Failed");
        problem.setType(URI.create("https://commercedna.io/errors/crypto-verification-failed"));
        enrichProblemDetail(problem);
        return problem;
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ProblemDetail handleIdempotencyConflict(IdempotencyConflictException ex) {
        log.warn("Idempotency conflict: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Idempotency Conflict");
        problem.setType(URI.create("https://commercedna.io/errors/idempotency-conflict"));
        enrichProblemDetail(problem);
        return problem;
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException ex) {
        log.warn("Domain violation: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Domain Rule Violation");
        problem.setType(URI.create("https://commercedna.io/errors/domain-violation"));
        enrichProblemDetail(problem);
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed for one or more fields.");
        problem.setTitle("Validation Failed");
        problem.setType(URI.create("https://commercedna.io/errors/validation-failed"));

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        problem.setProperty("invalidParams", fieldErrors);
        enrichProblemDetail(problem);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unhandled internal server error: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal error occurred. Reference the trace ID."
        );
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://commercedna.io/errors/internal-error"));
        enrichProblemDetail(problem);
        return problem;
    }

    private void enrichProblemDetail(ProblemDetail problem) {
        problem.setProperty("timestamp", Instant.now().toString());
        String traceId = MDC.get(TraceContextFilter.MDC_TRACE_KEY);
        if (traceId != null) {
            problem.setProperty("traceId", traceId);
        }
    }
}
