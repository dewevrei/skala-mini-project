package com.dewevrei.aikanban.aitask;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;

@Configuration(proxyBeanMethods = false)
class AiClientConfiguration {
    @Bean
    Client googleGenAiClient(@Value("${spring.ai.google.genai.api-key}") String apiKey) {
        return Client.builder()
                .apiKey(apiKey)
                .httpOptions(HttpOptions.builder()
                        .timeout(30_000)
                        .retryOptions(HttpRetryOptions.builder().attempts(1))
                        .build())
                .build();
    }
}
