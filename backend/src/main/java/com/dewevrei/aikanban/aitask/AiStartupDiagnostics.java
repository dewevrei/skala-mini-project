package com.dewevrei.aikanban.aitask;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.google.genai.Client;

@Component
class AiStartupDiagnostics implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AiStartupDiagnostics.class);

    private final Environment environment;
    private final ApplicationContext applicationContext;

    AiStartupDiagnostics(Environment environment, ApplicationContext applicationContext) {
        this.environment = environment;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        String apiKey = environment.getProperty("spring.ai.google.genai.api-key");
        log.info("AI config: spring.ai.google.genai.api-key={}", mask(apiKey));

        String[] clientBeans = applicationContext.getBeanNamesForType(Client.class);
        String[] chatClientBuilderBeans = applicationContext.getBeanNamesForType(ChatClient.Builder.class);

        log.info("AI beans: Client count={}, names={}", clientBeans.length, Arrays.toString(clientBeans));
        log.info("AI beans: ChatClient.Builder count={}, names={}",
                chatClientBuilderBeans.length, Arrays.toString(chatClientBuilderBeans));

        if (chatClientBuilderBeans.length > 0) {
            ChatClient.Builder builder = applicationContext.getBean(ChatClient.Builder.class);
            log.info("AI beans: ChatClient.Builder type={}", builder.getClass().getName());
        }
    }

    private static String mask(String value) {
        if (value == null) {
            return "null";
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "blank(len=0)";
        }

        String prefix = trimmed.substring(0, Math.min(4, trimmed.length()));
        String suffix = trimmed.substring(Math.max(0, trimmed.length() - 4));
        return prefix + "..." + suffix + "(len=" + trimmed.length() + ")";
    }
}
