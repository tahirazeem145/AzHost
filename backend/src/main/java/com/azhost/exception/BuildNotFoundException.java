package com.azhost.exception;

public class BuildNotFoundException extends RuntimeException {

    public BuildNotFoundException(String message) {
        super(message);
    }
}
