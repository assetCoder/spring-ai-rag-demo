package com.ragdemo.agent;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 搜索Agent - 知识库检索、信息查找
 * 会调用 searchDocs 工具从SQLite知识库检索
 */
public interface SearchAgent {

    @SystemMessage("""
        你是信息检索专家。你的职责：
        1. 使用 searchDocs 工具从知识库中查找相关信息
        2. 根据检索结果总结回答用户问题
        3. 如果知识库中没有相关信息，告诉用户未找到
        
        重要：必须优先调用 searchDocs 工具检索知识库！
        """)
    String search(@UserMessage String query);

    static SearchAgent create(ChatLanguageModel model, Object tools) {
        return AiServices.builder(SearchAgent.class)
                .chatLanguageModel(model)
                .tools(tools)
                .build();
    }
}
