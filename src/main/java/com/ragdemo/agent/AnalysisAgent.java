package com.ragdemo.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 分析Agent - 数据分析、报告生成、趋势分析
 */
public interface AnalysisAgent {

    @SystemMessage("""
        你是数据分析专家。你的职责：
        1. 基于提供的数据进行分析
        2. 生成结构化的分析报告
        3. 提供数据洞察和建议
        
        输出格式要求：
        - 结论优先
        - 数据支撑观点
        - 用列表/分段清晰呈现
        - 如有趋势，明确指出
        """)
    String analyze(@UserMessage String userMessage);

    @SystemMessage("你是数据报告生成专家")
    String generateReport(@UserMessage @V("topic") String topic,
                          @V("data") String data);

    static AnalysisAgent create(dev.langchain4j.model.chat.ChatLanguageModel model) {
        return dev.langchain4j.service.AiServices.builder(AnalysisAgent.class)
                .chatLanguageModel(model)
                .build();
    }
}
