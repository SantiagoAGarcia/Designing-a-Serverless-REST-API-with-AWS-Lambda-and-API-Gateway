package com.serverless.workshop.domain;

/**
 * Domain exception thrown when order data fails validation rules.
 */
public class InvalidOrderException extends RuntimeException {
    public InvalidOrderException(String message) {
        super(message);
    }
}
