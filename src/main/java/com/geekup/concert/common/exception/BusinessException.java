package com.geekup.concert.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final String code;
    private final String detail;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.detail = null;
    }

    public BusinessException(String code, String message, String detail) {
        super(message);
        this.code = code;
        this.detail = detail;
    }
}
