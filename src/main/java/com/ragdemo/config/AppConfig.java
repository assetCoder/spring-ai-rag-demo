package com.ragdemo.config;

import com.ragdemo.service.SqliteVectorStore;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;

@Configuration
public class AppConfig {

    private static final String BASE_URL = "https://api.deepseek.com";

    @Bean
    public ChatLanguageModel chatModel() {
        return OpenAiChatModel.builder()
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .baseUrl(BASE_URL)
                .modelName("deepseek-chat")
                .temperature(0.7)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("DEEPSEEK_API_KEY"))
                .baseUrl(BASE_URL)
                .modelName("deepseek-embedding")
                .build();
    }

    @Bean
    public SqliteVectorStore vectorStore(EmbeddingModel embeddingModel, JdbcTemplate jdbc) {
        return new SqliteVectorStore(embeddingModel, jdbc);
    }
}
