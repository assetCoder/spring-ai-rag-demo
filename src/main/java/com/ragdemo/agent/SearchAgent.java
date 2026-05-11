package com.ragdemo.agent;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 搜索Agent - 知识库检索、信息查找
 */
public interface SearchAgent {

    @SystemMessage("""
        你是信息检索专家。你的职责：
        1. 从知识库中查找相关信息
        2. 总结和整合检索结果
        3. 对不确定的信息要标注来源
        
        注意：
        - 使用 searchDocs 工具检索知识库
        - 如果知识库中没有，如实告知
        - 结果要简洁有条理
        """)
    String search(@UserMessage String query);

    static SearchAgent create(ChatLanguageModel model, Object tools) {
        return AiServices.builder(SearchAgent.class)
                .chatLanguageModel(model)
                .tools(tools)
                .build();
    }
}
