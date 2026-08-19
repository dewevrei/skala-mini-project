package com.dewevrei.aikanban.aitask;

import java.io.IOException;

import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiConnectionProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.genai.Client;
import com.google.genai.types.HttpOptions;

@Configuration
@EnableConfigurationProperties(GeminiClientProperties.class)
class GoogleGenAiClientConfig {
    @Bean
    Client googleGenAiClient(GoogleGenAiConnectionProperties properties,
            GeminiClientProperties geminiClientProperties) throws IOException {
        Client.Builder builder = Client.builder()
                .httpOptions(HttpOptions.builder().timeout(geminiClientProperties.timeoutMillis()).build());

        boolean hasApiKey = StringUtils.hasText(properties.getApiKey());
        boolean hasProject = StringUtils.hasText(properties.getProjectId());
        boolean hasLocation = StringUtils.hasText(properties.getLocation());
        boolean hasVertexConfig = hasProject && hasLocation;

        if (properties.isVertexAi()) {
            configureVertexAi(builder, properties);
        } else if (hasApiKey) {
            builder.apiKey(properties.getApiKey());
        } else if (hasVertexConfig) {
            configureVertexAi(builder, properties);
        } else {
            throw new IllegalStateException("Incomplete Google GenAI configuration: Provide 'api-key' for Gemini API "
                    + "or 'project-id' and 'location' for Vertex AI.");
        }

        return builder.build();
    }

    private void configureVertexAi(Client.Builder builder, GoogleGenAiConnectionProperties properties)
            throws IOException {
        Assert.hasText(properties.getProjectId(), "Google GenAI project-id must be set for Vertex AI mode!");
        Assert.hasText(properties.getLocation(), "Google GenAI location must be set for Vertex AI mode!");

        builder.project(properties.getProjectId()).location(properties.getLocation()).vertexAI(true);

        if (properties.getCredentialsUri() != null) {
            try (var inputStream = properties.getCredentialsUri().getInputStream()) {
                builder.credentials(GoogleCredentials.fromStream(inputStream));
            }
        }
    }
}
