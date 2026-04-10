package com.dispatchops.domain.exception;

public class AddressValidationException extends RuntimeException {

    private final String details;

    public AddressValidationException(String message, String details) {
        super(message);
        this.details = details;
    }

    public String getDetails() {
        return details;
    }
}
