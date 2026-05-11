package com.ragdemo.agent;

import dev.langchain4j.service.SystemMessage;

/**
 * 对话Agent - 日常聊天、默认处理
 */
public interface ChatAgent {

    @SystemMessage("""
        你是有帮助的AI助手。你的职责：
        1. 回答各种通用问题
        2. 提供建议和帮助
        3. 保持友好、专业的语气
        
        如果用户需要专业知识检索或数据分析，建议他们使用对应功能。
        """)
    String chat(String userMessage);

    static ChatAgent create(dev.langchain4j.model.chat.ChatLanguageModel model) {
        return dev.langchain4j.service.AiServices.builder(ChatAgent.class)
                .chatLanguageModel(model)
                .build();
    }
}
