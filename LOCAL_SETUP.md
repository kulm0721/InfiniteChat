# 本地配置

仓库中的配置文件只保留环境变量占位符，真实密码和 API Key 请通过运行环境提供。

## PowerShell

在启动服务前设置当前终端会话的变量，例如：

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "你的数据库密码"
$env:REDIS_PASSWORD = "你的 Redis 密码"
$env:MAIL_USERNAME = "你的邮箱"
$env:MAIL_PASSWORD = "邮箱授权码"
$env:DASHSCOPE_API_KEY = "你的百炼 API Key"
$env:BIGMODEL_API_KEY = "你的智谱 API Key"
$env:PGVECTOR_USERNAME = "root"
$env:PGVECTOR_PASSWORD = "你的 PGVector 密码"
$env:MINIO_ACCESS_KEY = "你的 MinIO 用户名"
$env:MINIO_SECRET_KEY = "你的 MinIO 密码"
$env:CANAL_USERNAME = "canal"
$env:CANAL_PASSWORD = "你的 Canal 密码"
```

然后在同一个终端启动 Maven 服务。也可以在 IntelliJ IDEA 的 Run/Debug Configuration → Environment variables 中填写这些变量。

`RAG_DOCS_PATH` 可选；不设置时，AI 服务默认读取 `src/main/resources/docs`。如果从仓库根目录启动，可设置为 `AiService/src/main/resources/docs`。
