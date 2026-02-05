package com.skillstorm.finsight.suspect_registry.util;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Utility class for extracting user information from Spring Security context.
 *
 * <p>When the identity-auth-service is configured and Spring Security is set up,
 * this utility extracts user IDs and roles from the JWT in the security context.
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

            if (principal instanceof Jwt) {
                String subject = ((Jwt) principal).getSubject();
                try {
                    return Optional.of(UUID.fromString(subject));
                } catch (IllegalArgumentException e) {
                    log.warn("JWT subject is not a valid UUID: {}", subject);
                    return Optional.empty();
                }
            } else if (principal instanceof UUID) {
                return Optional.of((UUID) principal);
            } else if (principal instanceof String) {
                try {
                    return Optional.of(UUID.fromString((String) principal));
                } catch (IllegalArgumentException e) {
                    log.warn("Principal is a String but not a valid UUID: {}", principal);
                    return Optional.empty();
                }
            } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
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
            log.debug("Could not extract user ID from security context: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static final String ROLE_PREFIX = "ROLE_";

    /**
     * Extracts the current user's role from Spring Security authorities (e.g. ANALYST, LAW_ENFORCEMENT_USER, COMPLIANCE_USER).
     */
    public static Optional<String> getCurrentUserRole() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }
            return authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith(ROLE_PREFIX))
                    .map(a -> a.substring(ROLE_PREFIX.length()))
                    .findFirst();
        } catch (Exception e) {
            log.debug("Could not extract role from security context: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** Returns true if the current user has the ANALYST role. */
    public static boolean isAnalyst() {
        return "ANALYST".equals(getCurrentUserRole().orElse(null));
    }

    /** Returns true if the current user has the LAW_ENFORCEMENT_USER role. */
    public static boolean isLawEnforcement() {
        return "LAW_ENFORCEMENT_USER".equals(getCurrentUserRole().orElse(null));
    }

    /** Returns true if the current user has the COMPLIANCE_USER role. */
    public static boolean isComplianceUser() {
        return "COMPLIANCE_USER".equals(getCurrentUserRole().orElse(null));
    }
}
