package com.dewevrei.aikanban.common.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void 성공_응답은_코드와_데이터를_담는다() {
        ApiResponse<String> response = ApiResponse.success(SuccessCode.PROJECT_READ, "data");

        assertThat(response.success()).isTrue();
        assertThat(response.code()).isEqualTo("PROJECT_READ");
        assertThat(response.message()).isEqualTo("프로젝트를 조회했습니다.");
        assertThat(response.data()).isEqualTo("data");
    }

    @Test
    void 오류_응답의_data는_항상_null이다() {
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.PROJECT_NOT_FOUND);

        assertThat(response.success()).isFalse();
        assertThat(response.code()).isEqualTo("PROJECT_NOT_FOUND");
        assertThat(response.message()).isEqualTo("프로젝트를 찾을 수 없습니다.");
        assertThat(response.data()).isNull();
    }

    @Test
    void 응답_종류와_상태가_맞지_않으면_거부한다() {
        assertThatThrownBy(() -> ApiResponse.success(ErrorCode.INVALID_REQUEST, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ApiResponse.error(SuccessCode.PROJECT_CREATED))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
