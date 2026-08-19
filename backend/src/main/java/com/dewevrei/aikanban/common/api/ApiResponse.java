package com.dewevrei.aikanban.common.api;

public record ApiResponse<T>(boolean success, String code, String message, T data) {

    public static <T> ApiResponse<T> success(ApiCode code, T data) {
        if (!code.isSuccess()) {
            throw new IllegalArgumentException("성공 응답에는 성공 ApiCode가 필요합니다.");
        }
        return new ApiResponse<>(true, code.code(), code.message(), data);
    }

    public static ApiResponse<Void> error(ApiCode code) {
        if (code.isSuccess()) {
            throw new IllegalArgumentException("오류 응답에는 오류 ApiCode가 필요합니다.");
        }
        return new ApiResponse<>(false, code.code(), code.message(), null);
    }
}
