package com.lit.api.exceptions;

public class RepositoryAlreadyExistsException extends RepositoryException {
    public RepositoryAlreadyExistsException(String message) {
        super(message);
    }
}
