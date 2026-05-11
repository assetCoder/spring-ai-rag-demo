# Multi-Agent RAG Demo

基于 **LangChain4j** + **DeepSeek** 的多Agent检索增强生成演示项目。

## 🏗️ 架构

```
用户输入 → OrchestratorAgent (路由)
                │
    ┌───────────┼────────────┐
    ▼           ▼            ▼
客服Agent   分析Agent     搜索Agent    对话Agent
(Q&A/FAQ)  (数据分析/报告) (知识库检索)  (默认聊天)
    │           │            │
    └───────────┼────────────┘
                ▼
          SearchTools
    (知识库检索/计算/时间)
```

## ✨ 功能

- **多Agent智能路由** - 自动识别用户意图，分配合适的Agent
- **客服Agent** - 问答、FAQ、产品咨询
- **分析Agent** - 数据分析、报告生成、趋势洞察
- **搜索Agent** - 知识库检索、信息查找（含工具调用）
- **对话Agent** - 日常聊天
- **RAG知识库** - 上传文档 → 向量化 → SQLite持久化 → 语义检索
- **工具调用** - 知识库搜索、计算器、获取时间

## 🚀 快速开始

### 前置条件
- Java 17+
- Maven
- DeepSeek API Key

### 运行

```bash
# 1. 配置 DeepSeek API Key
export DEEPSEEK_API_KEY=sk-your-deepseek-key-here

# 2. 运行
./mvnw spring-boot:run
```

### 访问

打开 http://localhost:8080

## 📡 API

### 对话
```
POST /api/chat
Body: {"message": "你好"}
Response: {"reply": "...", "agent": "对话"}
```

### 知识库上传
```
POST /api/rag/upload
Form: file=@document.txt
Response: {"message": "文档上传成功"}
```

## 🧪 测试用例

输入以下消息测试Agent路由：

| 消息 | 期望路由 |
|------|----------|
| "你好，有什么功能？" | 客服Agent |
| "分析一下上传的文档内容" | 分析Agent |
| "帮我找一下Spring Boot配置" | 搜索Agent |
| "随便聊聊" | 对话Agent |

## 🗄️ 数据存储

- **向量库**: SQLite (`data/rag.db`)，轻量持久化
- **文档目录**: `./docs/`，自动加载 `.txt` / `.md` 文件

## 🧱 技术栈

| 组件 | 选型 |
|------|------|
| 框架 | LangChain4j 0.35.0 |
| LLM | DeepSeek (deepseek-chat) |
| 嵌入模型 | DeepSeek (deepseek-embedding) |
| 向量库 | 自建SQLite + 余弦相似度 |
| 后端 | Spring Boot 3.2.5 / Java 17 |
| 前端 | Thymeleaf + 原生JS |

## 📂 项目结构

```
src/main/java/com/ragdemo/
├── RagDemoApplication.java       # 启动类
├── agent/                        # Agent层
│   ├── OrchestratorAgent.java    # 路由Agent
│   ├── CustomerServiceAgent.java # 客服Agent
│   ├── AnalysisAgent.java        # 分析Agent
│   ├── SearchAgent.java          # 搜索Agent
│   ├── ChatAgent.java            # 对话Agent
│   └── SearchTools.java          # 工具集
├── config/
│   └── AppConfig.java            # DeepSeek配置
├── controller/
│   ├── ChatController.java       # 聊天API
│   ├── RagController.java        # 知识库API
│   └── HomeController.java       # 页面路由
└── service/
    ├── AgentRegistry.java        # Agent注册中心
    ├── OrchestratorService.java  # 编排服务
    ├── DocumentService.java      # 文档管理
    ├── SqliteVectorStore.java    # SQLite向量库
    └── AppInitializer.java       # 启动初始化
```
