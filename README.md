# Multi-Agent RAG System

基于 **LangChain4j** + **Spring Boot 3** + **DeepSeek** 的多Agent检索增强生成（RAG）系统。

> ⚡ 轻量级 | 零外部依赖 | 单JAR部署 | SQLite持久化

## 🏗️ 系统架构

```
用户输入 ──→ OrchestratorAgent (智能路由)
                  │
       ┌──────────┼──────────────┐
       ▼          ▼              ▼
  客服Agent   分析Agent       搜索Agent     对话Agent
  (FAQ/咨询)  (数据分析/报告)  (知识库RAG)   (通用对话)
       │          │              │
       └──────────┼──────────────┘
                  ▼
            SearchTools
     (知识库检索 / 计算 / 时间)
                  │
                  ▼
           SQLite 持久化存储
```

## ✨ 核心特性

### 🤖 多Agent智能路由
- **OrchestratorAgent** - 自动识别用户意图，分发到最合适的Agent
- **客服Agent** - 产品咨询、FAQ、帮助问答
- **分析Agent** - 数据分析、报告生成、趋势洞察
- **搜索Agent** - SQLite知识库检索（RAG）
- **对话Agent** - 通用聊天、日常对话

### 📚 RAG知识库
- 上传文档（.txt / .md）→ SQLite持久化存储
- 关键词检索匹配
- 自动加载启动目录下的文档
- 支持运行时动态上传

### 🛠️ 工具调用
- 知识库检索（searchDocs）
- 数学计算（calculator）
- 时间查询（currentTime）

## 🚀 快速开始

### 前置条件
- **Java 17+**
- **Maven 3.8+**
- **DeepSeek API Key**（[获取Key](https://platform.deepseek.com/api_keys)）

### 一键运行

```bash
# 1. 克隆项目
git clone https://github.com/assetCoder/spring-ai-rag-demo.git
cd spring-ai-rag-demo

# 2. 配置API Key
export DEEPSEEK_API_KEY=sk-your-deepseek-api-key

# 3. 启动（自动下载依赖）
mvn spring-boot:run

# 4. 访问
open http://localhost:8080
```

## 📖 使用指南

### 💬 对话测试

| 输入 | 期望响应 |
|------|----------|
| "你好，有什么功能？" | 客服Agent - 功能介绍 |
| "分析一下上传的文档内容" | 分析Agent - 数据分析 |
| "帮我查一下Spring Boot配置" | 搜索Agent - 知识库检索 |
| "随便聊聊今天的天气" | 对话Agent - 通用对话 |
| "128 * 256 等于多少？" | 工具调用 - 计算器 |

### 📄 知识库使用
1. 点击"知识库"标签页
2. 上传 `.txt` 或 `.md` 文档
3. 切换到"对话"标签页提问
4. 搜索Agent会自动检索知识库内容

## 📡 API接口

### 对话
```bash
POST /api/chat
Content-Type: application/json

{"message": "Spring Boot如何配置数据源？"}

Response:
{
  "reply": "...",
  "agent": "auto"
}
```

### 知识库上传
```bash
POST /api/rag/upload
Content-Type: multipart/form-data

file=@document.txt

Response:
{
  "message": "文档上传成功",
  "size": "1234 字符"
}
```

### 知识库查询
```bash
POST /api/rag/ask
Content-Type: application/json

{"question": "文档中提到了什么内容？"}
```

## 🗄️ 数据存储

| 存储 | 位置 | 说明 |
|------|------|------|
| 向量库 | `./data/rag.db` | SQLite，持久化文档内容 |
| 文档源 | `./docs/` | 启动时自动加载 .txt / .md |
| 日志 | 控制台 | Spring Boot默认日志 |

## 🧱 技术栈

| 组件 | 选型 | 版本 |
|------|------|------|
| 核心框架 | LangChain4j | 0.35.0 |
| LLM | DeepSeek Chat | deepseek-chat |
| 后端框架 | Spring Boot | 3.2.5 |
| 语言 | Java | 17 |
| 数据库 | SQLite | 3.45.1 |
| 前端 | Thymeleaf + 原生JS | - |
| 构建 | Maven | - |

## 📂 项目结构

```
src/main/java/com/ragdemo/
├── RagDemoApplication.java           # 启动类
├── agent/                            # Agent层
│   ├── OrchestratorAgent.java        # 路由Agent
│   ├── CustomerServiceAgent.java     # 客服Agent
│   ├── AnalysisAgent.java            # 分析Agent
│   ├── SearchAgent.java              # 搜索Agent（RAG）
│   ├── ChatAgent.java                # 对话Agent
│   └── SearchTools.java              # 工具集
├── config/
│   ├── AppConfig.java                # DeepSeek配置
│   └── WebConfig.java                # Web配置
├── controller/
│   ├── ChatController.java           # 聊天API
│   ├── RagController.java            # 知识库API
│   └── HomeController.java           # 页面路由
└── service/
    ├── AgentRegistry.java            # Agent注册中心
    ├── OrchestratorService.java      # 编排服务
    ├── DocumentService.java          # 文档管理
    ├── SqliteVectorStore.java        # SQLite向量库
    └── AppInitializer.java           # 初始化
```

## 🚀 路线图

- [x] 多Agent路由架构
- [x] RAG知识库（关键字检索）
- [x] 工具调用（计算/时间/检索）
- [x] SQLite持久化存储
- [x] Web管理界面
- [ ] 真正向量嵌入检索（OpenAI/BGE）
- [ ] 流式输出（SSE/WebSocket）
- [ ] 文档自动分块（Chunking）
- [ ] 多轮对话记忆管理
- [ ] Agent间协作流程

## 📄 License

MIT
