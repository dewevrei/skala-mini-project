package com.dewevrei.aikanban.common.api;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode implements ApiCode {
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    CSRF_TOKEN_INVALID(HttpStatus.FORBIDDEN, "요청 보안 정보가 올바르지 않습니다. 다시 시도해 주세요."),
    OAUTH_PROFILE_INVALID(HttpStatus.BAD_REQUEST, "Google 계정 정보를 확인할 수 없습니다."),
    OAUTH_EMAIL_INVALID(HttpStatus.BAD_REQUEST, "Google 이메일 정보를 확인할 수 없습니다."),
    DUPLICATE_GOOGLE_ID(HttpStatus.CONFLICT, "이미 등록된 Google 계정입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "닉네임을 확인해 주세요."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    NICKNAME_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "닉네임을 생성하지 못했습니다."),
    SESSION_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "로그인 서비스를 사용할 수 없습니다. 잠시 후 다시 시도해 주세요."),
    INVALID_PROJECT_NAME(HttpStatus.BAD_REQUEST, "프로젝트 이름을 확인해 주세요."),
    INVALID_PROJECT_DESCRIPTION(HttpStatus.BAD_REQUEST, "프로젝트 설명을 확인해 주세요."),
    DUPLICATE_PROJECT_NAME(HttpStatus.CONFLICT, "같은 이름의 프로젝트가 이미 있습니다."),
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "프로젝트를 찾을 수 없습니다."),
    PROJECT_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "프로젝트를 생성하지 못했습니다."),
    PROJECT_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "프로젝트를 삭제하지 못했습니다."),
    INVALID_COLUMN_NAME(HttpStatus.BAD_REQUEST, "열 이름을 확인해 주세요."),
    DUPLICATE_COLUMN_NAME(HttpStatus.CONFLICT, "같은 이름의 열이 이미 있습니다."),
    COLUMN_NOT_FOUND(HttpStatus.NOT_FOUND, "보드 열을 찾을 수 없습니다."),
    INVALID_COLUMN_ORDER(HttpStatus.BAD_REQUEST, "보드 열 순서가 올바르지 않습니다."),
    LAST_COLUMN_DELETE_FORBIDDEN(HttpStatus.CONFLICT, "프로젝트의 마지막 열은 삭제할 수 없습니다."),
    COLUMN_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "보드 열을 삭제하지 못했습니다."),
    INVALID_TASK_TITLE(HttpStatus.BAD_REQUEST, "작업 제목을 입력해 주세요."),
    INVALID_TASK_DESCRIPTION(HttpStatus.BAD_REQUEST, "작업 설명을 확인해 주세요."),
    INVALID_TASK_DATE(HttpStatus.BAD_REQUEST, "작업 날짜를 확인해 주세요."),
    INVALID_TASK_PRIORITY(HttpStatus.BAD_REQUEST, "작업 우선순위가 올바르지 않습니다."),
    INVALID_AI_DESCRIPTION(HttpStatus.BAD_REQUEST, "AI 생성을 위한 설명을 입력해 주세요."),
    READ_ONLY_FIELD(HttpStatus.BAD_REQUEST, "변경할 수 없는 항목이 포함되어 있습니다."),
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "작업을 찾을 수 없습니다."),
    INVALID_TASK_MOVE(HttpStatus.BAD_REQUEST, "작업을 이동할 위치가 올바르지 않습니다."),
    INVALID_SEARCH_QUERY(HttpStatus.BAD_REQUEST, "검색어를 확인해 주세요."),
    TASK_BATCH_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "생성된 작업을 저장하지 못했습니다."),
    TASK_FALLBACK_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "작업을 저장하지 못했습니다. 잠시 후 다시 시도해 주세요."),
    TASK_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "작업을 삭제하지 못했습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 내용을 확인해 주세요."),
    MALFORMED_JSON(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 대상을 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public String code() {
        return name();
    }
}
