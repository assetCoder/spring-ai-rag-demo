package com.ragdemo.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.CosineSimilarity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于SQLite + 内存的轻量向量存储
 * 文档向量持久化在SQLite，检索时加载到内存计算相似度
 */
@Service
public class SqliteVectorStore implements EmbeddingStore<TextSegment> {

    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbc;

    public SqliteVectorStore(EmbeddingModel embeddingModel, JdbcTemplate jdbc) {
        this.embeddingModel = embeddingModel;
        this.jdbc = jdbc;
        initTable();
    }

    private void initTable() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS vector_store (
                id TEXT PRIMARY KEY,
                content TEXT NOT NULL,
                embedding BLOB,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """);
    }

    @Override
    public String add(Embedding embedding) {
        var id = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO vector_store (id, content, embedding) VALUES (?, '', ?)",
                id, serialize(embedding));
        return id;
    }

    @Override
    public void add(String id, Embedding embedding) {
        jdbc.update("INSERT OR REPLACE INTO vector_store (id, content, embedding) VALUES (?, '', ?)",
                id, serialize(embedding));
    }

    @Override
    public void add(String id, Embedding embedding, TextSegment textSegment) {
        var content = textSegment != null ? textSegment.text() : "";
        jdbc.update("INSERT OR REPLACE INTO vector_store (id, content, embedding) VALUES (?, ?, ?)",
                id, content, serialize(embedding));
    }

    @Override
    public List<EmbeddingMatch<TextSegment>> findRelevant(int maxResults, Embedding reference) {
        var rows = jdbc.queryForList("SELECT id, content, embedding FROM vector_store");
        if (rows.isEmpty()) return List.of();

        var scored = new ArrayList<ScoredMatch>();
        for (var row : rows) {
            var emb = deserialize((byte[]) row.get("embedding"));
            var score = CosineSimilarity.between(reference, emb);
            var segment = new TextSegment((String) row.get("content"));
            scored.add(new ScoredMatch((String) row.get("id"), score, segment, emb));
        }

        scored.sort((a, b) -> Double.compare(b.score, a.score));
        return scored.stream()
                .limit(maxResults)
                .map(s -> new EmbeddingMatch<>(s.score, s.id, s.embedding, s.segment))
                .collect(Collectors.toList());
    }

    // --- 文档存储辅助方法 ---

    public void storeDocument(String content) {
        var embedding = embeddingModel.embed(content).content();
        var id = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO vector_store (id, content, embedding) VALUES (?, ?, ?)",
                id, content, serialize(embedding));
    }

    public List<String> searchSimilar(String query, int topK) {
        var queryEmb = embeddingModel.embed(query).content();
        var matches = findRelevant(topK, queryEmb);
        return matches.stream()
                .map(m -> m.embedded().text())
                .collect(Collectors.toList());
    }

    // --- 序列化辅助 ---

    private byte[] serialize(Embedding embedding) {
        var vec = embedding.vector();
        var bb = ByteBuffer.allocate(vec.length() * 4);
        for (int i = 0; i < vec.length(); i++) {
            bb.putFloat(vec.getFloat(i));
        }
        return bb.array();
    }

    private Embedding deserialize(byte[] bytes) {
        var bb = ByteBuffer.wrap(bytes);
        var floats = new float[bb.capacity() / 4];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = bb.getFloat();
        }
        return new Embedding(floats);
    }

    private record ScoredMatch(String id, double score, TextSegment segment, Embedding embedding) {}
}
