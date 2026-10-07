package com.lit.api.exceptions;

public class ActiveConflictException extends WorkspaceException {
    public ActiveConflictException(String message) {
        super(message);
    }
}
