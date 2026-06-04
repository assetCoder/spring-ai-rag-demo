# Alan Assistant — 多模态 AI Telegram Bot

基于 **LangChain4j** + **Spring Boot 3** + **DeepSeek + 阿里云百炼** 的多模态多Agent Telegram Bot。

> ⚡ Telegram 交互 | 图片理解/生成 | 语音对话 | 语义搜索 | 单JAR部署

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                    Telegram Bot                          │
│                 @localRagDemoBot                         │
│            (Alan Assistant — 多模态AI助手)                │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│            OrchestratorService (编排+对话记忆)            │
│        智能路由 + 多模态记忆注入 + 10轮滑动窗口            │
└────┬──────────┬──────────┬──────────┬───────────────────┘
     │          │          │          │
     ▼          ▼          ▼          ▼
  客服Agent   分析Agent   搜索Agent  对话Agent
 (FAQ/咨询)  (数据分析)  (知识库RAG) (通用对话 + 多模态)
     │          │          │
     └──────────┼──────────┘
                ▼
          SearchTools
   (知识库检索 / 计算 / 时间)
                │
                ▼
        InMemoryEmbeddingStore
        (JSON持久化 / 语义搜索)


  ──── 多模态扩展层 ────

  图片 ──→ VisionService (Qwen-VL) ──────→ 图片理解 → 注入记忆
  语音 ──→ SpeechService (FunASR+CosyVoice) → 语音↔文字 → 注入记忆
  "画..." ─→ ImageGenerationService (Qwen-Image 2.0 Pro) → 图片生成
```

## ✨ 核心特性

### 🤖 多Agent智能路由
- **OrchestratorAgent** - 自动识别用户意图，分发到最合适的Agent
- **客服Agent** - 产品咨询、FAQ、帮助问答
- **分析Agent** - 数据分析、报告生成、趋势洞察
- **搜索Agent** - 知识库检索（RAG）
- **对话Agent** - 通用聊天，了解自身所有多模态能力

### 🖼️ 多模态能力（阿里云百炼全家桶）
- **图片理解** - 发图给 Bot，Qwen-VL 自动分析，结果注入对话记忆
- **图片生成** - 说"画一张..."，Qwen-Image 2.0 Pro 秒出图
- **语音对话** - 发语音→FunASR转文字→AI回答→CosyVoice语音回复
- **多模态记忆** - 图片分析结果、语音识别内容主动注入对话上下文，实现跨模态延续

### 📚 RAG知识库（语义搜索）
- 上传文档 → 自动分块 → 本地 AllMiniLmL6V2 嵌入向量化 → 语义搜索
- 语义检索（余弦相似度），理解"意思相近"而非"关键词匹配"
- JSON文件持久化，重启不丢失
- 支持运行时动态上传 + 启动时自动加载 docs/ 目录

### 🛠️ 工具调用
- 知识库检索（searchDocs）
- 数学计算（calculator）
- 时间查询（currentTime）

## 🚀 快速开始

### 前置条件
- **Java 17+**
- **Maven 3.8+**
- **DeepSeek API Key**（[获取Key](https://platform.deepseek.com/api_keys)）- 文字对话
- **阿里云百炼 API Key**（[获取Key](https://bailian.console.aliyun.com/)） - 多模态（可选）

### 一键运行

```bash
# 1. 克隆项目
git clone https://github.com/assetCoder/spring-ai-rag-demo.git
cd spring-ai-rag-demo

# 2. 配置环境变量
export DEEPSEEK_API_KEY=sk-your-deepseek-api-key
export QWEN_API_KEY=sk-your-aliyun-bailian-key    # 多模态，可选
export TELEGRAM_BOT_TOKEN=your-bot-token            # Telegram Bot，可选

# 3. 打包运行
mvn package -DskipTests
java -jar target/spring-ai-rag-demo-1.0.0.jar

