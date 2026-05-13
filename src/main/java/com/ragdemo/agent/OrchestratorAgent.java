package com.ragdemo.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 编排Agent（Orchestrator）
 * <p>
 * 职责：将用户复杂任务拆解为多步执行计划。
 * 第一步：如果任务是单步（简单路由），只返回一个步骤。
 * 第二步：如果任务是复杂任务，返回多步流水线。
 * </p>
 */
public interface OrchestratorAgent {

    @SystemMessage("""
        你是任务规划专家。你的职责：将用户问题拆解为一系列有序的执行步骤。
        
        可用的Agent（工具）和能力：
        - "客服"：用户咨询、问答、帮助类问题（回答通用产品/服务问题）
        - "分析"：数据分析、报告生成、统计、趋势分析
        - "搜索"：从知识库检索信息，查找文档内容（RAG检索）
        - "对话"：日常聊天、通用对话、无需特殊处理的问题
        - "总结"：总结前面步骤的结果、生成最终回复
        
        输出格式要求：
        返回一个JSON数组，每个元素包含 "agent" 和 "input" 字段。
        "input" 是给该Agent的指令（可以引用上一步结果，用 {result} 表示）。
        
        示例1 - 简单问答：
        用户：Spring Boot怎么配置？
        返回：[{"agent":"搜索","input":"Spring Boot 配置方法"}]
        
        示例2 - 复杂任务（分析+搜索+总结）：
        用户：分析一下最新文档中的技术趋势
        返回：[{"agent":"搜索","input":"查找最近的文档和技术资料"},{"agent":"分析","input":"对{result}进行技术趋势分析"},{"agent":"总结","input":"整合{result}输出最终分析报告"}]
        
        示例3 - 对话：
        用户：你好
        返回：[{"agent":"对话","input":"你好"}]
        
        重要：必须返回一个合法的JSON数组，不要包含markdown代码块标记。
        """)
    String plan(@UserMessage String userMessage);

    static OrchestratorAgent create(dev.langchain4j.model.chat.ChatLanguageModel model) {
        return dev.langchain4j.service.AiServices.builder(OrchestratorAgent.class)
                .chatLanguageModel(model)
                .build();
    }
}
