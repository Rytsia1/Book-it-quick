package com.DTMK.Online.Bookkeeping.Website.Project.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized error handler for the bookkeeping REST API.
 * <p>
 * Without this class, exceptions that escape a controller bubble up to Spring's
 * default error handling, which returns either a stack-trace-style JSON or an
 * HTML "Whitelabel error page" — both of which the Vue frontend can't read via
 * {@code e.response.data.message}. Every handler here returns the same JSON
 * envelope so the axios layer in {@code src/utils/request.js} and the
 * {@code ElMessage.error(e?.response?.data?.message)} calls in the views always
 * surface a meaningful toast.
 * <p>
 * Envelope shape:
 * <pre>{@code
 * {
 *   "success": false,
 *   "message": "Human-readable error",
 *   "status":  400,
 *   "path":    "/api/budget",
 *   "timestamp": "2026-07-18T13:17:00Z"
 * }
 * }</pre>
 * <p>
 * Happy-path responses from individual controllers are intentionally left
 * untouched — this class only kicks in for exceptions that aren't already
 * handled inside the controller.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── 400 Bad Request ───────────────────────────────────────────────

    /**
     * Triggered when the request body cannot be deserialized — e.g. the user
     * sends {@code monthlyBudget: "abc"} (the "incorrect number format" case
     * from the task description) or supplies malformed JSON. Without this
     * handler Spring returns a default 400 with a Spring-internal body that
     * the frontend can't read.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        // Strip the heavy "at [Source: ..." part that Jackson appends; keep only the first sentence.
        String raw = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : ex.getMessage();
        String message = "Malformed request body: " + sanitize(raw);
        log.warn("HttpMessageNotReadableException at {}: {}", path(request), message);
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /** e.g. {@code GET /api/budget/abc} where the controller expects an Integer. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        String required = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "value";
        String message = "Parameter '" + ex.getName() + "' must be a valid " + required;
        log.warn("MethodArgumentTypeMismatchException at {}: {}", path(request), message);
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /** A required {@code @RequestParam} is missing from the URL. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, WebRequest request) {
        String message = "Required parameter '" + ex.getParameterName() + "' is missing";
        log.warn("MissingServletRequestParameterException at {}: {}", path(request), message);
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /** Triggered by {@code @Valid} on a request body (forward-compatible). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        String message = "Validation failed: " + ex.getBindingResult().getFieldErrorCount() + " field error(s)";
        log.warn("MethodArgumentNotValidException at {}: {}", path(request), message);
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    // ── 404 Not Found ─────────────────────────────────────────────────

    /**
     * Triggered when no controller matches the request URL. Spring only
     * throws this when {@code spring.mvc.throw-exception-if-no-handler-found=true}
     * is set in {@code application.yml} — see that file for the rationale.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoHandlerFoundException ex, WebRequest request) {
        String message = "Endpoint not found: " + ex.getHttpMethod() + " " + ex.getRequestURL();
        log.warn("NoHandlerFoundException: {}", message);
        return build(HttpStatus.NOT_FOUND, message, request);
    }

    // ── 405 Method Not Allowed ────────────────────────────────────────

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        String message = "HTTP " + ex.getMethod() + " is not supported for this endpoint";
        log.warn("HttpRequestMethodNotSupportedException at {}: {}", path(request), message);
        return build(HttpStatus.METHOD_NOT_ALLOWED, message, request);
    }

    // ── 409 Conflict ──────────────────────────────────────────────────

    /**
     * DB-level conflict (unique-constraint violation, FK violation, etc.).
     * The current controllers do their own duplicate check before reaching
     * the DB, so this is a safety net for cases we haven't anticipated.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, WebRequest request) {
        String message = "Data conflict: " + sanitize(rootCauseMessage(ex));
        log.warn("DataIntegrityViolationException at {}: {}", path(request), message);
        return build(HttpStatus.CONFLICT, message, request);
    }

    // ── 401 / 403 Auth ────────────────────────────────────────────────
    // SecurityConfig currently uses anyRequest().permitAll(), so these are
    // forward-compatible: the moment a JwtAuthenticationFilter is added,
    // expired/missing tokens will land here instead of producing a generic
    // 500 or Spring's default error response.

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, WebRequest request) {
        String message = "Authentication required: " + sanitize(ex.getMessage());
        log.warn("AuthenticationException at {}: {}", path(request), message);
        return build(HttpStatus.UNAUTHORIZED, message, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        log.warn("AccessDeniedException at {}: {}", path(request), ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "Access denied", request);
    }

    // ── 500 fallback ──────────────────────────────────────────────────
    // This is the critical one: any unhandled exception (NullPointerException,
    // a SQL connection drop, a bug in a controller that throws unexpectedly)
    // is caught here and turned into a clean JSON response instead of either
    // crashing the app or returning Spring's default HTML error page.

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, WebRequest request) {
        log.error("Unhandled exception at {}", path(request), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private static ResponseEntity<ErrorResponse> build(HttpStatus status, String message, WebRequest request) {
        ErrorResponse body = new ErrorResponse(
                false,
                message,
                status.value(),
                path(request),
                Instant.now().toString()
        );
        return ResponseEntity.status(status).body(body);
    }

    private static String path(WebRequest request) {
        if (request instanceof ServletWebRequest swr) {
            return swr.getRequest().getRequestURI();
        }
        return "unknown";
    }

    private static String sanitize(String s) {
        if (s == null) return "unknown error";
        // Trim Jackson's "at [Source: ..." appendix and any leading "JSON parse error: " prefix
        // so the user-facing message stays short.
        int at = s.indexOf(" at [Source:");
        if (at > 0) s = s.substring(0, at);
        int colon = s.indexOf(": ", s.indexOf('\n') >= 0 ? s.indexOf('\n') : 0);
        // Best-effort: cap the message length so an enormous SQL message can't blow up the toast.
        return s.length() > 240 ? s.substring(0, 240) + "..." : s;
    }

    private static String rootCauseMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur.getMessage();
    }

    /**
     * Uniform error envelope. Kept as a static nested record so this advice
     * is fully self-contained and we don't have to add a new DTO to the
     * {@code dto/} package for a single-purpose shape.
     */
    public record ErrorResponse(
            boolean success,
            String message,
            int status,
            String path,
            String timestamp
    ) {
        /** Map-style accessor so older Jackson versions serialize the record correctly. */
        public Map<String, Object> toMap() {
            Map<String, Object> m = new HashMap<>();
            m.put("success", success);
            m.put("message", message);
            m.put("status", status);
            m.put("path", path);
            m.put("timestamp", timestamp);
            return m;
        }
    }
}
