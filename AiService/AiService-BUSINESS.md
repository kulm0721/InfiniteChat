# AiService 业务逻辑说明

> AI 对话微服务：基于 LangChain4j + 阿里云百炼（DashScope）通义千问大模型，提供 AI 对话、流式对话、RAG 知识库问答、工具调用（邮件/知识库/MCP 联网搜索）与监控埋点能力。

## 一、服务概述

| 项目 | 说明 |
| --- | --- |
| 服务名 | `AiService` |
| 端口 | `8105`（context-path：`/api`） |
| 技术栈 | Spring Boot 3.5、LangChain4j、DashScope（qwen-max / qwen3.7-text-embedding）、Redis（对话记忆）、PGVector（向量库）、JavaMailSender、Micrometer（监控） |
| AI 接口 | 非流式 `/chat`、流式 `/streamChat` |

## 二、模块结构

```
AiService
└── src/main/java/com/shanyangcode/aiservice
    ├── controller/       # 接口层（AiChatController）
    ├── ai/               # AI 会话抽象（AiChat 接口）+ AI 装配（AiChatService）
    ├── tool/             # AI 工具（RagTool / EmailTool / TimeTool）
    ├── service/          # RAG 文档加载（RagDataLoader，启动时执行）
    ├── config/           # 模型 / 向量库 / RAG / MCP / Redis 记忆 / CORS 配置
    ├── guardrail/        # 输入安全护栏（SafeInputGuardrail）
    ├── monitor/          # 模型调用监控（Listener / MetricsCollector / ContextHolder）
    ├── model/dto/        # 入参（ChatRequest / KnowledgeRequest）
    ├── common/           # 统一响应
    └── exception/        # 异常体系
```

## 三、接口一览

统一前缀：`/api`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/chat` | 非流式对话（一次性返回完整回答） |
| POST | `/api/streamChat` | 流式对话（SSE，逐 token 推送） |

### 请求体 ChatRequest

```json
{
  "sessionId": 1001,   // 会话 ID（Long），用于隔离对话记忆
  "userId": 1002,      // 用户 ID（Long），用于监控埋点
  "prompt": "你好"      // 用户输入
}
```

## 四、核心业务流程

### 1. 非流式对话（/chat）

1. 根据请求中的 `userId`、`sessionId` 构造 `MonitorContext` 并写入 `MonitorContextHolder`（ThreadLocal）。
2. 调用 `AiChat.chat(sessionId, prompt)`：
   - 先经过 **SafeInputGuardrail** 输入校验（见下文）。
   - 以 `sessionId` 作为 MemoryId 加载该会话的**对话记忆**（最近 20 条消息）。
   - 动态注入系统提示词：加载 `system-prompt/chat-bot.txt`，并拼接当前**北京时间**（Asia/Shanghai）。
   - 检索 RAG 知识库（contentRetriever）获取相关上下文。
   - 由模型决定是否调用工具（RagTool / EmailTool / MCP 联网搜索）。
3. 返回完整回答字符串；`finally` 清理 `MonitorContext`。

### 2. 流式对话（/streamChat）

1. 构造 `MonitorContext`，通过 `Flux.defer` 保证订阅时再写入 ThreadLocal（响应式上下文传递）。
2. 调用 `AiChat.streamChat(sessionId, prompt)`，返回 `Flux<String>` 流式输出。
3. `doFinally` 清理 `MonitorContext`，避免 ThreadLocal 泄漏。

### 3. 对话记忆（Redis）

- 由 `RedisChatMemoryStoreConfig` 构建 `RedisChatMemoryStore`（host/port/password/ttl 取 `spring.data.redis` 配置，ttl=3600s）。
- `AiChatService` 为每个 MemoryId（即 `sessionId`）创建 `MessageWindowChatMemory`，`maxMessages=20`。
- 效果：同一会话可连续多轮追问，上下文存 Redis，服务重启不丢失。

### 4. RAG 知识库

**加载阶段（启动时，RagDataLoader）**：
- `CommandLineRunner` 在应用启动时读取 `rag.docs-path` 目录下的文档，用 `EmbeddingStoreIngestor` 切分并向量化入库。

**检索阶段（问答时，RagConfig）**：
- `DocumentByParagraphSplitter(300, 100)` 按段落切分（maxSegmentSize=300，maxOverlapSize=100）。
- 每个片段文本前拼上 `file_name` 前缀作为来源标识。
- `EmbeddingStoreContentRetriever`：`maxResults=5`、`minScore=0.75`（相似度低于 0.75 的结果被丢弃）。
- 向量库使用 **PGVector**（`PgVectorEmbeddingStore`，dimension=1024，启动时 `dropTableFirst=true` 重建表）。

**新增知识（RagTool，模型主动调用）**：
- 当用户要求"保存问答对/知识点/向知识库添加信息"时，模型调用 `addKnowledgeToRag(question, answer, fileName)`。
- 将内容格式化为 `### Q: xxx\n\nA: xxx` 追加写入本地 md 文件（文件名默认 `InfiniteChat.md`，自动补 `.md` 后缀），再通过 `EmbeddingStoreIngestor` 向量化入库，实现"文件 + 向量"双写。

