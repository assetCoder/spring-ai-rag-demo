package com.ragdemo.service;

import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.document.Document;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于SQLite的轻量向量存储
 * 文档 → 向量化 → SQLite持久化 → 余弦相似度检索
 */
@Service
public class SqliteVectorStore implements VectorStore {

    private final EmbeddingClient embeddingClient;
    private final JdbcTemplate jdbc;

    public SqliteVectorStore(EmbeddingClient embeddingClient, JdbcTemplate jdbc) {
        this.embeddingClient = embeddingClient;
        this.jdbc = jdbc;
        initTable();
    }

    private void initTable() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS vector_store (
                id TEXT PRIMARY KEY,
                content TEXT NOT NULL,
                embedding BLOB,
                metadata TEXT DEFAULT '{}',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """);
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_vs_created ON vector_store(created_at)");
    }

    @Override
    public void add(List<Document> documents) {
        for (var doc : documents) {
            var embedding = embeddingClient.embed(doc);
            var id = doc.getId() != null ? doc.getId() : UUID.randomUUID().toString();
            jdbc.update(
                "INSERT OR REPLACE INTO vector_store (id, content, embedding, metadata) VALUES (?, ?, ?, ?)",
                id, doc.getContent(), serializeEmbedding(embedding), doc.getMetadata().toString());
        }
    }

    @Override
    public Optional<Boolean> delete(List<String> ids) {
        for (var id : ids) {
            jdbc.update("DELETE FROM vector_store WHERE id = ?", id);
        }
        return Optional.of(true);
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        var query = request.getQuery();
        var topK = request.getTopK();
        var queryEmbedding = embeddingClient.embed(query);

        var rows = jdbc.queryForList("SELECT id, content, embedding, metadata FROM vector_store");
        if (rows.isEmpty()) return List.of();

        var scored = new ArrayList<ScoredDoc>();
        for (var row : rows) {
            var docEmbedding = deserializeEmbedding((byte[]) row.get("embedding"));
            var score = cosineSimilarity(queryEmbedding, docEmbedding);
            scored.add(new ScoredDoc(
                (String) row.get("id"),
                (String) row.get("content"),
                (String) row.get("metadata"),
                score
            ));
        }

        scored.sort((a, b) -> Float.compare(b.score, a.score));
        return scored.stream()
            .limit(topK)
            .map(s -> {
                var doc = new Document(s.id, s.content);
                return doc;
            })
            .collect(Collectors.toList());
    }

    private float cosineSimilarity(List<Double> a, List<Double> b) {
        if (a.size() != b.size()) return 0;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.size(); i++) {
            dot += a.get(i) * b.get(i);
            na += a.get(i) * a.get(i);
            nb += b.get(i) * b.get(i);
        }
        return (float) (dot / (Math.sqrt(na) * Math.sqrt(nb) + 1e-10));
    }

    private byte[] serializeEmbedding(List<Double> embedding) {
        var bb = java.nio.ByteBuffer.allocate(embedding.size() * 8);
        for (var v : embedding) bb.putDouble(v);
        return bb.array();
    }

    private List<Double> deserializeEmbedding(byte[] bytes) {
        var bb = java.nio.ByteBuffer.wrap(bytes);
        var list = new ArrayList<Double>();
        while (bb.hasRemaining()) list.add(bb.getDouble());
        return list;
    }

    private record ScoredDoc(String id, String content, String metadata, float score) {}
}
