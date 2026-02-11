package com.skillstorm.finsight.documents_cases.exceptions;

import java.util.List;

/**
 * Thrown when a CTR or SAR document fails validation after PDF extraction
 * (e.g. missing first/last name, transaction amount, or date).
 * The message list is returned to the client as 400 Bad Request.
 */
public class DocumentValidationException extends RuntimeException {

    private final List<String> errors;

    public DocumentValidationException(List<String> errors) {
        super(errors == null || errors.isEmpty() ? "Validation failed" : String.join(" ", errors));
        this.errors = errors != null ? List.copyOf(errors) : List.of();
    }

    public List<String> getErrors() {
        return errors;
    }
}
