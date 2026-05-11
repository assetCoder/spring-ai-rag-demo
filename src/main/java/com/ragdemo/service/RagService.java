package com.ragdemo.service;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public String ask(String question) {
        var similarDocs = vectorStore.similaritySearch(
            SearchRequest.query(question).withTopK(3));

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
        var dir = Path.of(docsDir);
        if (!Files.exists(dir)) Files.createDirectories(dir);

        var path = dir.resolve(file.getOriginalFilename());
        Files.write(path, file.getBytes());

        var doc = new Document(file.getOriginalFilename(), new String(file.getBytes()));
        vectorStore.add(java.util.List.of(doc));
    }
}
