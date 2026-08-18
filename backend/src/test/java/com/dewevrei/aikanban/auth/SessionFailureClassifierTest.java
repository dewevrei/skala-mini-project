package com.dewevrei.aikanban.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class SessionFailureClassifierTest {

    private final SessionFailureClassifier classifier = new SessionFailureClassifier();

    @Test
    void 중첩된_Redis_연결_장애를_세션_저장소_장애로_판정한다() {
        RuntimeException exception = new RuntimeException("wrapper",
                new DataAccessResourceFailureException("redis unavailable"));

        assertThat(classifier.isSessionStoreFailure(exception)).isTrue();
    }

    @Test
    void 일반_애플리케이션_예외는_세션_장애로_오인하지_않는다() {
        assertThat(classifier.isSessionStoreFailure(new IllegalStateException("business error"))).isFalse();
    }
}