### 5. AI 工具集

| 工具 | 说明 |
| --- | --- |
| `RagTool.addKnowledgeToRag` | 保存知识到知识库（写文件 + 入向量库） |
| `EmailTool.sendEmail` | 发送邮件（`SimpleMailMessage`），发件人取 `spring.mail.username` |
| MCP 搜索（BigModelSearchMcpClient） | 通过智谱开放平台 MCP SSE 接口提供联网搜索能力（`McpToolConfig`，api-key 拼接在 SSE URL 中） |
| `TimeTool.getCurrentTime` | 获取北京/上海当前时间（未注册到 AiChat 装配中，仅作为独立工具示例） |

### 6. 输入安全护栏（SafeInputGuardrail）

- 通过 `@InputGuardrails({SafeInputGuardrail.class})` 注解绑定到 `AiChat` 接口。
- 敏感词表：`{"死", "杀"}`；用户消息中包含任一敏感词直接 `fatal` 拒绝（提示"提问不能包含敏感词！！！"）。
- 系统提示词中另有内容安全约束：禁止政治敏感、暴力色情、辱骂攻击内容；保护隐私信息（密码/身份证/银行卡/地址等）；输出必须纯文本、结尾带感叹号。

### 7. 模型调用监控（monitor）

- `AiChatController` 把 `userId/sessionId` 写入 `MonitorContextHolder`（InheritableThreadLocal）。
- `AiModelMonitorListener`（ChatModelListener）挂钩 `QwenChatModel` / `QwenStreamingChatModel` 的请求生命周期：
  - `onRequest`：记录开始时间 + 请求计数（`ai_model_requests_total`）。
  - `onResponse`：记录请求成功、响应耗时（`ai_model_response_duration_seconds`）、Token 消耗（`ai_model_tokens_total`，区分 input/output/total）。
  - `onError`：记录请求失败 + 错误计数（`ai_model_errors_total`）+ 耗时。
- 指标通过 Micrometer 暴露给 Prometheus（`management.endpoints.web.exposure.include=health,info,prometheus`）。

## 五、模型与配置

| 配置项 | 值 | 说明 |
| --- | --- | --- |
| `langchain4j.community.dashscope.chat-model.model-name` | qwen-max | 对话模型 |
| `langchain4j.community.dashscope.chat-model.api-key` | （百炼 API Key） | 通义千问密钥 |
| `langchain4j.community.dashscope.embedding-model.model-name` | qwen3.7-text-embedding | Embedding 模型 |
| `bigmodel.api-key` | （智谱 API Key） | MCP 联网搜索 |
| `rag.docs-path` | `.../InfiniteChat-Agent/src/main/resources/docs` | RAG 文档目录 |
| `pgvector.*` | host/port/user/password/table | PGVector 向量库（dimension=1024） |
| `spring.data.redis.ttl` | 3600 | 对话记忆过期时间（秒） |
| `spring.mail.*` | smtp.qq.com:587 | 邮件发送 |

## 六、错误码（部分）

| code | message | 触发场景 |
| --- | --- | --- |
| 40003 | 包含敏感词，请求被拒绝 | 输入护栏拦截 |
| 40100~40105 | 未登录 / 无权限 / 令牌相关 | 认证与令牌 |
| 50000 | 系统内部异常 | 全局兜底 |
| 50004 | 请先登录 | 未登录访问 |
| 90003 | 无效token，请重新登录 | WebSocket 校验 |

## 七、其他

- **CORS**：`CorsConfig` 允许所有来源与请求方法、暴露全部响应头。
- **统一响应 / 异常**：`ResultUtils` + `BaseResponse` + `GlobalExceptionHandler`（处理 `BusinessException`、参数校验异常）。
- **静态资源**：`resources/front/` 下内置了一个简易测试前端（`index.html` + 用户/头像图）。
