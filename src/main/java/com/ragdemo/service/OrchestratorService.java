package com.ragdemo.service;

import com.ragdemo.agent.*;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

/**
 * Agent编排服务 - 统一入口，自动路由到对应Agent
 */
@Service
public class OrchestratorService {

    private final AgentRegistry registry;
    private OrchestratorAgent orchestrator;

    public OrchestratorService(AgentRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    public void init() {
        this.orchestrator = OrchestratorAgent.create(registry.chatModel());
    }

    public String process(String message) {
        // 1. 路由判断
        String intent;
        try {
            intent = orchestrator.route(message);
        } catch (Exception e) {
            intent = "对话";
        }

        // 2. 派发
        return switch (intent) {
            case "客服" -> registry.customerServiceAgent().chat(message);
            case "分析" -> registry.analysisAgent().analyze(message);
            case "搜索" -> registry.searchAgent().search(message);
            default -> registry.chatAgent().chat(message);
        };
    }

    /**
     * 直接使用搜索Agent查询知识库（RAG专用入口）
     */
    public String searchKnowledge(String query) {
        return registry.searchAgent().search(query);
    }
}
