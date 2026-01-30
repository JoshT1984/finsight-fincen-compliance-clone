package com.skillstorm.finsight.suspect_registry.config;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for AES-GCM encryption using Java's built-in cryptography APIs.
 * 
 * Uses AES-256-GCM (Galois/Counter Mode) which provides authenticated encryption.
 * 
 * The encryption key should be provided via environment variable ENCRYPTION_KEY
 * or application property encryption.key. The key must be base64-encoded and 32 bytes (256 bits).
 * 
 * To generate a key: 
 *   KeyGenerator keyGen = KeyGenerator.getInstance("AES");
 *   keyGen.init(256);
 *   SecretKey key = keyGen.generateKey();
 *   String base64Key = Base64.getEncoder().encodeToString(key.getEncoded());
 * 
 * For production, store the key securely (e.g., AWS Secrets Manager, HashiCorp Vault).
 */
@Configuration
public class EncryptionConfig {

    private static final Logger log = LoggerFactory.getLogger(EncryptionConfig.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits for GCM
    private static final int GCM_TAG_LENGTH = 16; // 128 bits for authentication tag
    private static final int KEY_LENGTH = 32; // 256 bits

    /** Default key for local dev only - must match the default in application.yml 
     * This is a valid base64-encoded 32-byte (256-bit) AES key for local development only.
     * 32 bytes base64-encoded = 44 characters (with padding).
     * This key decodes to exactly 32 bytes: AAAA...AAA= (all zeros, safe for dev only) */
    private static final String DEFAULT_DEV_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    @Value("${encryption.key:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=}")
    private String encryptionKeyBase64;

    @Bean
    public SecretKey encryptionKey() {
        // Use provided key, or fall back to default dev key if empty/blank
        String keyToUse = encryptionKeyBase64;
        if (keyToUse == null || keyToUse.isBlank() || 
            keyToUse.equals("${ENCRYPTION_KEY:}") || 
            keyToUse.startsWith("${")) {
            keyToUse = DEFAULT_DEV_KEY;
            log.warn("Using default encryption key for local development. Set ENCRYPTION_KEY environment variable in production!");
        }

        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyToUse.trim());
            if (keyBytes.length != KEY_LENGTH) {
                throw new IllegalArgumentException(
                    "Encryption key must be " + KEY_LENGTH + " bytes (256 bits). Got: " + keyBytes.length + " bytes. " +
                    "Key length: " + keyToUse.length() + " chars, Key preview: " + 
                    (keyToUse.length() > 20 ? keyToUse.substring(0, 20) + "..." : keyToUse)
                );
            }
            return new SecretKeySpec(keyBytes, "AES");
        } catch (IllegalArgumentException e) {
            log.error("Failed to decode encryption key. Key length: {} chars, Key preview: {}", 
                keyToUse.length(), 
                keyToUse.length() > 20 ? keyToUse.substring(0, 20) + "..." : keyToUse);
            throw new IllegalStateException("Invalid encryption key format. Must be base64-encoded 32-byte key. Error: " + e.getMessage(), e);
        }
    }

    @Bean
    public SecureRandom secureRandom() {
        return new SecureRandom();
    }

    /**
     * Encrypts plaintext using AES-256-GCM
     */
    public static String encrypt(String plaintext, SecretKey key, SecureRandom random) throws Exception {
        if (plaintext == null || plaintext.isBlank()) {
            return null;
        }

        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        random.nextBytes(iv);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);

        // Encrypt
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));

        // Combine IV and ciphertext
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
        byteBuffer.put(iv);
        byteBuffer.put(ciphertext);
        byte[] encrypted = byteBuffer.array();

        // Return base64-encoded result
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * Decrypts ciphertext using AES-256-GCM
     */
    public static String decrypt(String ciphertextBase64, SecretKey key) throws Exception {
        if (ciphertextBase64 == null || ciphertextBase64.isBlank()) {
            return null;
        }

        // Decode from base64
        byte[] encrypted = Base64.getDecoder().decode(ciphertextBase64);

        // Extract IV and ciphertext
        ByteBuffer byteBuffer = ByteBuffer.wrap(encrypted);
        byte[] iv = new byte[GCM_IV_LENGTH];
        byteBuffer.get(iv);
        byte[] ciphertext = new byte[byteBuffer.remaining()];
        byteBuffer.get(ciphertext);

        // Decrypt
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
        byte[] plaintext = cipher.doFinal(ciphertext);

        return new String(plaintext, "UTF-8");
    }
}
