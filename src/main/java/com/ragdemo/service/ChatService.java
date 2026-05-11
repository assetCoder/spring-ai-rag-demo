package com.ragdemo.service;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final List<Message> history = new ArrayList<>();

    public ChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String chat(String message) {
        history.add(new UserMessage(message));

        var prompt = new Prompt(history);
        var response = chatClient.call(prompt);

        var reply = response.getResult().getOutput().getContent();
        history.add(new AssistantMessage(reply));

        if (history.size() > 20) {
            history.remove(0);
            history.remove(0);
        }

        return reply;
    }

    public void clearHistory() {
        history.clear();
    }
}
