package com.dewevrei.aikanban.aitask;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.stereotype.Component;

@Component
public final class GoogleAiTaskGateway implements AiTaskGenerator {
    static final String OUTPUT_SCHEMA = """
            {"type":"object","required":["tasks"],"properties":{"tasks":{"type":"array","minItems":1,"items":{"type":"object","required":["title","description","priority"],"properties":{"title":{"type":"string"},"description":{"type":"string"},"priority":{"type":"integer","minimum":1,"maximum":5}}}}}}
            """;
    private static final String SYSTEM_PROMPT = """
            입력 요구사항을 실행 가능한 평면적이고 서로 독립적인 작업들로 분해하세요.
            모든 작업은 명확한 title, 실행 내용을 설명하는 description, 1(가장 높음)부터 5까지의 정수 priority를 가져야 합니다.
            부모 작업이나 중첩 구조를 만들지 말고, 지정된 JSON 스키마 외의 설명, Markdown, 코드 블록을 반환하지 마세요.
            """;

    private final ChatClient chatClient;
    private final AiTaskJsonParser parser = new AiTaskJsonParser();

    public GoogleAiTaskGateway(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public List<AiTaskItem> generate(String title, String description) {
        try {
            String content = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user("title: " + title + "\ndescription: " + description)
                    .options(options())
                    .call()
                    .content();
            if (content == null || content.isBlank()) throw new AiGenerationException("AI returned no result", null);
            return parser.parse(content, title);
        } catch (InvalidAiResponseException | AiGenerationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AiGenerationException("AI generation failed", exception);
        }
    }

    static GoogleGenAiChatOptions.Builder options() {
        GoogleGenAiChatOptions.Builder builder = GoogleGenAiChatOptions.builder();
        builder.model(GoogleGenAiChatModel.ChatModel.GEMINI_2_5_FLASH)
                .temperature(0.2)
                .topP(0.9)
                .maxOutputTokens(8192)
                .outputSchema(OUTPUT_SCHEMA);
        return builder;
    }
}
