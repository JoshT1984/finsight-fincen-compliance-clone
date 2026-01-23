package com.skillstorm.finsight.documents_cases.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;

@Service
public class S3Service {
    
    private static final Logger log = LoggerFactory.getLogger(S3Service.class);
    
    private final S3Client s3Client;
    private final String bucketName;
    
    public S3Service(S3Client s3Client, @Value("${aws.s3.bucket}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }
    
    /**
     * Generates a presigned URL for downloading a document from S3.
     * 
     * @param storagePath The S3 key/path to the document (e.g., "cases/123/document.pdf" or "s3://bucket/cases/123/document.pdf")
     * @param expirationMinutes How long the URL should be valid (default: 15 minutes)
     * @return A presigned URL that can be used to download the document
     */
    public String generateDownloadUrl(String storagePath, int expirationMinutes) {
        log.debug("Generating presigned download URL for: {} (expires in {} minutes)", storagePath, expirationMinutes);
        
        String key = extractKey(storagePath);
        
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Invalid storage path - could not extract S3 key from: " + storagePath);
        }
        
        try (S3Presigner presigner = S3Presigner.builder()
                .region(s3Client.serviceClientConfiguration().region())
                .credentialsProvider(s3Client.serviceClientConfiguration().credentialsProvider())
                .build()) {
            
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(
                    presignedRequestBuilder -> presignedRequestBuilder
                            .signatureDuration(Duration.ofMinutes(expirationMinutes))
                            .getObjectRequest(getObjectRequest));
            
            String url = presignedRequest.url().toString();
            
            log.info("Generated presigned download URL for: {} (valid for {} minutes)", key, expirationMinutes);
            return url;
        }
    }
    
    /**
     * Generates a presigned URL with default 15-minute expiration.
     */
    public String generateDownloadUrl(String storagePath) {
        return generateDownloadUrl(storagePath, 15);
    }
    
    /**
     * Uploads a file to S3.
     * 
     * @param file The multipart file to upload
     * @param s3Key The S3 key/path where the file should be stored (e.g., "case/123/document.pdf")
     * @param contentType The content type of the file (e.g., "application/pdf", "image/png")
     * @return The S3 key where the file was stored
     * @throws IOException If there's an error reading the file
     */
    public String uploadFile(MultipartFile file, String s3Key, String contentType) throws IOException {
        log.debug("Uploading file to S3: {} (content-type: {})", s3Key, contentType);
        
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType(contentType)
                    .build();
            
            PutObjectResponse response = s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
            
            log.info("Successfully uploaded file to S3: {} (ETag: {})", s3Key, response.eTag());
            return s3Key;
        } catch (Exception e) {
            log.error("Failed to upload file to S3: {}", s3Key, e);
            throw new IOException("Failed to upload file to S3: " + e.getMessage(), e);
        }
    }
    
    /**
     * Deletes a file from S3.
     * 
     * @param storagePath The S3 key/path to the document (e.g., "case/123/document.pdf" or "s3://bucket/case/123/document.pdf")
     * @throws IOException If there's an error deleting the file
     */
    public void deleteFile(String storagePath) throws IOException {
        log.info("Attempting to delete file from S3. Original storagePath: {}", storagePath);
        
        String key = extractKey(storagePath);
        
        log.info("Extracted S3 key: '{}' for bucket: '{}'", key, bucketName);
        
        if (key == null || key.isEmpty()) {
            throw new IOException("Invalid storage path - could not extract S3 key from: " + storagePath);
        }
        
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            log.debug("Sending delete request to S3 - bucket: {}, key: {}", bucketName, key);
            s3Client.deleteObject(deleteObjectRequest);
            log.info("Successfully deleted file from S3 - bucket: {}, key: {}", bucketName, key);
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            log.error("S3 error deleting file - bucket: {}, key: {}, error code: {}, error message: {}", 
                    bucketName, key, e.awsErrorDetails().errorCode(), e.awsErrorDetails().errorMessage(), e);
            throw new IOException("Failed to delete file from S3: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error deleting file from S3 - bucket: {}, key: {}", bucketName, key, e);
            throw new IOException("Failed to delete file from S3: " + e.getMessage(), e);
        }
    }
    
    /**
     * Copies/moves a file within S3 from one key to another.
     * 
     * @param sourceKey The source S3 key (e.g., "sar/123/document.pdf")
     * @param destinationKey The destination S3 key (e.g., "case/456/document.pdf")
     * @return The destination key
     * @throws IOException If there's an error copying the file
     */
    public String copyFile(String sourceKey, String destinationKey) throws IOException {
        log.info("Copying file in S3 from '{}' to '{}'", sourceKey, destinationKey);
        
        // Extract clean keys (remove s3://bucket/ prefix if present)
        String cleanSourceKey = extractKey(sourceKey);
        String cleanDestinationKey = extractKey(destinationKey);
        
        if (cleanSourceKey == null || cleanSourceKey.isEmpty()) {
            throw new IOException("Invalid source storage path - could not extract S3 key from: " + sourceKey);
        }
        if (cleanDestinationKey == null || cleanDestinationKey.isEmpty()) {
            throw new IOException("Invalid destination storage path - could not extract S3 key from: " + destinationKey);
        }
        
        try {
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                    .sourceBucket(bucketName)
                    .sourceKey(cleanSourceKey)
                    .destinationBucket(bucketName)
                    .destinationKey(cleanDestinationKey)
                    .build();
            
            CopyObjectResponse response = s3Client.copyObject(copyRequest);
            log.info("Successfully copied file in S3 from '{}' to '{}' (ETag: {})", 
                    cleanSourceKey, cleanDestinationKey, response.copyObjectResult().eTag());
            
            // Delete the source file after successful copy
            deleteFile(cleanSourceKey);
            log.info("Deleted source file after copy: {}", cleanSourceKey);
            
            return cleanDestinationKey;
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            log.error("S3 error copying file - source: {}, destination: {}, error code: {}, error message: {}", 
                    cleanSourceKey, cleanDestinationKey, e.awsErrorDetails().errorCode(), e.awsErrorDetails().errorMessage(), e);
            throw new IOException("Failed to copy file in S3: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error copying file in S3 - source: {}, destination: {}", cleanSourceKey, cleanDestinationKey, e);
            throw new IOException("Failed to copy file in S3: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extracts the S3 key from a storage path, removing s3://bucket/ prefix if present.
     */
    private String extractKey(String storagePath) {
        if (storagePath == null) {
            return null;
        }
        
        String key = storagePath;
        if (storagePath.startsWith("s3://")) {
            int thirdSlash = storagePath.indexOf("/", 5);
            if (thirdSlash != -1) {
                key = storagePath.substring(thirdSlash + 1);
            } else {
                key = storagePath.substring(5);
            }
        } else if (storagePath.startsWith(bucketName + "/")) {
            key = storagePath.substring(bucketName.length() + 1);
        }
        
        return key;
    }
}
