package com.skillstorm.finsight.suspect_registry.config;

import java.security.SecureRandom;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter that automatically encrypts SSN when persisting to database
 * and decrypts when reading from database.
 * 
 * Uses AES-256-GCM encryption via Java's built-in cryptography APIs.
 * The encrypted value is stored as a base64-encoded string in the database.
 * 
 * Note: This converter is registered as a Spring component so it can access
 * the encryption key bean. The @Convert annotation on the entity field
 * references this converter class.
 */
@Converter
@Component
public class SsnEncryptor implements AttributeConverter<String, String> {

    private static final Logger log = LoggerFactory.getLogger(SsnEncryptor.class);

    private static SecretKey encryptionKey;
    private static SecureRandom secureRandom;

    @Autowired
    public void setEncryptionKey(SecretKey encryptionKey) {
        SsnEncryptor.encryptionKey = encryptionKey;
    }

    @Autowired
    public void setSecureRandom(SecureRandom secureRandom) {
        SsnEncryptor.secureRandom = secureRandom;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank()) {
            return null;
        }
        if (encryptionKey == null || secureRandom == null) {
            throw new IllegalStateException(
                "Encryption not initialized. Ensure EncryptionConfig is loaded and ENCRYPTION_KEY is set."
            );
        }
        try {
            // Encrypt the SSN before storing in database
            return EncryptionConfig.encrypt(attribute, encryptionKey, secureRandom);
        } catch (Exception e) {
            log.error("Failed to encrypt SSN", e);
            throw new RuntimeException("Failed to encrypt SSN", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        if (encryptionKey == null) {
            throw new IllegalStateException(
                "Encryption not initialized. Ensure EncryptionConfig is loaded and ENCRYPTION_KEY is set."
            );
        }
        try {
            // Decrypt the SSN when reading from database
            return EncryptionConfig.decrypt(dbData, encryptionKey);
        } catch (Exception e) {
            log.error("Failed to decrypt SSN", e);
            throw new RuntimeException("Failed to decrypt SSN", e);
        }
    }
}
