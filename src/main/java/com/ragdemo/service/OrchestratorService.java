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

    /**
     * 入口方法：自动路由到合适的Agent
     */
    public String process(String message) {
        // 1. 路由判断
        String intent = orchestrator.route(message);

        // 2. 派发到对应Agent
        return switch (intent) {
            case "客服" -> registry.customerServiceAgent().chat(message);
            case "分析" -> registry.analysisAgent().analyze(message);
            case "搜索" -> registry.searchAgent().search(message);
            default -> registry.chatAgent().chat(message);
        };
    }
}
