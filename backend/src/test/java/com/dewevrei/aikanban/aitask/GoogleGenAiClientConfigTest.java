package com.dewevrei.aikanban.aitask;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiConnectionProperties;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.genai.ApiClient;

class GoogleGenAiClientConfigTest {
    @Test
    void Gemini_Client는_호출당_30초_timeout을_사용한다() throws Exception {
        GoogleGenAiConnectionProperties properties = new GoogleGenAiConnectionProperties();
        properties.setApiKey("test-api-key");
        GeminiClientProperties geminiClientProperties = new GeminiClientProperties(Duration.ofSeconds(30));

        var client = new GoogleGenAiClientConfig().googleGenAiClient(properties, geminiClientProperties);
        var apiClient = (ApiClient) ReflectionTestUtils.getField(client, "apiClient");

        assertThat(apiClient).isNotNull();
        assertThat(apiClient.httpOptions().timeout()).contains(30_000);
        assertThat(apiClient.httpClient().callTimeoutMillis()).isEqualTo(30_000);
    }
}
