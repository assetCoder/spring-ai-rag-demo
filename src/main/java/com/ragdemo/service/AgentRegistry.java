package com.ragdemo.service;

import com.ragdemo.agent.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Component;

/**
 * Agent注册中心 - 管理所有Agent实例
 */
@Component
public class AgentRegistry {

    private final ChatLanguageModel chatModel;
    private final SearchTools searchTools;

    private CustomerServiceAgent customerServiceAgent;
    private AnalysisAgent analysisAgent;
    private SearchAgent searchAgent;
    private ChatAgent chatAgent;

    public AgentRegistry(ChatLanguageModel chatModel, SqliteVectorStore vectorStore) {
        this.chatModel = chatModel;
        this.searchTools = new SearchTools(vectorStore);
        init();
    }

    private void init() {
        this.customerServiceAgent = CustomerServiceAgent.create(chatModel, searchTools);
        this.analysisAgent = AnalysisAgent.create(chatModel);
        this.searchAgent = SearchAgent.create(chatModel, searchTools);
        this.chatAgent = ChatAgent.create(chatModel);
    }

    public ChatLanguageModel chatModel() { return chatModel; }
    public CustomerServiceAgent customerServiceAgent() { return customerServiceAgent; }
    public AnalysisAgent analysisAgent() { return analysisAgent; }
    public SearchAgent searchAgent() { return searchAgent; }
    public ChatAgent chatAgent() { return chatAgent; }
}
