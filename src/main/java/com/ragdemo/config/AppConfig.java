package com.ragdemo.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
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
                .timeout(Duration.ofSeconds(120))
                .maxRetries(2)
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        // DeepSeek 已不再支持 embedding API，使用本地嵌入模型
        log.info("Using local AllMiniLmL6V2 embedding model (no API key needed)");
        return new dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel();
    }
}
