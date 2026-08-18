package com.dewevrei.aikanban.aitask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dewevrei.aikanban.common.api.ApiCode;
import com.dewevrei.aikanban.common.exception.DomainException;
import com.dewevrei.aikanban.domain.Project;
import com.dewevrei.aikanban.repository.ProjectRepository;

@ExtendWith(MockitoExtension.class)
class AiTaskServiceTest {
    @Mock ProjectRepository projects;
    @Mock AiTaskGenerator generator;
    @Mock AiTaskPersistence persistence;
    private AiTaskService service;

    @BeforeEach
    void setUp() { service = new AiTaskService(projects, generator, persistence); }

    @Test
    void 유효_결과는_한번만_호출하고_batch로_저장한다() {
        var generated = List.of(new AiTaskItem("one", "first", 2),
                new AiTaskItem("two", "second", 4));
        owned();
        when(generator.generate("original", "requirement")).thenReturn(generated);
        when(persistence.saveBatch(1L, 10L, "original", generated)).thenReturn(List.of());

        service.generate(1L, 10L, new AiGenerateRequest(" original ", " requirement "));

        verify(generator).generate("original", "requirement");
        verify(persistence).saveBatch(1L, 10L, "original", generated);
        verify(persistence, never()).saveFallback(1L, 10L, "original", "requirement");
    }

    @Test
    void 첫_구조오류만_동일입력으로_한번_재시도한다() {
        var generated = List.of(new AiTaskItem("one", "detail", 1));
        owned();
        when(generator.generate("original", "requirement"))
                .thenThrow(new InvalidAiResponseException("invalid"))
                .thenReturn(generated);
        when(persistence.saveBatch(1L, 10L, "original", generated)).thenReturn(List.of());

        service.generate(1L, 10L, new AiGenerateRequest("original", "requirement"));

        verify(generator, times(2)).generate("original", "requirement");
        verify(persistence, never()).saveFallback(1L, 10L, "original", "requirement");
    }

    @Test
    void 두번_구조오류면_fallback하고_batch는_저장하지_않는다() {
        owned();
        when(generator.generate("original", "requirement"))
                .thenThrow(new InvalidAiResponseException("invalid"));
        when(persistence.saveFallback(1L, 10L, "original", "requirement")).thenReturn(List.of());

        service.generate(1L, 10L, new AiGenerateRequest("original", "requirement"));

        verify(generator, times(2)).generate("original", "requirement");
        verify(persistence, never()).saveBatch(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void transport_timeout_safety_no_result_실패는_재시도없이_fallback한다() {
        owned();
        when(generator.generate("original", "requirement"))
                .thenThrow(new AiGenerationException("timeout", new RuntimeException()));
        when(persistence.saveFallback(1L, 10L, "original", "requirement")).thenReturn(List.of());

        service.generate(1L, 10L, new AiGenerateRequest("original", "requirement"));

        verify(generator).generate("original", "requirement");
        verify(persistence).saveFallback(1L, 10L, "original", "requirement");
    }

    @Test
    void DB_batch_실패는_fallback으로_전환하지_않는다() {
        var generated = List.of(new AiTaskItem("one", "detail", 1));
        owned();
        when(generator.generate("original", "requirement")).thenReturn(generated);
        when(persistence.saveBatch(1L, 10L, "original", generated))
                .thenThrow(new DomainException(ApiCode.TASK_BATCH_SAVE_FAILED));

        assertCode(ApiCode.TASK_BATCH_SAVE_FAILED,
                () -> service.generate(1L, 10L, new AiGenerateRequest("original", "requirement")));
        verify(persistence, never()).saveFallback(1L, 10L, "original", "requirement");
    }

    @Test
    void fallback_저장실패는_정확한_오류를_그대로_반환한다() {
        owned();
        when(generator.generate("original", "requirement"))
                .thenThrow(new AiGenerationException("transport", new RuntimeException()));
        when(persistence.saveFallback(1L, 10L, "original", "requirement"))
                .thenThrow(new DomainException(ApiCode.TASK_FALLBACK_SAVE_FAILED));

        assertCode(ApiCode.TASK_FALLBACK_SAVE_FAILED,
                () -> service.generate(1L, 10L, new AiGenerateRequest("original", "requirement")));
        verify(generator).generate("original", "requirement");
    }

    @Test
    void 입력과_소유권을_AI호출전에_검증한다() {
        assertCode(ApiCode.INVALID_TASK_TITLE,
                () -> service.generate(1L, 10L, new AiGenerateRequest(" ", "description")));
        assertCode(ApiCode.INVALID_AI_DESCRIPTION,
                () -> service.generate(1L, 10L, new AiGenerateRequest("title", " ")));
        assertCode(ApiCode.INVALID_TASK_TITLE,
                () -> service.generate(1L, 10L, new AiGenerateRequest("bad\u0001", "description")));
        assertCode(ApiCode.INVALID_TASK_TITLE,
                () -> service.generate(1L, 10L, new AiGenerateRequest("x".repeat(201), "description")));
        assertCode(ApiCode.INVALID_AI_DESCRIPTION,
                () -> service.generate(1L, 10L, new AiGenerateRequest("title", "x".repeat(5001))));
        AiGenerateRequest extra = new AiGenerateRequest("title", "description");
        extra.captureUnknown("extra", true);
        assertCode(ApiCode.INVALID_REQUEST, () -> service.generate(1L, 10L, extra));

        when(projects.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());
        assertCode(ApiCode.PROJECT_NOT_FOUND,
                () -> service.generate(1L, 10L, new AiGenerateRequest("title", "description")));
        verify(generator, never()).generate(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    private void owned() {
        when(projects.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(org.mockito.Mockito.mock(Project.class)));
    }

    private void assertCode(ApiCode code, Runnable call) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(DomainException.class,
                exception -> assertThat(exception.getCode()).isEqualTo(code));
    }
}
