package com.skillstorm.finsight.suspect_registry.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.skillstorm.finsight.suspect_registry.models.OrganizationType;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Global exception handler for the Suspect Registry service.
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
 *   <li>{@link DataIntegrityViolationException} - Returns HTTP 400 (Bad Request) for database constraint violations</li>
 *   <li>{@link HttpMessageNotReadableException} - Returns HTTP 400 (Bad Request) for invalid JSON, including invalid enum values (e.g., organization type)</li>
 *   <li>{@link AccessDeniedException} - Returns HTTP 403 (Forbidden) when compliance users attempt write operations</li>
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
     * Handles AccessDeniedException (e.g., compliance user attempting write) by returning an HTTP 403 response.
     *
     * @param ex The AccessDeniedException that was thrown
     * @param request The HTTP request that triggered the exception
     * @return A ProblemDetail with HTTP 403 status and message
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex,
                                           HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        pd.setTitle("Forbidden");
        pd.setDetail(ex.getMessage() != null ? ex.getMessage() : "Compliance users have read-only access to the suspect registry");
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
	/**
	 * Handles HttpMessageNotReadableException (e.g., invalid JSON, invalid enum values) by returning an HTTP 400 response.
	 *
	 * <p>When an invalid organization type is sent (e.g., "type": "INVALID"), Jackson throws this exception
	 * with an InvalidFormatException cause. This handler detects OrganizationType validation failures and
	 * returns a clear message listing acceptable values.
	 *
	 * @param ex The HttpMessageNotReadableException that was thrown
	 * @param request The HTTP request that triggered the exception
	 * @return A ProblemDetail with HTTP 400 status and user-friendly message
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
	                                                   HttpServletRequest request) {
		String detailMessage = "Invalid request body. Please check your JSON format and field values.";
		Throwable cause = ex.getCause();

		if (cause instanceof InvalidFormatException ife) {
			Class<?> targetType = ife.getTargetType();
			if (targetType != null && targetType == OrganizationType.class) {
				String invalidValue = ife.getValue() != null ? ife.getValue().toString() : "null";
				detailMessage = "Invalid organization type: '" + invalidValue + "'. " +
						"Acceptable types are: CARTEL, GANG, TERRORIST, FRAUD_RING, MONEY_LAUNDERING, OTHER";
			}
		}

		log.error("HttpMessageNotReadableException: {}", ex.getMessage());
		ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		pd.setTitle("Bad Request");
		pd.setDetail(detailMessage);
		pd.setProperty("path", request.getRequestURI());
		return pd;
	}

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
	
	/**
	 * Handles DataIntegrityViolationException (e.g., foreign key violations) by returning an HTTP 400 response.
	 * 
	 * <p>This handler catches database constraint violations, particularly foreign key violations
	 * when trying to reference a non-existent suspect, organization, address, or alias record.
	 * 
	 * @param ex The DataIntegrityViolationException that was thrown
	 * @param request The HTTP request that triggered the exception
	 * @return A ProblemDetail with HTTP 400 status and a user-friendly error message
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex,
	                                                   HttpServletRequest request) {
		String errorMessage = ex.getMessage();
		String detailMessage = "Data integrity violation";
		
		// Check for foreign key constraint violations
		if (errorMessage != null) {
			// Foreign key violations
			if (errorMessage.contains("fk_alias_suspect") || errorMessage.contains("suspect_id")) {
				// Extract suspect ID from error message if possible
				if (errorMessage.contains("Key (suspect_id)=")) {
					int startIdx = errorMessage.indexOf("Key (suspect_id)=") + 17;
					int endIdx = errorMessage.indexOf(")", startIdx);
					if (endIdx > startIdx) {
						String suspectId = errorMessage.substring(startIdx, endIdx);
						detailMessage = "Suspect with ID " + suspectId + " does not exist";
					} else {
						detailMessage = "The specified suspect does not exist";
					}
				} else {
					detailMessage = "The specified suspect does not exist";
				}
			} else if (errorMessage.contains("fk_suspect_address_suspect") || errorMessage.contains("fk_suspect_address_address")) {
				if (errorMessage.contains("suspect_id")) {
					detailMessage = "The specified suspect does not exist";
				} else if (errorMessage.contains("address_id")) {
					detailMessage = "The specified address does not exist";
				} else {
					detailMessage = "Referenced record does not exist";
				}
			} else if (errorMessage.contains("fk_suspect_org_suspect") || errorMessage.contains("fk_suspect_org_org")) {
				if (errorMessage.contains("suspect_id")) {
					detailMessage = "The specified suspect does not exist";
				} else if (errorMessage.contains("org_id")) {
					detailMessage = "The specified organization does not exist";
				} else {
					detailMessage = "Referenced record does not exist";
				}
			} else if (errorMessage.contains("violates foreign key constraint")) {
				detailMessage = "Referenced record does not exist";
			}
			// CHECK constraint violations (risk level, alias type, address type, etc.)
			else if (errorMessage.contains("violates check constraint") || errorMessage.contains("check constraint") || errorMessage.contains("chk_org_type")) {
				if (errorMessage.contains("risk_level")) {
					detailMessage = "Invalid risk level. Must be one of: UNKNOWN, LOW, MEDIUM, HIGH";
				} else if (errorMessage.contains("alias_type")) {
					detailMessage = "Invalid alias type. Must be one of: AKA, LEGAL, NICKNAME, BUSINESS";
				} else if (errorMessage.contains("address_type")) {
					detailMessage = "Invalid address type. Must be one of: HOME, WORK, MAILING, UNKNOWN";
				} else if (errorMessage.contains("org_type") || errorMessage.contains("chk_org_type")) {
					detailMessage = "Invalid organization type. Acceptable types are: CARTEL, GANG, TERRORIST, FRAUD_RING, MONEY_LAUNDERING, OTHER";
				} else {
					detailMessage = "Data validation failed. Please check your input values";
				}
			}
		}
		
		log.error("DataIntegrityViolationException: {}", detailMessage);
		
		ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		pd.setTitle("Bad Request");
		pd.setDetail(detailMessage);
		pd.setProperty("path", request.getRequestURI());
		return pd;
	}
	
}
