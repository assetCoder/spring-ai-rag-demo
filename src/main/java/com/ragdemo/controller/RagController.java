package com.ragdemo.controller;

import com.ragdemo.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final DocumentService documentService;

    public RagController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请选择文件"));
        }
        try {
            // 读取文件内容
            var content = new String(file.getBytes());

            // 分行存储，每行作为一个文档块，便于检索
            documentService.uploadDocument(content);

            return ResponseEntity.ok(Map.of(
                "message", "文档上传成功: " + file.getOriginalFilename(),
                "size", content.length() + " 字符"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
