package com.ragdemo.agent;

import com.ragdemo.service.SqliteVectorStore;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

/**
 * 搜索工具 - Agent可调用的工具
 */
public class SearchTools {

    private final SqliteVectorStore vectorStore;

    public SearchTools(SqliteVectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Tool("从知识库中根据关键词检索相关文档内容")
    public String searchDocs(@P("搜索关键词") String query) {
        if (vectorStore == null) return "知识库未初始化";
        var results = vectorStore.searchSimilar(query, 3);
        if (results.isEmpty()) {
            return "未找到相关文档";
        }
        return String.join("\n---\n", results);
    }

    @Tool("获取当前时间")
    public String currentTime() {
        return java.time.LocalDateTime.now().toString();
    }

    @Tool("计算数学表达式")
    public String calculate(@P("数学表达式，如 1+2*3") String expression) {
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
