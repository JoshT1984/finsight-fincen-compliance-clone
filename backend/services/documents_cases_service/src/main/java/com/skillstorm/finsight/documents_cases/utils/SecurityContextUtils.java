package com.skillstorm.finsight.documents_cases.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/**
 * Utility class for extracting user information from Spring Security context.
 * 
 * <p>This class works without requiring the identity-auth-service to be fully set up.
 * If Spring Security is not configured or no user is authenticated, it returns
 * Optional.empty(), allowing audit events to be created with null actorUserId.
 * 
 * <p>When the identity-auth-service is ready and Spring Security is configured,
 * this utility will automatically start extracting user IDs from the security context.
 */
public class SecurityContextUtils {
    
    private static final Logger log = LoggerFactory.getLogger(SecurityContextUtils.class);
    
    /**
     * Extracts the current user ID from Spring Security context.
     * 
     * @return Optional containing the user ID if available, empty otherwise
     */
    public static Optional<UUID> getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                log.debug("No authenticated user found in security context");
                return Optional.empty();
            }
            
            Object principal = authentication.getPrincipal();
            
            // Handle different principal types
            if (principal instanceof UUID) {
                return Optional.of((UUID) principal);
            } else if (principal instanceof String) {
                try {
                    return Optional.of(UUID.fromString((String) principal));
                } catch (IllegalArgumentException e) {
                    log.warn("Principal is a String but not a valid UUID: {}", principal);
                    return Optional.empty();
                }
            } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                // If using UserDetails, try to extract UUID from username
                String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
                try {
                    return Optional.of(UUID.fromString(username));
                } catch (IllegalArgumentException e) {
                    log.debug("Username is not a UUID: {}", username);
                    return Optional.empty();
                }
            } else {
                log.debug("Principal type not recognized: {}", principal != null ? principal.getClass().getName() : "null");
                return Optional.empty();
            }
        } catch (Exception e) {
            // If Spring Security is not available, SecurityContextHolder may throw an exception
            log.debug("Could not extract user ID from security context (Spring Security may not be configured): {}", e.getMessage());
            return Optional.empty();
        }
    }
}