# 4. 访问
open http://localhost:8080
```

## 🤖 Telegram Bot

### 配置

```bash
export TELEGRAM_BOT_TOKEN=your-bot-token
export DEEPSEEK_API_KEY=sk-your-deepseek-api-key
export QWEN_API_KEY=sk-your-aliyun-bailian-key    # 多模态支持
java -jar target/spring-ai-rag-demo-1.0.0.jar
```

### 支持的操作

| 操作 | 说明 | 依赖 |
|------|------|------|
| 💬 文字消息 | 多Agent对话 | DeepSeek |
| 🖼️ 发送图片 | 自动分析图片内容 | Qwen-VL |
| 🎨 说"画一张..." | AI生成图片 | Qwen-Image |
| 🎤 发语音消息 | 语音→文字→回复→语音回复 | FunASR+CosyVoice |

## 📖 使用指南

### 💬 对话测试

| 输入 | 期望响应 |
|------|----------|
| "你好，有什么功能？" | 客服Agent - 功能介绍 |
| "分析一下上传的文档内容" | 分析Agent - 数据分析 |
| "帮我查一下Spring Boot配置" | 搜索Agent - 知识库检索 |
| "随便聊聊今天的天气" | 对话Agent - 通用对话 |
| "128 * 256 等于多少？" | 工具调用 - 计算器 |
| "画一只在太空中的猫" | 图片生成 |

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

## 🧱 技术栈

| 组件 | 选型 | 版本 |
|------|------|------|
| 核心框架 | LangChain4j | 0.35.0 |
| 文字模型 | DeepSeek Chat | deepseek-chat |
| 视觉模型 | Qwen-VL (阿里云百炼) | qwen-vl-plus |
| 图片生成 | Qwen-Image (阿里云百炼) | qwen-image-2.0-pro |
| 语音识别 | FunASR (阿里云百炼) | fun-asr |
| 语音合成 | CosyVoice (阿里云百炼) | cosyvoice-v3.5-plus |
| 嵌入模型 | AllMiniLmL6V2 (本地，免API) | - |
| 后端框架 | Spring Boot | 3.2.5 |
| 语言 | Java | 17 |
| 向量库 | InMemoryEmbeddingStore | JSON文件持久化 |
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
│   ├── AppConfig.java                # DeepSeek配置 + 本地嵌入模型
│   └── WebConfig.java                # Web配置
├── controller/
│   ├── ChatController.java           # 聊天API
│   ├── RagController.java            # 知识库API
│   └── HomeController.java           # 页面路由
├── service/
│   ├── AgentRegistry.java            # Agent注册中心
│   ├── OrchestratorService.java      # 编排服务（工作流+对话记忆）
│   ├── DocumentService.java          # 文档管理
│   ├── VectorStore.java              # 向量库（语义搜索+JSON持久化）
│   ├── Chunker.java                  # 文档分块工具
│   ├── AppInitializer.java           # 启动初始化
│   ├── VisionService.java            # 图片理解（Qwen-VL）
│   ├── SpeechService.java            # 语音服务（FunASR+CosyVoice）
│   └── ImageGenerationService.java   # 图片生成（Qwen-Image）
└── telegrambot/
    └── TelegramBotService.java        # Telegram Bot 集成（多模态）
```

## 🚀 演进路线

- [x] v1.0 - 基础：多Agent路由 + DeepSeek对话
- [x] v1.1 - RAG：知识库上传 + 向量语义搜索 + 工具调用
- [x] v1.2 - 工作流：多步编排 + 对话记忆 + Web界面
- [x] v1.3 - Telegram Bot：Long Polling接入 + 消息路由
- [x] v1.4 - 图片理解：Qwen-VL集成，发图即分析
- [x] v1.5 - 图片生成：Qwen-Image，说"画一张..."即出图
- [x] v1.6 - 语音对话：FunASR语音识别 + CosyVoice语音合成
- [x] v1.7 - 本地嵌入：AllMiniLmL6V2取代DeepSeek Embedding，启动更快
- [x] v1.8 - 多模态记忆：图片分析/语音识别结果注入对话历史，实现跨模态上下文延续
- [x] v1.9 - 修复图片生成API：改用阿里云百炼 Multimodal Generation 同步接口，修复Qwen-Image 2.0 Pro调用
- [ ] v1.10 - 上下文优化：长对话窗口压缩、摘要轮转、多轮记忆持久化
- [ ] v2.0 - 流式输出（SSE/WebSocket）+ 更多LLM支持

## 📄 License

MIT
