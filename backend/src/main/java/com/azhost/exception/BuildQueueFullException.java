package com.azhost.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class BuildQueueFullException extends RuntimeException {
    public BuildQueueFullException(String message) {
        super(message);
    }
}
