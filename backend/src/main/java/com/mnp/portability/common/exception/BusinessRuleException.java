package com.mnp.portability.common.exception;

import org.springframework.http.HttpStatus;


public class BusinessRuleException extends RuntimeException {

    private final HttpStatus status;

    private BusinessRuleException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static BusinessRuleException conflict(String message) {
        return new BusinessRuleException(HttpStatus.CONFLICT, message);
    }

    public static BusinessRuleException unprocessable(String message) {
        return new BusinessRuleException(HttpStatus.UNPROCESSABLE_CONTENT, message);
    }

    public HttpStatus getStatus() {
        return status;
    }
}