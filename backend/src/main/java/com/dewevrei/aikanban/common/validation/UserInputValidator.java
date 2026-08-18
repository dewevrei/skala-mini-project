package com.dewevrei.aikanban.common.validation;

import java.util.Objects;

import com.dewevrei.aikanban.common.api.ApiCode;
import com.dewevrei.aikanban.common.exception.DomainException;

public final class UserInputValidator {

    private UserInputValidator() {
    }

    public static String required(String value, int maxCodePoints) {
        String normalized = normalize(value);
        if (normalized == null || normalized.isEmpty() || exceeds(normalized, maxCodePoints)) {
            throw new IllegalArgumentException("필수 문자열이 비어 있거나 허용 길이를 초과했습니다.");
        }
        return normalized;
    }

    public static String required(String value, int maxCodePoints, ApiCode errorCode) {
        try {
            return required(value, maxCodePoints);
        } catch (IllegalArgumentException exception) {
            throw new DomainException(errorCode, exception);
        }
    }

    public static String optional(String value, int maxCodePoints) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        if (exceeds(normalized, maxCodePoints)) {
            throw new IllegalArgumentException("문자열이 허용 길이를 초과했습니다.");
        }
        return normalized;
    }

    public static String optional(String value, int maxCodePoints, ApiCode errorCode) {
        try {
            return optional(value, maxCodePoints);
        } catch (IllegalArgumentException exception) {
            throw new DomainException(errorCode, exception);
        }
    }

    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        if (stripped.codePoints().anyMatch(UserInputValidator::isForbiddenControl)) {
            throw new IllegalArgumentException("제어 문자는 입력할 수 없습니다.");
        }
        return stripped;
    }

    public static boolean exceeds(String value, int maxCodePoints) {
        Objects.requireNonNull(value, "value");
        if (maxCodePoints < 0) {
            throw new IllegalArgumentException("최대 길이는 음수일 수 없습니다.");
        }
        return value.codePointCount(0, value.length()) > maxCodePoints;
    }

    private static boolean isForbiddenControl(int codePoint) {
        return Character.getType(codePoint) == Character.CONTROL;
    }
}
