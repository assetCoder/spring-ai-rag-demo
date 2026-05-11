package com.ragdemo.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档导入服务 - 启动时自动加载示例文档
 */
@Service
public class DocumentImportService implements CommandLineRunner {

    private final VectorStore vectorStore;

    @Value("${rag.docs-dir:./docs}")
    private String docsDir;

    public DocumentImportService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) throws Exception {
        // 创建示例文档目录
        var dir = Path.of(docsDir);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
            createSampleDocuments(dir);
        }

        // 加载文档
        loadDocuments(dir);
    }

    private void createSampleDocuments(Path dir) throws IOException {
        // 示例文档1：Spring AI 介绍
        Files.writeString(dir.resolve("spring-ai-intro.txt"), """
            Spring AI 是一个用于集成人工智能功能的Spring框架模块。
            它提供了与各种AI提供商（如OpenAI、Anthropic、DeepSeek）的集成。
            主要功能包括：AI对话、文本生成、文档向量化、RAG检索增强生成。
            RAG（Retrieval-Augmented Generation）是一种将检索与生成相结合的AI技术。
            它通过从知识库中检索相关信息，增强AI模型的回答准确性。
            """);

        // 示例文档2：Spring Boot 快速开始
        Files.writeString(dir.resolve("spring-boot-quickstart.txt"), """
            Spring Boot 是一个用于创建独立、生产级Spring应用的框架。
            特点：自动配置、嵌入式服务器、生产就绪功能。
            使用spring-boot-starter-parent作为父POM。
            可以使用@SpringBootApplication注解启动应用。
            配置文件支持application.yml和application.properties。
            """);

        System.out.println("已创建示例文档");
    }

    private void loadDocuments(Path dir) throws IOException {
        try (var files = Files.list(dir)) {
            var docs = new ArrayList<Document>();
            files.filter(f -> f.toString().endsWith(".txt") || f.toString().endsWith(".md"))
                .forEach(f -> {
                    try {
                        var content = Files.readString(f);
                        docs.add(new Document(f.getFileName().toString(), content));
                        System.out.println("加载文档: " + f.getFileName());
                    } catch (IOException e) {
                        System.err.println("加载失败: " + f.getFileName());
                    }
                });

            if (!docs.isEmpty()) {
                vectorStore.add(docs);
                System.out.println("向量化完成: " + docs.size() + " 个文档");
            }
        }
    }
}
