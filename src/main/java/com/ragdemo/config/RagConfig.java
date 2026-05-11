package com.ragdemo.config;

import com.ragdemo.service.SqliteVectorStore;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RagConfig {

    @Bean
    @Primary
    public SqliteVectorStore sqliteVectorStore(EmbeddingClient embeddingClient,
                                                org.springframework.jdbc.core.JdbcTemplate jdbc) {
        return new SqliteVectorStore(embeddingClient, jdbc);
    }
}
