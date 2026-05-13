package com.ragdemo.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文档管理服务
 * - 上传文档→分块→向量嵌入→存入向量库
 * - 启动时自动加载 docs/ 目录下默认文档
 */
@Service
public class DocumentService {

    private final VectorStore vectorStore;

    public DocumentService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 上传文档内容，自动分块后存入向量库
     */
    public void uploadDocument(String content) {
        var chunks = Chunker.chunk(content);
        vectorStore.addDocuments(chunks);
    }

    /**
     * 启动时加载 docs/ 目录下的默认文档
     */
    public void loadDefaultDocs() throws IOException {
        var dir = Path.of("./docs");
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);

            // 写入默认文档
            Files.writeString(dir.resolve("spring-ai-intro.txt"), """
                Spring AI 是一个用于集成人工智能功能的Spring框架模块。
                它提供了与各种AI提供商（如OpenAI、Anthropic、DeepSeek）的集成。
                主要功能包括：AI对话、文本生成、文档向量化、RAG检索增强生成。
                RAG（Retrieval-Augmented Generation）是一种将检索与生成相结合的AI技术。
                它能显著提高大语言模型回答的准确性和时效性。
                """);

            Files.writeString(dir.resolve("spring-boot-quickstart.txt"), """
                Spring Boot 是一个用于创建独立、生产级Spring应用的框架。
                特点：自动配置、嵌入式服务器、生产就绪功能。
                使用spring-boot-starter-parent作为父POM。
                可以使用@SpringBootApplication注解启动应用。
                配置文件支持application.yml和application.properties。
                Spring Boot 3.x 要求 Java 17 以上版本。
                """);
        }

        // 加载所有文档到向量库
        try (var files = Files.list(dir)) {
            files.filter(f -> f.toString().endsWith(".txt") || f.toString().endsWith(".md"))
                    .forEach(f -> {
                        try {
                            var content = Files.readString(f);
                            uploadDocument(content);
                            System.out.println("已加载文档: " + f.getFileName());
                        } catch (IOException e) {
                            System.err.println("加载失败: " + f.getFileName() + " - " + e.getMessage());
                        }
                    });
        }
    }
}
