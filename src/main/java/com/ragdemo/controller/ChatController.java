package com.ragdemo.controller;

import com.ragdemo.service.OrchestratorService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 统一聊天入口
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final OrchestratorService orchestrator;

    public ChatController(OrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping
    public Map<String, Object> chat(@RequestBody Map<String, String> request) {
        var message = request.get("message");
        if (message == null || message.isBlank()) {
            return Map.of("error", "请输入消息");
        }

        var reply = orchestrator.process(message);

        return Map.of(
                "reply", reply,
                "agent", detectAgentUsed(message)
        );
    }

    private String detectAgentUsed(String message) {
        // 简化判断，实际由Orchestrator路由
        return "auto";
    }
}
