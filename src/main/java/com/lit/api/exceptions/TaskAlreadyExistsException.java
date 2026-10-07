package com.lit.api.exceptions;

public class TaskAlreadyExistsException extends TaskException {
    public TaskAlreadyExistsException(String message) {
        super(message);
    }
}
