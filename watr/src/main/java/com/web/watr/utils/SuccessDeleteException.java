package com.web.watr.utils;

import org.springframework.http.HttpStatus;

public class SuccessDeleteException extends RuntimeException {
    private final HttpStatus status;
    public SuccessDeleteException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
