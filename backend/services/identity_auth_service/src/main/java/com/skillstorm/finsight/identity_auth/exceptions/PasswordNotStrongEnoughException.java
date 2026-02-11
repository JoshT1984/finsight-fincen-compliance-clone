package com.skillstorm.finsight.identity_auth.exceptions;

public class PasswordNotStrongEnoughException extends RuntimeException {
    public PasswordNotStrongEnoughException(String message) {
        super(message);
    }
}
