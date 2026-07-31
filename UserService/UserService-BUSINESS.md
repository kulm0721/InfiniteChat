# UserService 业务逻辑说明

> 用户中心微服务：负责用户的注册、登录（密码/验证码）、登出、Token 签发与刷新等认证业务。

## 一、服务概述

| 项目 | 说明 |
| --- | --- |
| 服务名 | `UserService` |
| 端口 | `8104` |
| 技术栈 | Spring Boot 3.5、MyBatis-Plus、MySQL、Redis、JWT（jjwt）、JavaMailSender |
| 数据库 | MySQL（库：`InitProject`），用户表 `user` |
| 缓存 | Redis（db: 2），用于存储验证码与 Token |

## 二、模块结构

```
UserService
└── src/main/java/com/shanyangcode/userservice
    ├── controller/      # 接口层（UserController）
    ├── service/         # 业务接口（UserService）+ 实现（UserServiceImpl）
    ├── mapper/          # MyBatis-Plus Mapper（UserMapper）
    ├── model/
    │   ├── entity/      # 实体（User）
    │   ├── dto/         # 入参（注册/密码登录/验证码登录请求）
    │   └── vo/          # 出参（登录注册响应 / Token 响应）
    ├── constant/        # 业务常量（UserConstant）
    ├── utils/           # 工具类（EmailUtil / RandomCodeUtil）
    ├── handler/         # 拦截器（JwtHandler）
    ├── config/          # 配置（WebConfig：拦截器 + CORS）
    ├── aop/             # 请求日志切面（LogInterceptor）
    ├── common/          # 统一响应（BaseResponse / ResultUtils / ErrorCode）
    └── exception/       # 异常体系（BusinessException / GlobalExceptionHandler / ThrowUtils）
```

## 三、接口一览

统一前缀：`/api/user`

| 方法 | 路径 | 说明 | 是否需要登录 |
| --- | --- | --- | --- |
| GET | `/api/user/sendCaptcha` | 发送邮箱验证码 | 否 |
| POST | `/api/user/register` | 邮箱验证码注册 | 否 |
| POST | `/api/user/login/password` | 密码登录 | 否 |
| POST | `/api/user/login/code` | 验证码登录 | 否 |
| GET | `/api/user/logout` | 登出 | 是 |
| POST | `/api/user/refresh` | 刷新 Token | 否 |

## 四、核心业务流程

### 1. 发送邮箱验证码（sendCaptcha）

**入参**：`targetEmail`（邮箱，非空 + 邮箱格式校验）

**处理流程**：
1. 从 Redis 中按 `targetEmail` 作为 key 查询是否已有验证码。
2. 若验证码已存在（未过期），抛出 `LOGIN_SEND_CODE_ERROR(70007, 验证码已发送，请稍后重试)`，防止频繁发送。
3. 通过 `RandomCodeUtil.getRandomCode()` 生成 6 位随机数字验证码。
4. 调用 `EmailUtil.sendEmail()` 发送邮件，标题为「【验证码】」，正文提示"五分钟内有效"。
5. 将验证码写入 Redis，key 为邮箱地址，TTL 为 `CAPTCHA_EXPIRE_TIME = 5` 分钟。

**返回**：`BaseResponse<String>`，成功提示「发送邮件成功」。

### 2. 注册（register）

**入参**（UserRegisterRequest，均带校验）：
- `email`：邮箱（非空、格式正确）
- `password`：密码（非空，6-20 位）
- `confirmPassword`：确认密码（非空）
- `code`：验证码（非空，6 位数字）
- `nickname`：昵称（可选，最长 50 字符）

**处理流程**：
1. **校验验证码**：用 `email` 从 Redis 取验证码，若为空或与传入 `code` 不一致，抛 `LOGIN_ERROR_CODE(70004, 验证码错误/失效)`。
2. **校验账号唯一性**：按 `email` 查库，若用户已存在，抛 `USER_ALREADY_EXISTS(70001, 用户已存在)`。
3. **校验两次密码一致**：不一致抛 `LOGIN_PASSWORD_ERROR(70006, 密码不一致)`。
4. **密码加密**：`MD5(PASSWORD_SALT("goat") + password)`，不存明文。
5. **创建用户**：在 `synchronized (email.intern())` 临界区中，用雪花算法（worker=1, datacenter=1）生成 `userId`，写入 `user` 表；保存失败抛 `SYSTEM_ERROR(50000)`。
6. **清理验证码**：注册成功后删除 Redis 中的验证码。
7. **签发 Token**：调用 `createJwt()` 生成 accessToken + refreshToken 并写入响应。

**返回**：`LoginAndRegisterResponse`（含用户信息 + 双 Token）。

### 3. 密码登录（login/password）

**入参**：`email`、`password`（校验规则同注册）。

**处理流程**：
1. 按 `email` 查询用户，不存在抛 `USER_NOT_EXISTS(70002, 用户不存在)`。
2. 将传入密码 MD5 加密后与库中密码比对，不一致抛 `LOGIN_ERROR(70005, 登录失败, 用户名或密码错误)`。
3. 校验通过后，拷贝用户信息并调用 `createJwt()` 签发 Token。

**返回**：`LoginAndRegisterResponse`。

### 4. 验证码登录（login/code）

**入参**：`email`、`code`（6 位数字）。

