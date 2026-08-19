package com.dewevrei.aikanban.common.api;

import org.springframework.http.HttpStatus;

public interface ApiCode {

    String code();

    String message();

    HttpStatus status();

    default boolean isSuccess() {
        return status().is2xxSuccessful();
    }
}
