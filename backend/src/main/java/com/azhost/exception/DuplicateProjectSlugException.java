package com.azhost.exception;

public class DuplicateProjectSlugException extends RuntimeException {
    public DuplicateProjectSlugException(String message) {
        super(message);
    }
}
