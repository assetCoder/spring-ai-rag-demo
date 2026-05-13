package com.ragdemo.agent;

import com.ragdemo.service.VectorStore;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

/**
 * 搜索工具 - Agent可调用的工具
 */
public class SearchTools {

    private final VectorStore vectorStore;

    public SearchTools(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Tool("从知识库中根据语义搜索相关文档内容")
    public String searchDocs(@P("搜索关键词或问题") String query) {
        var results = vectorStore.searchSimilar(query, 3);
        if (results.isEmpty()) {
            return "未找到相关文档";
        }
        var sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            sb.append("【结果").append(i + 1).append("】\n");
            sb.append(results.get(i)).append("\n\n");
        }
        return sb.toString().trim();
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
