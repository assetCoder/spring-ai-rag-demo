package com.ragdemo.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于 InMemoryEmbeddingStore 的向量知识库
 * - 写入：文本→DeepSeek Embedding→存入内存→序列化JSON到文件持久化
 * - 读取：query→Embedding→相似度搜索→返回语义匹配结果
 * - 启动：从JSON文件反序列化恢复
 */
@Service
public class VectorStore {

    private static final Logger log = LoggerFactory.getLogger(VectorStore.class);

    private final EmbeddingModel embeddingModel;
    private final Path storePath;

    private InMemoryEmbeddingStore<TextSegment> store;

    public VectorStore(EmbeddingModel embeddingModel,
                       @Value("${rag.store-path:./data/vector-store.json}") String storePath) {
        this.embeddingModel = embeddingModel;
        this.storePath = Paths.get(storePath);
    }

    @PostConstruct
    public void init() {
        if (Files.exists(storePath)) {
            try {
                store = InMemoryEmbeddingStore.fromJson(Files.readString(storePath));
                log.info("从 {} 恢复向量库", storePath);
            } catch (IOException e) {
                log.warn("恢复向量库失败: {}", e.getMessage());
                store = new InMemoryEmbeddingStore<>();
            }
        } else {
            store = new InMemoryEmbeddingStore<>();
            log.info("新建空向量库");
        }
    }

    @PreDestroy
    public void persist() {
        try {
            Files.createDirectories(storePath.getParent());
            Files.writeString(storePath, store.serializeToJson());
            log.info("向量库已持久化到 {}", storePath);
        } catch (IOException e) {
            log.error("持久化向量库失败: {}", e.getMessage());
        }
    }

    /**
     * 添加一条文档到知识库
     */
    public void addDocument(String content) {
        TextSegment segment = TextSegment.from(content);
        Embedding embedding = embeddingModel.embed(content).content();
        store.add(embedding, segment);
        log.info("已添加文档: {} 字符", content.length());
    }

    /**
     * 批量添加文档块
     */
    public void addDocuments(List<String> chunks) {
        for (String chunk : chunks) {
            addDocument(chunk);
        }
    }

    /**
     * 语义搜索，返回匹配内容列表
     */
    public List<String> searchSimilar(String query, int topK) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        List<EmbeddingMatch<TextSegment>> matches = store.findRelevant(queryEmbedding, topK);

        return matches.stream()
                .map(m -> m.embedded().text())
                .collect(Collectors.toList());
    }

    /**
     * 语义搜索附带相似度分数
     */
    public List<ScoredResult> searchWithScore(String query, int topK) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        List<EmbeddingMatch<TextSegment>> matches = store.findRelevant(queryEmbedding, topK);

        return matches.stream()
                .map(m -> new ScoredResult(m.embedded().text(), m.score()))
                .collect(Collectors.toList());
    }

    public record ScoredResult(String content, double score) {}
}
