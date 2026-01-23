package com.skillstorm.finsight.documents_cases.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the Documents Cases service.
 * 
 * <p>This class provides centralized exception handling for all REST controllers.
 * It converts application exceptions into appropriate HTTP responses using Spring's
 * ProblemDetail format (RFC 7807).
 * 
 * <p>Handled exceptions:
 * <ul>
 *   <li>{@link ResourceNotFoundException} - Returns HTTP 404 (Not Found)</li>
 *   <li>{@link ResourceConflictException} - Returns HTTP 409 (Conflict)</li>
 *   <li>{@link IllegalArgumentException} - Returns HTTP 400 (Bad Request) with full error logging</li>
 * </ul>
 * 
 * <p>All exception responses include:
 * <ul>
 *   <li>HTTP status code appropriate for the exception type</li>
 *   <li>Title describing the error category</li>
 *   <li>Detail message from the exception</li>
 *   <li>Request path where the error occurred</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles ResourceNotFoundException by returning an HTTP 404 response.
     * 
     * @param ex The ResourceNotFoundException that was thrown
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
     * @param ex The ResourceConflictException that was thrown
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
	 * <p>This handler logs the full exception details (including stack trace) for debugging
	 * purposes, while returning a user-friendly message to the client. This is particularly
	 * useful for business rule violations (e.g., invalid status transitions, constraint violations).
	 * 
	 * @param ex The IllegalArgumentException that was thrown
	 * @param request The HTTP request that triggered the exception
	 * @return A ProblemDetail with HTTP 400 status and exception message
	 */
	@ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex,
                                               HttpServletRequest request) {
        // Log the error message without stack trace
        log.error("IllegalArgumentException: {}", ex.getMessage());
        
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Bad Request");
        pd.setDetail(ex.getMessage());
        pd.setProperty("path", request.getRequestURI());
        return pd;
    }
	
}
