package com.skillstorm.finsight.suspect_registry.exceptions;

public class ResourceConflictException extends RuntimeException {
    
    public ResourceConflictException(String message) {
        super(message);
    }
    
    public ResourceConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