**处理流程**：
1. 从 Redis 取验证码校验，失败抛 `LOGIN_ERROR_CODE(70004)`。
2. 校验通过后**立即删除** Redis 中的验证码（一次性使用）。
3. 按 `email` 查用户，不存在抛 `USER_NOT_EXISTS(70002)`。
4. 调用 `createJwt()` 签发 Token。

**返回**：`LoginAndRegisterResponse`。

### 5. 登出（logout）

**入参**：请求头 `Access-Token`。

**处理流程**：
1. 从请求头取出 `Access-Token`，`JwtUtil.parse()` 解析，解析失败抛 `NOT_LOGIN_ERROR(40100)`。
2. 从 Claims 中取 `subject`（userId）。
3. 删除 Redis 中该用户的 `access:token:{userId}` 与 `refresh:token:{userId}` 两条记录，实现"踢下线"。

**返回**：`BaseResponse<Boolean>`，恒为 `true`。

### 6. 刷新 Token（refresh）

**入参**：请求头 `Refresh-Token`（非空校验，否则抛 `PARAMS_ERROR(40000)`）。

**处理流程**：
1. 解析 `Refresh-Token`，无效抛 `TOKEN_INVALID(40101, 凭证已失效，请重新登录)`。
2. 从 Claims 取 `userId`。
3. **Redis 兜底校验**：对比 Redis 中 `refresh:token:{userId}` 与传入值，不一致抛 `TOKEN_INVALID(40101, 凭证已过期或在其他地方登录)`。这是实现**单设备登录**（Token 撤销）的关键，一旦在新设备登录，旧设备 Refresh Token 即失效。
4. 生成一对新 Token（accessToken + refreshToken）。
5. 更新 Redis 中的两对 Token。

**返回**：`TokenResponse{ accessToken, refreshToken }`。

## 五、Token 体系与登录拦截

### Token 生成（createJwt）
- `accessToken`：有效期 **30 分钟**（`ACCESS_TOKEN_EXPIRE_TIME=30`，`TimeUnit.MINUTES`）。
- `refreshToken`：有效期 **7 天**（`REFRESH_TOKEN_EXPIRE_TIME=7`，`TimeUnit.DAYS`）。
- 均使用 `JwtUtil.generate(userId, timeout, unit)`，HS256 签名，密钥来自 `CommonConstant.TOKEN_SECRET_KEY`。
- 签发后同时写入 Redis：`access:token:{userId}` / `refresh:token:{userId}`，TTL 与各自有效期一致。

### JWT 解析（Common 模块 JwtUtil）
- 空 Token 直接返回 null。
- 区分过期（ExpiredJwtException）、签名/格式错误、未知异常，统一返回 null 并记录日志。

### 登录拦截（JwtHandler + WebConfig）
- 拦截所有路径 `/**`，排除：`/api/user/sendCaptcha`、`/api/user/register`、`/api/user/login/code`、`/api/user/login/password`、`/api/user/refresh`。
- 校验逻辑（`preHandle`）：
  1. 有 `Access-Token` 且解析成功，且与 Redis 中的 `access:token:{userId}` 一致 → 放行。
  2. 否则若有 `Refresh-Token` 且有效且与 Redis 一致 → 抛 `TOKEN_EXPIRED(40103, Access Token 过期)`，**提示前端用 Refresh-Token 换新**。
  3. 以上均不满足 → 抛 `NOT_LOGIN_ERROR(40100, 未登录)`。
- CORS：允许所有来源、GET/POST/PUT/DELETE/OPTIONS、允许携带凭证。

## 六、Redis Key 设计

| Key | Value | TTL | 用途 |
| --- | --- | --- | --- |
| `{email}` | 6 位验证码 | 5 分钟 | 邮箱验证码（防重发 + 注册/登录校验） |
| `access:token:{userId}` | accessToken | 30 分钟 | 登录态校验 / 单设备登录 |
| `refresh:token:{userId}` | refreshToken | 7 天 | 刷新校验 / 单设备登录 |

## 七、错误码一览（UserService）

| code | message | 触发场景 |
| --- | --- | --- |
| 40000 | 请求参数错误 | 参数校验失败 |
| 40100 | 未登录 | Token 缺失或解析失败 |
| 40101 | 无效的身份认证 | Refresh Token 无效/撤销 |
| 40103 | Access Token 过期 | Access 过期但 Refresh 有效 |
| 50000 | 系统内部异常 | 保存用户失败等 |
| 70001 | 用户已存在 | 注册时邮箱重复 |
| 70002 | 用户不存在 | 登录时查无此人 |
| 70004 | 验证码错误/失效 | 注册/验证码登录校验失败 |
| 70005 | 登录失败, 用户名或密码错误 | 密码错误 |
| 70006 | 密码不一致 | 两次密码不同 |
| 70007 | 验证码已发送，请稍后重试 | 验证码未过期重复发送 |

## 八、其他

- **请求日志**：`LogInterceptor`（AOP 切面）对 `controller` 包所有方法记录请求 id、URL、IP、参数与耗时。
- **统一响应**：`ResultUtils.success()/error()` 包装为 `BaseResponse`；`GlobalExceptionHandler` 统一处理 `BusinessException`、`RuntimeException`、参数校验异常（`MethodArgumentNotValidException` / `ConstraintViolationException`）。
- **逻辑删除**：MyBatis-Plus 配置 `isDelete` 字段逻辑删除（0 正常 / 1 已删）。
