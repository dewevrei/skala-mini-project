package com.dewevrei.aikanban.aitask;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;

import com.google.genai.types.Schema;

class GoogleAiTaskGatewayOptionsTest {
    @Test
    void 공식_GoogleGenAI_options에_확정값만_설정한다() {
        var options = GoogleAiTaskGateway.options().build();

        assertThat(options.getModel()).isEqualTo(GoogleGenAiChatModel.ChatModel.GEMINI_3_5_FLASH.value);
        assertThat(options.getTemperature()).isEqualTo(0.2);
        assertThat(options.getTopP()).isEqualTo(0.9);
        assertThat(options.getMaxOutputTokens()).isEqualTo(8192);
        assertThat(options.getResponseMimeType()).isEqualTo("application/json");
        assertThat(options.getOutputSchema()).isEqualTo(GoogleAiTaskGateway.OUTPUT_SCHEMA);
        assertThat(options.getTopK()).isNull();
        assertThat(options.getSafetySettings()).isNull();
        assertThat(options.getThinkingBudget()).isNull();
        assertThat(options.getThinkingLevel()).isNull();
        assertThat(Schema.fromJson(options.getOutputSchema()).required()).isPresent();
    }
}
