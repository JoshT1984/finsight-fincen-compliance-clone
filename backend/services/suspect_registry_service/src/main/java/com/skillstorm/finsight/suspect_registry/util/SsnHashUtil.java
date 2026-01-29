package com.skillstorm.finsight.suspect_registry.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility for hashing and validating SSN.
 * 
 * SSN is stored encrypted (non-deterministic due to random IV), so we cannot
 * query by encrypted value. This hash is deterministic: same SSN always
 * produces same hash, enabling uniqueness checks without decrypting.
 * 
 * Normalizes SSN to digits only before hashing (e.g., "123-45-6789" and
 * "123456789" produce the same hash).
 */
public final class SsnHashUtil {

    private static final int SSN_DIGIT_COUNT = 9;

    private SsnHashUtil() {
    }

    /**
     * Normalizes SSN to digits only. Returns null if SSN is null/blank.
     */
    public static String normalize(String ssn) {
        if (ssn == null || ssn.isBlank()) {
            return null;
        }
        String digits = ssn.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? null : digits;
    }

    /**
     * Validates that SSN contains exactly 9 digits (dashes/spaces allowed).
     * Does nothing if SSN is null/blank (optional field).
     *
     * @throws IllegalArgumentException if SSN has non-digit characters or not exactly 9 digits
     */
    public static void validateSsn(String ssn) {
        if (ssn == null || ssn.isBlank()) {
            return;
        }
        String normalized = normalize(ssn);
        if (normalized == null) {
            throw new IllegalArgumentException("SSN must contain only digits");
        }
        if (normalized.length() != SSN_DIGIT_COUNT) {
            throw new IllegalArgumentException("SSN must contain exactly 9 digits");
        }
    }

    /**
     * Returns SHA-256 hash of normalized SSN (digits only), or null if SSN is null/blank.
     */
    public static String hash(String ssn) {
        if (ssn == null || ssn.isBlank()) {
            return null;
        }
        String normalized = ssn.replaceAll("[^0-9]", "");
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
