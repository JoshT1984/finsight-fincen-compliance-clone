package com.skillstorm.finsight.compliance_event.exceptions;

public class ResourceNotFoundException extends RuntimeException {
    /**
     * Constructs a new ResourceNotFoundException with the specified detail message.
     * 
     * @param message The detail message explaining which resource was not found
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}