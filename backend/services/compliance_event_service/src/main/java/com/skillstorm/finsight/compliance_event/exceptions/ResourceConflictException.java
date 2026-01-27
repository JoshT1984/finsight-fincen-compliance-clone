package com.skillstorm.finsight.compliance_event.exceptions;

public class ResourceConflictException extends RuntimeException {
    /**
     * Constructs a new ResourceConflictException with the specified detail message.
     * 
     * @param message The detail message explaining the conflict
     */
    public ResourceConflictException(String message) {
        super(message);
    }
}