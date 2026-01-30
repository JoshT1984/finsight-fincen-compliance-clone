package com.skillstorm.finsight.compliance_event.exceptions;

import java.nio.file.AccessDeniedException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;



/**
 * <ul>
 * <li>{@link ResourceNotFoundException} - Returns HTTP 404 (Not Found)</li>
 * <li>{@link ResourceConflictException} - Returns HTTP 409 (Conflict)</li>
 * <li>{@link IllegalArgumentException} - Returns HTTP 400 (Bad Request) with
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Handles missing endpoints and static resources as 404 Not Found
    @ExceptionHandler({ org.springframework.web.servlet.resource.NoResourceFoundException.class,
            org.springframework.web.servlet.NoHandlerFoundException.class })
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NOT_FOUND)
    public java.util.Map<String, Object> handleNotFound(Exception ex, jakarta.servlet.http.HttpServletRequest req) {
        return java.util.Map.of(
                "status", 404,
                "error", "Not Found",
                "path", req.getRequestURI(),
                "message", ex.getMessage());
    }

    /**
     * Handles ResourceNotFoundException by returning an HTTP 404 response.
     * 
     * @param ex      The ResourceNotFoundException that was thrown
     * @param request The HTTP request that triggered the exception
     * @return A ProblemDetail with HTTP 404 status and exception message
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex,
            HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setTitle("Not Found");
        pd.setDetail(ex.getMessage());
        pd.setProperty("path", request.getRequestURI());
        return pd;
    }

    /**
     * Handles ResourceConflictException by returning an HTTP 409 response.
     * 
     * @param ex      The ResourceConflictException that was thrown
     * @param request The HTTP request that triggered the exception
     * @return A ProblemDetail with HTTP 409 status and exception message
     */
    @ExceptionHandler(ResourceConflictException.class)
    public ProblemDetail handleConflict(ResourceConflictException ex,
            HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Conflict");
        pd.setDetail(ex.getMessage());
        pd.setProperty("path", request.getRequestURI());
        return pd;
    }

    /**
     * Handles IllegalArgumentException by returning an HTTP 400 response.
     * 
     * <p>
     * This handler logs the full exception details (including stack trace) for
     * debugging
     * purposes, while returning a user-friendly message to the client. This is
     * particularly
     * useful for business rule violations (e.g., capacity exceeded, negative
     * quantities).
     * 
     * @param ex      The IllegalArgumentException that was thrown
     * @param request The HTTP request that triggered the exception
     * @return A ProblemDetail with HTTP 400 status and exception message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex,
            HttpServletRequest request) {
        // Log the full error with details to console
        log.error("IllegalArgumentException at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Bad Request");
        pd.setDetail(ex.getMessage());
        pd.setProperty("path", request.getRequestURI());
        return pd;
    }

    // Handles authentication failures (missing/invalid credentials)
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex,
            HttpServletRequest request) {
        log.warn("Authentication failed at {}: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        pd.setTitle("Unauthorized");
        pd.setDetail("Authentication is required to access this resource.");
        pd.setProperty("path", request.getRequestURI());
        return pd;
    }

    // Handles authorization failures (authenticated but not permitted)
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex,
            HttpServletRequest request) {
        log.warn("Access denied at {}: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        pd.setTitle("Forbidden");
        pd.setDetail("You do not have permission to perform this action.");
        pd.setProperty("path", request.getRequestURI());
        return pd;
    }

    // Handles DataIntegrityViolations with DB-level Uniqueness Violations
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex,
            HttpServletRequest request) {
        Throwable mostSpecificCause = ex.getMostSpecificCause();
        String dbMessage = mostSpecificCause != null
                ? mostSpecificCause.getMessage()
                : ex.getMessage();

        log.warn("Data integrity violation at {}: {}", request.getRequestURI(), dbMessage);

        String detail = "Request violates a data constraint.";

        if (dbMessage != null) {
            if (dbMessage.contains("uq_users_email")) {
                detail = "Email address is already in use.";
            } else if (dbMessage.contains("foreign key")) {
                detail = "Request references a related resource that does not exist.";
            }
        }

        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setTitle("Conflict");
        pd.setDetail(detail);
        pd.setProperty("path", request.getRequestURI());

        return pd;
    }

    // Handles DTO Validation Errors (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        log.warn("Validation failed at {}: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Validation Failed");
        pd.setDetail("One or more fields are invalid.");
        pd.setProperty("path", request.getRequestURI());

        var errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage() == null ? "Invalid value" : fe.getDefaultMessage(),
                        (a, b) -> a // in case of duplicates
                ));

        pd.setProperty("errors", errors);
        return pd;
    }

    // Handles invalid Enums or Field Issues
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleBadJson(HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        log.warn("Bad request body at {}: {}", request.getRequestURI(), ex.getMessage());

        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Bad Request");
        pd.setDetail("Malformed JSON or invalid value.");
        pd.setProperty("path", request.getRequestURI());
        return pd;
    }

    // Handles validation failures for @RequestParam / @PathVariable (when using
    // @Validated)
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex,
            HttpServletRequest request) {

        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Validation Failed");
        pd.setDetail("One or more parameters are invalid.");
        pd.setProperty("path", request.getRequestURI());

        var errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        v -> v.getMessage(),
                        (a, b) -> a));

        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleAny(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setTitle("Internal Server Error");
        pd.setDetail("An unexpected error occurred.");
        pd.setProperty("path", request.getRequestURI());
        return pd;
    }

}