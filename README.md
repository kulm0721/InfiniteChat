# InfiniteChat

InfiniteChat 是一个基于 Spring Boot 的即时聊天系统，采用微服务架构，提供用户认证、好友与群聊、实时消息、离线消息、AI 对话和红包等功能。

## 功能概览

- 邮箱验证码注册、密码登录、验证码登录、JWT 认证
- 好友申请、好友管理、群聊和群成员管理
- 基于 Netty/WebSocket 的实时消息推送
- 离线消息与历史消息查询
- 基于 Kafka、Canal、Redis 和 MySQL 的消息处理
- 基于 LangChain4j 的 AI 对话、流式对话和 RAG 知识库问答
- 基于 PgVector 的向量检索
- 红包发送、领取和过期处理
- MinIO 用户头像上传

## 项目结构

```text
InfiniteChat
├── Common              # 公共响应、异常、工具类和常量
├── GateWay             # Spring Cloud Gateway 网关
├── UserService         # 用户、好友、群聊和会话服务
├── AiService           # AI 对话和 RAG 服务
├── RealTimeService     # WebSocket 实时消息服务
├── OfflineDataService  # 离线消息和历史消息服务
└── RedPacketService    # 红包服务
```

## 服务端口

| 服务 | 端口 | 主要职责 |
| --- | ---: | --- |
| GateWay | 10010 | API 网关和路由 |
| OfflineDataService | 8101 | 离线消息、历史消息 |
| RealTimeService | 8102 | WebSocket 实时通信 |
| RedPacketService | 8103 | 红包业务 |
| UserService | 8104 | 用户和群聊业务 |
| AiService | 8105 | AI 对话和知识库 |

客户端通常通过网关访问服务：

```text
http://localhost:10010/api/user/**
http://localhost:10010/api/ai/**
http://localhost:10010/api/message/**
```

## 技术栈

- Java 17
- Spring Boot 3.5.11
- Spring Cloud Gateway、Nacos、OpenFeign
- MyBatis-Plus、MySQL、Redis
- Apache Kafka、Canal
- Netty、WebSocket
- LangChain4j、DashScope、PgVector
- MinIO

## 本地依赖

完整运行项目需要准备以下服务：

- MySQL：业务数据库 `infinitechat`
- Redis：默认端口 `6379`
- Kafka：默认地址 `localhost:9092`
- Nacos：默认地址 `localhost:18375`
- MinIO：默认地址 `http://localhost:9000`
- Canal：默认端口 `11111`
- PostgreSQL + pgvector：AI 向量数据库

## 配置环境变量

仓库中的配置文件不保存真实密码和 API Key。请参考 [.env.example](.env.example) 设置环境变量，或在 IntelliJ IDEA 的 Run/Debug Configuration 中填写。

主要变量包括：

```text
DB_USERNAME
DB_PASSWORD
REDIS_PASSWORD
MAIL_USERNAME
MAIL_PASSWORD
DASHSCOPE_API_KEY
BIGMODEL_API_KEY
PGVECTOR_USERNAME
PGVECTOR_PASSWORD
MINIO_ACCESS_KEY
MINIO_SECRET_KEY
CANAL_USERNAME
CANAL_PASSWORD
```

详细的 PowerShell 配置方式见 [LOCAL_SETUP.md](LOCAL_SETUP.md)。`.env.example` 只是示例文件，Spring Boot 不会自动加载它；真实 `.env` 文件也不要提交到 Git。

## 启动方式

1. 启动 MySQL、Redis、Kafka、Nacos、MinIO、Canal 和 PgVector。
2. 设置环境变量。
3. 在 IntelliJ IDEA 中分别运行各服务的 `*Application` 主类。
4. 最后运行网关 `GateWayApplication`，通过 `http://localhost:10010` 访问接口。

也可以使用 Maven 分别启动服务，例如：

```powershell
mvn -f UserService/pom.xml spring-boot:run
```

如果使用 IntelliJ IDEA 自带 Maven，请在 Maven 设置中选择对应的 Maven 安装目录。各服务目录也提供了 Maven Wrapper。

## 主要接口

用户服务统一前缀为 `/api/user`，包括：

- `GET /api/user/sendCaptcha`：发送邮箱验证码
- `POST /api/user/register`：注册
- `POST /api/user/login/password`：密码登录
- `POST /api/user/login/code`：验证码登录
- `GET /api/user/logout`：退出登录

AI 服务统一前缀为 `/api/ai`，包括：

- `POST /api/ai/chat`：普通对话
- `POST /api/ai/streamChat`：流式对话
- `GET /api/ai/summary`：会话摘要

更多业务说明见各模块中的 `*-BUSINESS.md` 文档。

## 安全说明

- 不要提交真实密码、邮箱授权码、API Key 或本地配置文件。
- 之前使用过的凭据如果曾经出现在公开仓库或共享历史中，应立即撤销并重新生成。
- 生产环境应使用独立账号、最小权限和独立密钥，不要使用开发环境默认凭据。

## License

当前项目尚未指定开源许可证。如需公开发布，请根据项目用途补充合适的 License。
