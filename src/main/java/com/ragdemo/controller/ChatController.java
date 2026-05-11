package com.ragdemo.controller;

import com.ragdemo.service.OrchestratorService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 统一聊天入口
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private final OrchestratorService orchestrator;

    public ChatController(OrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, String> request) {
        var message = request.get("message");
        if (message == null || message.isBlank()) {
            return Map.of("error", "请输入消息");
        }

        try {
            var reply = orchestrator.process(message);
            return Map.of("reply", reply, "agent", "auto");
        } catch (Exception e) {
            return Map.of("reply", "处理出错: " + e.getMessage(), "agent", "error");
        }
    }

    @PostMapping("/rag/ask")
    public Map<String, Object> ask(@RequestBody Map<String, String> request) {
        var question = request.get("question");
        if (question == null || question.isBlank()) {
            return Map.of("error", "请输入问题");
        }

        try {
            var reply = orchestrator.searchKnowledge(question);
            return Map.of("answer", reply);
        } catch (Exception e) {
            return Map.of("error", "查询出错: " + e.getMessage());
        }
    }
}
