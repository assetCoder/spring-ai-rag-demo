# Spring AI RAG Demo

基于 Spring AI 的 RAG（检索增强生成）查询演示项目。

## 功能
- 文档加载与向量化
- 语义搜索与检索
- AI 增强问答

## 快速开始

```bash
# 配置 DeepSeek API Key
export DEEPSEEK_API_KEY=your_key_here

# 运行
./mvnw spring-boot:run
```

## API

- `POST /api/rag/ask` - 提问
- `POST /api/rag/upload` - 上传文档
