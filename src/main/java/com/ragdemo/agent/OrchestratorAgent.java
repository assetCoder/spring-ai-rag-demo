package com.ragdemo.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 编排Agent（Orchestrator）- 路由分发
 */
public interface OrchestratorAgent {

    @SystemMessage("""
        你是任务调度专家。你的职责：
        根据用户的问题，判断应该交给哪个Agent处理：
        
        - "客服"：用户咨询、问答、帮助类问题
        - "分析"：数据分析、报告、统计类问题
        - "搜索"：查找信息、检索资料类问题
        - "对话"：日常聊天、不需要特殊处理的问题
        
        只返回Agent名称，不要返回其他内容。
        """)
    String route(@UserMessage String userMessage);

    static OrchestratorAgent create(dev.langchain4j.model.chat.ChatLanguageModel model) {
        return dev.langchain4j.service.AiServices.builder(OrchestratorAgent.class)
                .chatLanguageModel(model)
                .build();
    }
}
