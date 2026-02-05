package com.skillstorm.finsight.suspect_registry.util;

import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * Utility class to generate encryption keys for local development.
 * 
 * Run this main method to generate a new encryption key:
 *   java -cp target/classes com.skillstorm.finsight.suspect_registry.util.KeyGeneratorUtil
 * 
 * Or use it programmatically in a test or one-time script.
 */
public class KeyGeneratorUtil {

    public static void main(String[] args) {
        try {
            SecretKey key = generateKey();
            String base64Key = Base64.getEncoder().encodeToString(key.getEncoded());
            System.out.println("\n========================================");
            System.out.println("Generated AES-256 Encryption Key:");
            System.out.println("========================================");
            System.out.println("ENCRYPTION_KEY=" + base64Key);
            System.out.println("========================================\n");
            System.out.println("Add this to your environment variables or application.yml");
            System.out.println("For local development, you can set it in your IDE run configuration.\n");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Failed to generate key: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generates a new AES-256 key
     */
    public static SecretKey generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256); // 256-bit key
        return keyGenerator.generateKey();
    }

    /**
     * Generates a base64-encoded key string
     */
    public static String generateKeyBase64() throws NoSuchAlgorithmException {
        SecretKey key = generateKey();
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
}
