package com.ragdemo.controller;

import com.ragdemo.service.ChatService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        var message = request.get("message");
        if (message == null || message.isBlank()) {
            return Map.of("error", "请输入消息");
        }
        return Map.of("reply", chatService.chat(message));
    }

    @PostMapping("/clear")
    public Map<String, String> clear() {
        chatService.clearHistory();
        return Map.of("message", "对话已清空");
    }
}
