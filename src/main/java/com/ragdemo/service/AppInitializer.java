package com.ragdemo.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

/**
 * 启动时初始化
 */
@Service
public class AppInitializer implements CommandLineRunner {

    private final DocumentService documentService;

    public AppInitializer(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Override
    public void run(String... args) throws Exception {
        documentService.loadDefaultDocs();
    }
}
