package com.lit.api.exceptions;

public class RepositoryNotFoundException extends RepositoryException {
    public RepositoryNotFoundException(String message) {
        super(message);
    }
}
