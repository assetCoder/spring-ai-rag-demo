package com.ragdemo.service;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    @Value("${rag.docs-dir:./docs}")
    private String docsDir;

    public RagService(ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void init() {
        var dir = new File(docsDir);
        if (dir.exists()) {
            loadDocuments(dir);
        }
    }

    public String ask(String question) {
        var similarDocs = vectorStore.similaritySearch(
            org.springframework.ai.vectorstore.SearchRequest.query(question)
                .withTopK(3));

        if (similarDocs.isEmpty()) {
            return chatClient.call(question);
        }

        var context = similarDocs.stream()
            .map(Document::getContent)
            .collect(Collectors.joining("\n---\n"));

        var prompt = """
            基于以下参考信息回答问题。如果参考信息不足以回答，请如实说明。

            参考信息：
            %s

            问题：%s
            """.formatted(context, question);

        return chatClient.call(prompt);
    }

    public void uploadDocument(MultipartFile file) throws IOException {
        var dir = new File(docsDir);
        if (!dir.exists()) dir.mkdirs();

        var path = Path.of(docsDir, file.getOriginalFilename());
        Files.write(path, file.getBytes());

        var doc = new Document(file.getOriginalFilename(), new String(file.getBytes()));
        vectorStore.add(List.of(doc));
    }

    private void loadDocuments(File dir) {
        var files = dir.listFiles((d, name) -> name.endsWith(".txt") || name.endsWith(".md"));
        if (files == null) return;

        for (var file : files) {
            try {
                var content = Files.readString(file.toPath());
                var doc = new Document(file.getName(), content);
                vectorStore.add(List.of(doc));
                System.out.println("已加载文档: " + file.getName());
            } catch (IOException e) {
                System.err.println("加载失败: " + file.getName());
            }
        }
    }
}
