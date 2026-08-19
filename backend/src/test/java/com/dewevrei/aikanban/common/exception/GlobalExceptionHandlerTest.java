package com.dewevrei.aikanban.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.dewevrei.aikanban.common.api.ApiCode;
import com.dewevrei.aikanban.common.api.ErrorCode;
import com.dewevrei.aikanban.common.api.ApiResponse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void 도메인_예외를_계약된_HTTP_응답으로_변환한다() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleDomain(new DomainException(ErrorCode.DUPLICATE_PROJECT_NAME));

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.DUPLICATE_PROJECT_NAME.status());
        assertThat(response.getBody()).isEqualTo(ApiResponse.error(ErrorCode.DUPLICATE_PROJECT_NAME));
    }

    @Test
    void 예상하지_못한_예외의_내부_내용은_응답에_노출하지_않는다() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleUnexpected(new RuntimeException("secret sql"));

        assertThat(response.getBody()).isEqualTo(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
        assertThat(response.getBody().message()).doesNotContain("secret sql");
    }
}
