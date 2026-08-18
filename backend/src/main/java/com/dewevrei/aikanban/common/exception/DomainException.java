package com.dewevrei.aikanban.common.exception;

import com.dewevrei.aikanban.common.api.ApiCode;

public class DomainException extends RuntimeException {

    private final ApiCode code;

    public DomainException(ApiCode code) {
        super(code.message());
        if (code.isSuccess()) {
            throw new IllegalArgumentException("도메인 예외에는 오류 ApiCode가 필요합니다.");
        }
        this.code = code;
    }

    public DomainException(ApiCode code, Throwable cause) {
        super(code.message(), cause);
        if (code.isSuccess()) {
            throw new IllegalArgumentException("도메인 예외에는 오류 ApiCode가 필요합니다.");
        }
        this.code = code;
    }

    public ApiCode getCode() { return code; }
}
