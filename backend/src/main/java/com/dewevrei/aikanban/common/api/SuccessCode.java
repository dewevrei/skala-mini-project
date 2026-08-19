package com.dewevrei.aikanban.common.api;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum SuccessCode implements ApiCode {
    CSRF_TOKEN_ISSUED(HttpStatus.OK, "요청 보안 정보를 발급했습니다."),
    LOGOUT_SUCCEEDED(HttpStatus.OK, "로그아웃되었습니다."),
    USER_READ(HttpStatus.OK, "사용자 정보를 조회했습니다."),
    USER_UPDATED(HttpStatus.OK, "사용자 정보를 변경했습니다."),
    PROJECT_LISTED(HttpStatus.OK, "프로젝트 목록을 조회했습니다."),
    PROJECT_CREATED(HttpStatus.CREATED, "프로젝트가 생성되었습니다."),
    PROJECT_READ(HttpStatus.OK, "프로젝트를 조회했습니다."),
    PROJECT_UPDATED(HttpStatus.OK, "프로젝트가 수정되었습니다."),
    PROJECT_DELETED(HttpStatus.OK, "프로젝트가 삭제되었습니다."),
    COLUMN_CREATED(HttpStatus.CREATED, "보드 열이 생성되었습니다."),
    COLUMN_UPDATED(HttpStatus.OK, "보드 열이 수정되었습니다."),
    COLUMNS_REORDERED(HttpStatus.OK, "보드 열 순서가 변경되었습니다."),
    COLUMN_DELETED(HttpStatus.OK, "보드 열이 삭제되었습니다."),
    BOARD_READ(HttpStatus.OK, "보드를 조회했습니다."),
    ITEMS_READ(HttpStatus.OK, "작업 목록을 조회했습니다."),
    TASK_CREATED(HttpStatus.CREATED, "작업이 등록되었습니다."),
    TASKS_CREATED(HttpStatus.CREATED, "작업이 등록되었습니다."),
    TASK_READ(HttpStatus.OK, "작업을 조회했습니다."),
    TASK_UPDATED(HttpStatus.OK, "작업이 수정되었습니다."),
    TASK_DATES_UPDATED(HttpStatus.OK, "작업 날짜가 변경되었습니다."),
    TASK_DELETED(HttpStatus.OK, "작업이 삭제되었습니다."),
    TASK_MOVED(HttpStatus.OK, "작업이 이동되었습니다.");

    private final HttpStatus status;
    private final String message;

    SuccessCode(HttpStatus status, String message) {
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
