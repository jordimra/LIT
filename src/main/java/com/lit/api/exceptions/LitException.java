package com.lit.api.exceptions;

public class LitException extends RuntimeException {
    public LitException(String message) {
        super(message);
    }

    public LitException(String message, Throwable cause) {
        super(message, cause);
    }
}
