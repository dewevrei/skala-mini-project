package com.dewevrei.aikanban.aitask;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.gemini")
record GeminiClientProperties(Duration timeout) {
    GeminiClientProperties {
        if (timeout == null) {
            timeout = Duration.ofSeconds(30);
        }
    }

    int timeoutMillis() {
        return Math.toIntExact(timeout.toMillis());
    }
}
