package com.ragdemo.agent;

import dev.langchain4j.agent.tool.Tool;

/**
 * 搜索工具 - Agent可调用的工具
 */
public class SearchTools {

    @Tool("从知识库中检索相关文档")
    public String searchDocs(String query) {
        return "知识库检索功能需要部署后上传文档使用。当前可回答问题。";
    }

    @Tool("获取当前时间")
    public String currentTime() {
        return java.time.LocalDateTime.now().toString();
    }

    @Tool("计算数学表达式")
    public String calculate(String expression) {
        if (!expression.matches("[0-9+\\-*/.()% ]+")) {
            return "不支持的表达式";
        }
        try {
            var engine = new javax.script.ScriptEngineManager()
                    .getEngineByName("JavaScript");
            var result = engine.eval(expression);
            return result.toString();
        } catch (Exception e) {
            return "计算错误: " + e.getMessage();
        }
    }
}
