package com.dewevrei.aikanban.common.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.dewevrei.aikanban.common.api.ApiCode;
import com.dewevrei.aikanban.common.exception.DomainException;

class UserInputValidatorTest {

    @Test
    void 앞뒤_공백을_제거한다() {
        assertThat(UserInputValidator.required("  한글 이름  ", 10)).isEqualTo("한글 이름");
    }

    @Test
    void 필수값은_null과_빈값을_거부한다() {
        assertThatThrownBy(() -> UserInputValidator.required(null, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UserInputValidator.required("   ", 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 제어_문자를_거부한다() {
        assertThatThrownBy(() -> UserInputValidator.normalize("hello\u0000world"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UserInputValidator.normalize("hello\nworld"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UserInputValidator.normalize("\nhello"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UserInputValidator.normalize("hello\t"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UserInputValidator.normalize("\r hello \r"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 길이는_UTF16이_아닌_유니코드_문자_수로_계산한다() {
        assertThat(UserInputValidator.required("😀😀", 2)).isEqualTo("😀😀");
        assertThatThrownBy(() -> UserInputValidator.required("😀😀😀", 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 선택값은_null과_빈문자열을_허용한다() {
        assertThat(UserInputValidator.optional(null, 10)).isNull();
        assertThat(UserInputValidator.optional("  ", 10)).isEmpty();
    }

    @Test
    void 서비스가_지정한_도메인_오류_코드로_변환할_수_있다() {
        assertThatThrownBy(() -> UserInputValidator.required("  ", 100, ApiCode.INVALID_PROJECT_NAME))
                .isInstanceOfSatisfying(DomainException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ApiCode.INVALID_PROJECT_NAME));
    }
}
