package com.lit.api.exceptions;

public class TaskNotFoundException extends TaskException {
    public TaskNotFoundException(String message) {
        super(message);
    }
}
