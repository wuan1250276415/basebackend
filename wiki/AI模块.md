# AI 模块

> `basebackend-ai` — 平台级 AI 基础设施，多模型适配、会话管理、RAG、Token 追踪

## 概述

基于 RestClient + SSE 实现的轻量级 AI 模块，不依赖 Spring AI（2.0.0-M2 不稳定），直接调用 OpenAI 兼容 API。

## 模块结构

```
basebackend-ai/
├── client/          # AI 客户端
│   ├── OpenAiClient         — OpenAI 兼容客户端 (RestClient + SSE)
│   ├── DeepSeekClient       — DeepSeek 适配 (继承 OpenAiClient)
│   └── QianWenClient        — 通义千问适配 (继承 OpenAiClient)
├── prompt/          # 提示词模板
│   ├── PromptTemplate       — {{variable}} 模板引擎
│   ├── PromptTemplateRegistry — 模板注册表
│   └── PromptBuilder        — 链式 Prompt 构建器
├── conversation/    # 会话管理
│   ├── InMemoryConversationManager — 内存实现 (TTL + 历史轮数)
│   └── RedisConversationManager    — Redis 实现 (分布式)
├── rag/             # 检索增强生成
│   ├── SimpleVectorStore    — 内存向量存储 (余弦相似度)
│   ├── RagService           — RAG 服务
│   ├── TextSplitter         — 文本分割器
│   └── OpenAiEmbeddingClient — Embedding 客户端
├── token/           # Token 管理
│   ├── SimpleTokenCounter   — Token 计数 (CJK 加权)
│   └── UsageTracker         — 用量追踪
├── annotation/      # 声明式注解
│   ├── @AIGenerate          — 标注方法自动调用 AI 生成
│   └── AIGenerateAspect     — AOP 切面实现
└── config/
    └── AiAutoConfiguration  — 自动配置 (basebackend.ai.enabled=true)
```

## 快速使用

### 1. 启用模块

```yaml
basebackend:
  ai:
    enabled: true
    provider: openai          # openai / deepseek / qianwen
    api-key: ${AI_API_KEY}
    base-url: https://api.openai.com
    model: gpt-4o
    max-tokens: 4096
    temperature: 0.7
```

### 2. 单轮对话

```java
@Autowired
private OpenAiClient aiClient;

ChatResponse response = aiClient.chat("分析这段代码的性能瓶颈...");
System.out.println(response.content());
```

### 3. 流式输出

```java
aiClient.streamChat(messages, chunk -> {
    System.out.print(chunk.content());
});
```

### 4. 会话管理

```java
@Autowired
private InMemoryConversationManager conversationManager;

// 自动维护上下文（TTL + 最大历史轮数）
String sessionId = "user-123";
conversationManager.addMessage(sessionId, "user", "你好");
List<ChatMessage> history = conversationManager.getMessages(sessionId);
```

### 5. @AIGenerate 注解

```java
@AIGenerate(prompt = "为以下内容生成摘要: {{content}}")
public String generateSummary(String content) {
    return null; // 由 AOP 切面自动调用 AI 生成
}
```

### 6. RAG 检索增强

```java
@Autowired
private RagService ragService;

// 索引文档
ragService.indexDocument("doc-1", "Java 25 引入了虚拟线程...");

// 检索增强问答
String answer = ragService.queryWithContext("什么是虚拟线程？", 3);
```

## 设计决策

- **不依赖 Spring AI**: Spring AI 2.0.0-M2 是 milestone 版本，不适合生产
- **OpenAI 兼容基类**: DeepSeek/通义千问 仅需修改 base-url，零额外代码
- **内存优先**: SimpleVectorStore 和 InMemoryConversationManager 作为默认，Redis/Milvus 可选升级
- **所有 Bean 支持 @ConditionalOnMissingBean**: 业务层可覆盖任何组件

## 测试覆盖

57 个测试全部通过：
- AiModelTest (15): ChatMessage/ChatResponse/ChatRequest/AiProperties
- PromptTest (20): 模板渲染/注册表/Builder/边界情况
- InMemoryConversationManagerTest (14): 会话CRUD/TTL/历史限制
- TokenTest (16): CJK计数/用量追踪/统计
- RagTest (10): 向量存储/文本分割/相似度搜索
- AiExceptionTest (8): 异常体系
