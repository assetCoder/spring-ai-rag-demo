package com.ragdemo.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AppConfig {

    private static final String BASE_URL = "https://api.deepseek.com";

    @Value("${DEEPSEEK_API_KEY:#{null}}")
    private String apiKey;

    @Bean
    public ChatLanguageModel chatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(BASE_URL)
                .modelName("deepseek-chat")
                .temperature(0.7)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .baseUrl(BASE_URL)
                .modelName("deepseek-embedding")
                .timeout(Duration.ofSeconds(30))
                .build();
    }
}
