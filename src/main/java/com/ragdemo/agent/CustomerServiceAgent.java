package com.ragdemo.agent;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;

/**
 * 客服Agent - 处理常规问答、订单查询、FAQ
 */
public interface CustomerServiceAgent {

    @SystemMessage("""
        你是友好的客服助手。你的职责：
        1. 回答用户关于产品或服务的问题
        2. 帮助查询订单和账户信息
        3. 如果问题超出范围，礼貌说明
        
        请用简洁、友好的语气回复。
        """)
    String chat(String userMessage);

    static CustomerServiceAgent create(ChatLanguageModel model, Object tools) {
        return AiServices.builder(CustomerServiceAgent.class)
                .chatLanguageModel(model)
                .tools(tools)
                .build();
    }
}
