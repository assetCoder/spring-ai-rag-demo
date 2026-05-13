package com.ragdemo.service;

import com.ragdemo.agent.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Agent编排服务
 * <p>
 * 核心能力：
 * 1. 工作流（Workflow）：将复杂任务拆解为多步执行计划，串行执行并汇总
 * 2. 多轮对话记忆：MessageWindowChatMemory，记住最近10轮对话
 * 3. 也保留旧接口兼容：单Agent路由（作为最简单工作情况）
 * </p>
 */
@Service
public class OrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorService.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final AgentRegistry registry;
    private OrchestratorAgent orchestrator;
    /** 滑动窗口对话记忆，保留最近10条消息（用户+AI） */
    private final java.util.LinkedList<ChatMessage> chatHistory = new java.util.LinkedList<>();
    private static final int MAX_HISTORY = 10;

    public OrchestratorService(AgentRegistry registry) {
        this.registry = registry;
    }

    @PostConstruct
    public void init() {
        this.orchestrator = OrchestratorAgent.create(registry.chatModel());
    }

    /**
     * 统一入口：工作流执行
     *
     * @return WorkflowResult 包含最终回复 + 执行步骤日志
     */
    public WorkflowResult process(String message) {
        // 1. 保存用户消息到记忆
        addToHistory(new UserMessage(message));

        // 2. 获取对话上下文
        String context = buildContext();

        // 3. 生成执行计划
        List<Step> steps;
        try {
            // 带上下文的规划
            String planJson = orchestrator.plan("[对话上下文]\n" + context + "\n[当前问题]\n" + message);
            planJson = cleanJson(planJson);
            steps = parsePlan(planJson);
        } catch (Exception e) {
            log.warn("规划失败，降级为单步对话: {}", e.getMessage());
            steps = List.of(new Step("对话", message));
        }

        // 4. 串行执行
        String previousResult = null;
        List<StepResult> stepLogs = new ArrayList<>();

        for (int i = 0; i < steps.size(); i++) {
            Step step = steps.get(i);
            String input = step.input;

            // 替换 {result} 为上一步结果
            if (previousResult != null) {
                input = input.replace("{result}", previousResult);
            }

            log.info("执行步骤 {}: {} -> {}", i + 1, step.agent, input);
            long start = System.currentTimeMillis();

            String output;
            try {
                output = executeStep(step.agent, input);
            } catch (Exception e) {
                output = "执行出错: " + e.getMessage();
            }

            long elapsed = System.currentTimeMillis() - start;
            stepLogs.add(new StepResult(step.agent, input, output, elapsed));
            previousResult = output;
        }

        // 5. 最终回复 = 最后一步的结果
        String finalReply = previousResult != null ? previousResult : "处理完成";

        // 6. 保存AI回复到记忆
        addToHistory(new AiMessage(finalReply));

        return new WorkflowResult(finalReply, stepLogs);
    }

    /**
     * 向滑动窗口添加消息（超出上限时移除最早的消息）
     */
    private void addToHistory(ChatMessage msg) {
        chatHistory.addLast(msg);
        while (chatHistory.size() > MAX_HISTORY) {
            chatHistory.removeFirst();
        }
    }

    /**
     * 执行单步Agent
     */
    private String executeStep(String agent, String input) {
        return switch (agent) {
            case "客服" -> registry.customerServiceAgent().chat(input);
            case "分析" -> registry.analysisAgent().analyze(input);
            case "搜索" -> registry.searchAgent().search(input);
            case "总结" -> registry.chatAgent().chat(input + "\n\n请基于上述内容给出清晰的总结回复。");
            default -> registry.chatAgent().chat(input);
        };
    }

    /**
     * 构建对话上下文字符串
     */
    private String buildContext() {
        if (chatHistory.isEmpty()) return "(无历史对话)";

        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : chatHistory) {
            if (msg instanceof UserMessage um) {
                sb.append("用户: ").append(um.singleText()).append("\n");
            } else if (msg instanceof AiMessage am) {
                String text = am.text();
                if (text != null) {
                    if (text.length() > 100) text = text.substring(0, 100) + "...";
                    sb.append("AI: ").append(text).append("\n");
                }
            }
        }
        return sb.toString();
    }

    /**
     * 清理JSON字符串（去掉可能的markdown代码块标记）
     */
    private String cleanJson(String raw) {
        return raw.replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();
    }

    /**
     * 解析JSON执行计划
     */
    private List<Step> parsePlan(String json) {
        try {
            List<Map<String, String>> steps = JSON.readValue(json,
                    new TypeReference<List<Map<String, String>>>() {});
            return steps.stream()
                    .map(m -> new Step(m.get("agent"), m.get("input")))
                    .toList();
        } catch (Exception e) {
            log.warn("解析执行计划JSON失败, 降级为单步对话: {}", e.getMessage());
            return List.of(new Step("对话", json));
        }
    }

    /**
     * 直接使用搜索Agent查询知识库（RAG专用入口，保持向后兼容）
     */
    public String searchKnowledge(String query) {
        return registry.searchAgent().search(query);
    }

    // ========== 内部数据结构 ==========

    public record Step(String agent, String input) {}
    public record StepResult(String agent, String input, String output, long elapsedMs) {}
    public record WorkflowResult(String reply, List<StepResult> steps) {}
}
