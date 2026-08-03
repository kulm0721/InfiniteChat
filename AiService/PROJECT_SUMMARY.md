# InfiniteChat-Agent (千言) 项目总结

## 项目概述
基于 Spring Boot 4.1 + LangChain4j 1.1.0 的 AI 智能聊天机器人后端服务。

## 技术栈
- **框架**: Spring Boot 4.1.0, Java 17
- **AI 框架**: LangChain4j 1.1.0
- **对话模型**: 阿里云 DashScope — qwen-max (通义千问旗舰)
- **Embedding 模型**: 阿里云 DashScope — qwen3.7-text-embedding (向量化)
- **MCP 工具**: 智谱 BigModel (网络搜索) + mcp-server-time (本地时间)
- **会话记忆**: Redis (RedisChatMemoryStore)，滑动窗口 max 20 条
- **向量数据库**: PgVector (PostgreSQL + pgvector 扩展)，Docker 部署
- **文档存储**: 本地文件系统 (docs/ 目录)

## 项目结构
```
src/main/java/com/shanyangcode/infinitechatagent/
├── ai/                     # AI 接口与核心服务
│   ├── AiChat.java         # AI 聊天接口定义 (@SystemMessage, @MemoryId, @InputGuardrails)
│   └── AiChatService.java  # AI Bean 组装 (对话+记忆+RAG+工具)
├── config/                  # Spring 配置类
│   ├── EmbeddingStoreConfig.java  # PgVector 向量库配置
│   ├── RagConfig.java             # RAG 管道配置 (Ingestor + ContentRetriever)
│   └── RedisChatMemoryStoreConfig.java # Redis 会话记忆配置
├── controller/
│   └── AiChatController.java      # /api/chat?sessionId=xxx&prompt=xxx
├── service/
│   └── RagDataLoader.java         # 启动时自动加载 docs/ 文档入库
├── tool/
│   └── RagTool.java               # AI 工具：动态添加知识到知识库
├── guardrail/
│   └── SafeInputGuardrail.java    # 输入护栏：敏感词过滤
├── exception/
│   └── GlobalExceptionHandler.java # 全局异常处理
└── common/
    ├── BaseResponse.java          # 统一响应体
    ├── ErrorCode.java             # 业务错误码枚举
    └── ResultUtils.java           # 响应工具类

src/main/resources/
├── application.yml                # 核心配置
├── docs/InfiniteChat.md          # 知识库文档
└── system-prompt/chat-bot.txt    # AI 系统提示词
```

## 核心架构

### AiChat 接口 (组装了什么)
```
AiChat = AI 模型 (qwen-max)
       + ContentRetriever (RAG 检索：PgVector 向量搜索，Top5，minScore 0.75)
       + ChatMemory (Redis 会话记忆：sessionId 隔离，max 20 条)
       + McpToolProvider (智谱网络搜索 + 本地时间工具)
       + SafeInputGuardrail (敏感词：死/杀 拦截)
```

### RAG 管道
```
文档 → DocumentByParagraphSplitter(300字/段, 重叠100)
     → textSegmentTransformer (加文件名前缀)
     → qwen3.7-text-embedding (向量化, 1024维)
     → PgVectorEmbeddingStore → dp_embedding 表 (持久化)
```

### MCP 工具
- **智谱搜索**: HTTP SSE → `open.bigmodel.cn/api/mcp/web_search/sse`
- **本地时间**: Stdio → `uvx mcp-server-time --local-timezone=Asia/Shanghai`

## Docker 服务
| 服务 | 容器名 | 端口 | 凭证 |
|------|--------|------|------|
| PgVector | pgvector | 54328:5432 | root / [REDACTED] |
| Redis | (需确认) | 6379 | - |

PgVector 数据库: `dp`, 表: `dp_embedding`, 数据目录: `C:\docker\pgvector\data`

## 当前待解决问题
1. **RagDataLoader 启动重复入库**: 每次重启都把 docs/ 重新 Embedding 一遍，PgVector 数据重复。需加"已加载"检查。
2. **RagConfig transformer**: 已修复 `file_name` key 统一。
3. **API Key 硬编码在 yml**: 安全隐患，生产环境需迁移到环境变量。

## API 端点
```
GET /api/chat?sessionId=xxx&prompt=你好
```
