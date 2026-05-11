package com.ragdemo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文档管理服务
 */
@Service
public class DocumentService {

    private final SqliteVectorStore vectorStore;

    public DocumentService(SqliteVectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void uploadDocument(String content) {
        vectorStore.storeDocument(content);
    }

    public void loadDefaultDocs() throws IOException {
        var dir = Path.of("./docs");
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);

            Files.writeString(dir.resolve("spring-ai-intro.txt"), """
                Spring AI 是一个用于集成人工智能功能的Spring框架模块。
                它提供了与各种AI提供商（如OpenAI、Anthropic、DeepSeek）的集成。
                主要功能包括：AI对话、文本生成、文档向量化、RAG检索增强生成。
                RAG（Retrieval-Augmented Generation）是一种将检索与生成相结合的AI技术。
                """);

            Files.writeString(dir.resolve("spring-boot-quickstart.txt"), """
                Spring Boot 是一个用于创建独立、生产级Spring应用的框架。
                特点：自动配置、嵌入式服务器、生产就绪功能。
                使用spring-boot-starter-parent作为父POM。
                可以使用@SpringBootApplication注解启动应用。
                配置文件支持application.yml和application.properties。
                """);
        }

        try (var files = Files.list(dir)) {
            files.filter(f -> f.toString().endsWith(".txt"))
                    .forEach(f -> {
                        try {
                            vectorStore.storeDocument(Files.readString(f));
                        } catch (IOException e) {
                            System.err.println("加载失败: " + f.getFileName());
                        }
                    });
        }
    }
}
