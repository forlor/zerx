# Zerx Spring Web Module Design

## 1. 模块概述

Zerx Spring Web 是面向 Servlet Web 应用的全链路增强框架，提供统一响应封装、全局异常处理、链路追踪、请求日志、限流、幂等性控制、HTTP 客户端拦截器链、CORS 跨域、Jackson 全局序列化配置等开箱即用的 Web 层基础设施。基于 Spring Boot AutoConfiguration 实现零侵入集成，业务代码仅需引入依赖即可获得完整的 Web 增强能力。

### 设计原则

| 原则 | 实现方式 |
|------|----------|
| 零配置开箱即用 | 所有组件均提供合理默认值，引入依赖即自动激活 |
| 按需关闭 | 每个功能模块均有独立 `enabled` 开关，可按需禁用 |
| 高性能设计 | 预编译正则、惰性清理、System.nanoTime 计时、ConcurrentHashMap 无锁读取 |
| 模块解耦 | Web 模块与日志模块通过 `OpLogContextExtractor` SPI 桥接，互不依赖 |
| 敏感数据安全 | 请求日志参数脱敏 + 出站 Header 脱敏，防止凭据泄露到日志 |
| Servlet 限定 | 通过 `@ConditionalOnWebApplication(SERVLET)` 限定激活范围，避免 Reactive 环境冲突 |

---

## 2. 模块结构

```
zerx-spring-web/
├── pom.xml
└── src/main/java/com/zerx/spring/web/
    ├── annotation/
    │   ├── ZerxResponseResult              统一响应封装标记注解（类级别）
    │   ├── RateLimit                       限流注解（方法级别）
    │   └── Idempotent                      幂等性注解（方法级别）
    ├── autoconfigure/
    │   ├── ZerxWebAutoConfiguration        主自动配置类（Filter/Interceptor/Advice/Aspect 注册）
    │   └── RequestContextOpLogContextExtractor  OpLogContextExtractor 桥接实现
    ├── config/
    │   ├── ZerxCorsAutoConfiguration       CORS 跨域过滤器自动配置
    │   ├── JacksonAutoConfiguration        Jackson 全局序列化配置
    │   ├── ZerxHttpClientAutoConfiguration HTTP 客户端 + 拦截器链自动配置
    │   └── ZerxObservabilityAutoConfiguration  Micrometer HTTP 可观测性配置
    ├── context/
    │   └── RequestContext                  ThreadLocal 请求上下文载体
    ├── filter/
    │   ├── TraceFilter                     链路追踪过滤器（TraceID 生成/传播/MDC）
    │   └── AccessLogFilter                 请求访问日志过滤器（慢请求告警/参数脱敏）
    ├── interceptor/
    │   └── RequestContextInterceptor       请求上下文拦截器（客户端 IP 解析）
    ├── advise/
    │   ├── ZerxResponseBodyAdvice          统一响应体封装（→ Result<T>）
    │   └── GlobalExceptionHandler          全局异常处理器（13 种异常类型映射）
    ├── aspect/
    │   ├── RateLimitAspect                 限流切面（滑动窗口 + SpEL key）
    │   └── IdempotentAspect                幂等性切面（ConcurrentHashMap + TTL + SpEL key）
    ├── sensitive/
    │   └── SensitiveDataMasker             敏感数据脱敏器（预编译正则）
    ├── client/interceptor/
    │   ├── RetryInterceptor                出站重试拦截器（指数退避 + 抖动）
    │   ├── ErrorResponseInterceptor        非 2xx 响应异常转换拦截器
    │   ├── OutboundAccessLogInterceptor     出站请求日志拦截器（响应体缓冲/截断）
    │   ├── SensitiveHeaderInterceptor       敏感 Header 脱敏拦截器
    │   └── TracePropagationInterceptor      链路追踪透传拦截器
    └── properties/
        └── ZerxWebProperties               配置属性根类（zerx.web.*）
```

### 依赖关系

```
zerx-spring-web
  ├── spring-boot-starter-web           (Servlet API, Spring MVC)
  ├── spring-boot-starter-aop           (AspectJ, 限流/幂等切面)
  ├── spring-boot-starter-validation    (Jakarta Bean Validation)
  ├── micrometer-observation (optional)  (HTTP 可观测性)
  └── zerx-common                       (Result, ErrorCode, OpLogContextExtractor)
```

被依赖关系：

```
zerx-spring-security  →  zerx-spring-web   (RequestContext, 统一 Result 响应)
zerx-spring-logging   →  zerx-spring-web   (OpLogContextExtractor SPI)
```

---

## 3. 架构设计

### 3.1 请求处理全流程

```
HTTP Request
    │
    ▼
┌──────────────────────────────────────────────────────────────┐
│  Servlet Filter Chain (按 Order 排序)                         │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ 1. zerxCorsFilter           (order = -1)               │  │
│  │    CORS 预检/跨域处理                                   │  │
│  ├────────────────────────────────────────────────────────┤  │
│  │ 2. zerxObservationFilter    (HIGHEST_PRECEDENCE + 1)   │  │
│  │    Micrometer Observation 创建（Span + Metrics）         │  │
│  ├────────────────────────────────────────────────────────┤  │
│  │ 3. zerxTraceFilter          (Integer.MIN_VALUE)         │  │
│  │    ├─ RequestContext.init()                            │  │
│  │    ├─ 生成/提取 TraceID (UUIDv7, X-Trace-Id)           │  │
│  │    ├─ MDC.put("traceId", ...)                          │  │
│  │    └─ response.setHeader("X-Trace-Id", ...)            │  │
│  ├────────────────────────────────────────────────────────┤  │
│  │ 4. zerxAccessLogFilter      (Integer.MIN_VALUE + 2)    │  │
│  │    ├─ 执行 FilterChain（先执行，不阻塞请求）              │  │
│  │    └─ 记录访问日志（后执行，含慢请求告警/参数脱敏）        │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────┬───────────────────────────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────────────────────────┐
│  Spring MVC Interceptor Chain                                │
│                                                              │
│  RequestContextInterceptor (order = MIN_VALUE)               │
│    ├─ 确认 RequestContext 已初始化（TraceFilter 已创建）       │
│    └─ 解析客户端 IP → RequestContext.setRequestIp()          │
│       （X-Forwarded-For → X-Real-IP → RemoteAddr）           │
└──────────────┬───────────────────────────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────────────────────────┐
│  Controller Method                                           │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ AOP Aspect 拦截（Controller 方法前后）                   │  │
│  │                                                        │  │
│  │  @RateLimit 切面                                       │  │
│  │    ├─ 解析 SpEL key                                     │  │
│  │    ├─ 拼接限流 key = "rateLimit:{ip}:{key}"            │  │
│  │    └─ 滑动窗口检查 → 超限抛 BusinessException          │  │
│  │                                                        │  │
│  │  @Idempotent 切面                                      │  │
│  │    ├─ 解析 SpEL key                                     │  │
│  │    ├─ 拼接幂等 key = "idempotent:{userId}:{uri}:{key}" │  │
│  │    ├─ putIfAbsent → 已存在且未过期则抛异常               │  │
│  │    └─ 执行失败时移除 key，允许重试                       │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│  业务 Controller 方法执行                                      │
└──────────────┬───────────────────────────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────────────────────────┐
│  Response Body Advice (ZerxResponseBodyAdvice)               │
│                                                              │
│  if (@RestController || @ZerxResponseResult):                │
│    ├─ 返回值已是 Result<?> → 直接返回                         │
│    ├─ 返回类型 void       → Result.ok()                      │
│    ├─ 返回类型 String     → ObjectMapper 序列化为 JSON       │
│    └─ 其他类型             → Result.ok(data)                  │
│                                                              │
│  排除包：org.springdoc, org.springframework.boot.actuator    │
└──────────────┬───────────────────────────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────────────────────────┐
│  Exception Handler (GlobalExceptionHandler)                  │
│                                                              │
│  13 种异常类型 → Result<Void> + HTTP 状态码                   │
│    ├─ BusinessException         → 400                        │
│    ├─ ValidationException       → 400                        │
│    ├─ AuthorizationException    → 401                        │
│    ├─ NotFoundException         → 404                        │
│    ├─ ExternalServiceException  → 502                        │
│    ├─ MethodArgumentNotValidException → 400                  │
│    ├─ ConstraintViolationException     → 400                  │
│    ├─ HttpMessageNotReadableException   → 400                │
│    ├─ HttpRequestMethodNotSupportedException → 405           │
│    ├─ NoHandlerFoundException          → 404                 │
│    ├─ MaxUploadSizeExceededException   → 400                 │
│    ├─ MultipartException               → 400                 │
│    └─ Exception (兜底)                 → 500                 │
└──────────────┬───────────────────────────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────────────────────────┐
│  TraceFilter finally 块                                      │
│    ├─ MDC.remove("traceId")                                  │
│    └─ RequestContext.clear()                                 │
└──────────────────────────────────────────────────────────────┘
```

### 3.2 出站 HTTP 客户端拦截器链

```
业务代码
    │
    ▼ RestClient.get()/post()...
    │
    ▼
┌──────────────────────────────────────────────────────────┐
│  拦截器链 (按注册顺序执行，第一个最先执行)                    │
│                                                          │
│  1. RetryInterceptor                                     │
│     ├─ 5xx / 429 → 指数退避重试                           │
│     ├─ 网络超时 → 重试                                     │
│     └─ 4xx / ExternalServiceException → 不重试             │
│                                                          │
│  2. ErrorResponseInterceptor                              │
│     ├─ 非 2xx 响应 → ExternalServiceException             │
│     ├─ 超时 → EXTERNAL_SERVICE_TIMEOUT                    │
│     └─ 网络异常 → NETWORK_ERROR                           │
│                                                          │
│  3. OutboundAccessLogInterceptor                          │
│     ├─ 记录请求方法、URL、状态码、耗时                       │
│     ├─ 响应体缓冲（可重复读取）                              │
│     └─ 超大响应体跳过缓冲，防止 OOM                         │
│                                                          │
│  4. SensitiveHeaderInterceptor                            │
│     ├─ authorization / x-token / x-api-key → ******       │
│     └─ Bearer Token → "Bearer ******"                     │
│                                                          │
│  5. TracePropagationInterceptor                           │
│     └─ RequestContext.traceId → X-Trace-Id Header         │
│                                                          │
└──────────────┬───────────────────────────────────────────┘
               │
               ▼
        JDK HttpClient (HTTP/2, 连接池)
```

---

## 4. 核心组件

### 4.1 RequestContext

基于 `ThreadLocal` 的请求级别信息载体，贯穿整个请求处理链路。

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | `Long` | 用户 ID（由 Security 模块设置） |
| `username` | `String` | 用户名（由 Security 模块设置） |
| `tenantId` | `String` | 租户 ID（多租户场景） |
| `traceId` | `String` | 链路追踪 ID（由 TraceFilter 设置） |
| `requestIp` | `String` | 客户端 IP（由 RequestContextInterceptor 设置） |
| `requestId` | `String` | 请求唯一标识（UUIDv7，init 时自动生成） |

**生命周期管理：**

```
请求进入:
  TraceFilter.doFilterInternal()
    → RequestContext.init()      // 创建实例 + 生成 requestId (UUIDv7)

请求处理:
  RequestContextInterceptor.preHandle()
    → 设置 requestIp
  Security 模块
    → 设置 userId, username, tenantId

请求完成:
  TraceFilter.finally
    → RequestContext.clear()     // 移除 ThreadLocal，防止内存泄漏
```

### 4.2 TraceFilter

链路追踪过滤器，负责 TraceID 的生成/传播/MDC 集成。

**TraceID 生成策略：**

| 优先级 | 来源 | 格式 |
|--------|------|------|
| 1 | 请求头 `X-Trace-Id` | 上游服务传递（微服务调用链） |
| 2 | 自动生成 | `UuidUtil.uuidv7Hex()` — 32 位十六进制 UUIDv7 |

**传播路径：**

```
入站:  X-Trace-Id Header → RequestContext.traceId → MDC["traceId"]
出站:  RequestContext.traceId → TracePropagationInterceptor → X-Trace-Id Header
响应:  RequestContext.traceId → response.setHeader("X-Trace-Id", traceId)
日志:  MDC["traceId"] → logback pattern "%X{traceId}"
```

### 4.3 AccessLogFilter

高性能异步访问日志过滤器，支持慢请求告警和敏感参数脱敏。

**日志级别策略：**

| 条件 | 日志级别 | 格式 |
|------|----------|------|
| 耗时 >= `slowThresholdMs` | `WARN` | `[SLOW] GET /api/users -> 200 (3200ms) [10.0.0.1]` |
| 状态码 >= 500 | `ERROR` | `[ERROR] POST /api/orders -> 500 (1200ms) [10.0.0.1]` |
| 状态码 >= 400 | `WARN` | `[CLIENT_ERROR] GET /api/users/999 -> 404 (5ms) [10.0.0.1]` |
| 其他 (debug 模式) | `DEBUG` | `GET /api/users -> 200 (15ms) [10.0.0.1]` |
| 其他 (info 模式) | `INFO` | `GET /api/users -> 200 (15ms) [10.0.0.1]` |

**性能设计：**
- 先执行 FilterChain 后记录日志，不阻塞请求
- 排除路径（`excludeUrls`）在启动时构建为 `Set<String>`，O(1) 查找
- 敏感参数通过 `SensitiveDataMasker` 预编译正则脱敏，零运行时编译开销

### 4.4 SensitiveDataMasker

敏感数据脱敏器，支持 URL Query String 和 JSON 两种格式。

**脱敏规则：**

| 格式 | 正则匹配 | 示例 |
|------|----------|------|
| URL Query String | `(\bpassword=)([^&]*)` | `password=abc123` → `password=******` |
| JSON | `("password"\s*:\s*")([^"]*?)(")` | `"password":"abc123"` → `"password":"******"` |

默认脱敏参数：`password`, `token`, `secret`, `credential`, `authorization`, `phone`, `mobile`

### 4.5 ZerxResponseBodyAdvice

统一响应体封装，将 Controller 返回值自动包装为 `Result<T>`。

**激活条件（需同时满足）：**

| 条件 | 说明 |
|------|------|
| `zerx.web.response-wrap-enabled = true` | 配置开关（默认 `true`） |
| 标注 `@RestController` 或 `@ZerxResponseResult` | 类级别注解 |
| 包名不在排除列表中 | 默认排除 `org.springdoc`, `org.springframework.boot.actuator` |

**包装规则：**

| 返回值类型 | 包装结果 |
|-----------|----------|
| `Result<?>` | 直接返回，不做二次包装 |
| `void` | `Result.ok()` |
| `String` | `ObjectMapper.writeValueAsString(Result.ok(body))`，设置 `Content-Type: application/json` |
| 其他类型 | `Result.ok(data)` |

### 4.6 GlobalExceptionHandler

全局异常处理器，统一 13 种异常类型到 `Result<Void>` + HTTP 状态码的映射。

| 异常类型 | HTTP 状态码 | 错误码来源 |
|----------|------------|-----------|
| `BusinessException` | 400 | `ex.getCode()` |
| `ValidationException` | 400 | `ex.getCode()` |
| `AuthorizationException` | 401 | `ex.getCode()` |
| `NotFoundException` | 404 | `ex.getCode()` |
| `ExternalServiceException` | 502 | `ex.getCode()` |
| `MethodArgumentNotValidException` | 400 | `PARAM_INVALID` + 字段详情 |
| `ConstraintViolationException` | 400 | `PARAM_INVALID` + 违规详情 |
| `HttpMessageNotReadableException` | 400 | `BODY_REQUIRED` |
| `HttpRequestMethodNotSupportedException` | 405 | `PARAM_FORMAT_ERROR` |
| `NoHandlerFoundException` | 404 | `DATA_NOT_FOUND` |
| `MaxUploadSizeExceededException` | 400 | `PARAM_OUT_OF_RANGE` |
| `MultipartException` | 400 | `PARAM_FORMAT_ERROR` |
| `Exception` (兜底) | 500 | `SYSTEM_ERROR` |

### 4.7 RateLimitAspect

基于滑动窗口的内存级限流切面。

**限流 Key 构成：**

```
rateLimit:{clientIp}:{spelKey 或 uri:method:argsHash}
```

**滑动窗口算法：**

```
请求到达:
  ├─ 获取/创建 WindowCounter(key)
  ├─ synchronized(counter):
  │   ├─ 窗口已过期 (now - windowStart >= windowMs)?
  │   │   ├─ YES → 重置窗口: windowStart=now, count=0
  │   │   └─ NO  → 继续
  │   ├─ count < capacity?
  │   │   ├─ YES → count++, 放行
  │   │   └─ NO  → 拒绝, 抛 BusinessException(TOO_MANY_REQUESTS)
  └─ 惰性清理 (每 100 次请求)
```

**SpEL 上下文变量：** 方法参数名（如 `userId`）和索引参数（`p0`, `a0`, `p1`, `a1`）

### 4.8 IdempotentAspect

基于 ConcurrentHashMap 的请求幂等性控制切面。

**幂等 Key 构成：**

```
idempotent:{userId 或 clientIp}:{uri}:{spelKey 或 method:argsHash}
```

**处理逻辑：**

```
请求到达:
  ├─ 构建 key + 计算 expireAt = now + ttl
  ├─ putIfAbsent(key, expireAt):
  │   ├─ 返回 null → 首次请求, 执行方法
  │   ├─ 返回值 <= now → 已过期, 替换并执行
  │   └─ 返回值 > now → 重复请求, 抛 BusinessException(STATE_CONFLICT)
  ├─ 执行成功 → 保留 key (阻止重复)
  ├─ 执行失败 → remove(key), 允许重试
  └─ 惰性清理 (每 100 次请求)
```

**失败重试设计：** 方法执行抛出异常时，自动移除幂等 key，确保客户端可以在修正参数后重试。

### 4.9 HTTP 客户端拦截器链

基于 Spring Boot 3.x `RestClient` + JDK HttpClient (HTTP/2) 构建。

| 拦截器 | 顺序 | 功能 | 可禁用 |
|--------|------|------|--------|
| `RetryInterceptor` | 1 (最外层) | 指数退避重试 + 抖动 | `maxRetries = 0` |
| `ErrorResponseInterceptor` | 2 | 非 2xx → `ExternalServiceException` | `errorHandlingEnabled = false` |
| `OutboundAccessLogInterceptor` | 3 | 请求/响应日志，响应体缓冲+截断 | `accessLogEnabled = false` |
| `SensitiveHeaderInterceptor` | 4 | Authorization 等敏感 Header 脱敏 | `sensitiveHeaderMaskingEnabled = false` |
| `TracePropagationInterceptor` | 5 | `X-Trace-Id` 自动透传 | `tracePropagationEnabled = false` |

**重试策略（RetryInterceptor）：**

| 可重试 | 不可重试 |
|--------|----------|
| HTTP 5xx | HTTP 4xx |
| HTTP 429 | `ExternalServiceException` |
| `ResourceAccessException` (超时) | `InterruptedException` |
| `IOException` (非超时) | — |

**异常映射（ErrorResponseInterceptor）：**

| HTTP 状态码 | ErrorCode | 说明 |
|-------------|-----------|------|
| 429 | `EXTERNAL_SERVICE_RATE_LIMIT` | 外部服务限流 |
| 4xx (非 429) | `EXTERNAL_SERVICE_DATA_ERROR` | 参数/数据异常 |
| 5xx | `EXTERNAL_SERVICE_ERROR` | 外部服务内部错误 |
| 超时 | `EXTERNAL_SERVICE_TIMEOUT` | 连接/读取超时 |
| 其他网络异常 | `NETWORK_ERROR` | 网络 I/O 异常 |

### 4.10 RequestContextOpLogContextExtractor

连接 Web 模块与日志模块的桥梁，实现 `OpLogContextExtractor` SPI 接口。

```
zerx-spring-logging (ZerxOpLogAspect)
    │  调用 OpLogContextExtractor.extract()
    ▼
RequestContextOpLogContextExtractor
    │  从 RequestContext 读取
    ▼
OpLogContext(userId, username, requestIp)
```

当 `RequestContext` 未初始化时（如非 Web 请求、异步线程），返回 `null`，日志模块安全降级。

---

## 5. 自动配置

### 5.1 激活条件

| 条件 | 注解 |
|------|------|
| Servlet Web 应用 | `@ConditionalOnWebApplication(type = SERVLET)` |

### 5.2 自动配置注册

`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册 4 个自动配置类：

```
com.zerx.spring.web.autoconfigure.ZerxWebAutoConfiguration
com.zerx.spring.web.config.JacksonAutoConfiguration
com.zerx.spring.web.config.ZerxObservabilityAutoConfiguration
com.zerx.spring.web.config.ZerxHttpClientAutoConfiguration
```

### 5.3 Bean 注册清单

| Bean | 条件 | 配置开关 |
|------|------|----------|
| `ZerxResponseBodyAdvice` | 始终注册 | `zerx.web.response-wrap-enabled` (运行时判断) |
| `TraceFilter` | 始终注册 | — |
| `AccessLogFilter` | `zerx.web.access-log.enabled=true` (默认 `true`) | `@ConditionalOnProperty` |
| `RequestContextInterceptor` | 始终注册 (WebMvcConfigurer) | — |
| `OpLogContextExtractor` | 无同名 Bean 时注册 | `@ConditionalOnMissingBean` |
| `RateLimitAspect` | `zerx.web.rate-limit.enabled=true` (默认 `true`) | `@ConditionalOnProperty` |
| `IdempotentAspect` | `zerx.web.idempotent.enabled=true` (默认 `true`) | `@ConditionalOnProperty` |
| `CorsFilter` | `zerx.web.cors.enabled=true` (默认 `true`) | `@ConditionalOnProperty` |
| `ObjectMapper` | 无同名 Bean 时注册 | `@ConditionalOnMissingBean` |
| `RestClient` | `zerx.web.http-client.enabled=true` (默认 `true`) + 无同名 Bean | `@ConditionalOnProperty` + `@ConditionalOnMissingBean` |
| `ClientHttpRequestFactory` | 同上 + 无同名 Bean | `@ConditionalOnMissingBean` |
| HTTP Observation Filter | `ObservationRegistry` 在 classpath + `zerx.web.observability.enabled=true` | `@ConditionalOnClass` + `@ConditionalOnProperty` |

---

## 6. 使用示例

### 6.1 最小配置

```yaml
# application.yml — 通常无需任何配置，所有组件开箱即用
# 仅在需要自定义时添加以下配置：
zerx:
  web:
    response-wrap-enabled: true
```

### 6.2 完整配置示例

```yaml
zerx:
  web:
    # 统一响应封装
    response-wrap-enabled: true
    response-wrap-exclude-packages:
      - org.springdoc
      - org.springframework.boot.actuator

    # Jackson 序列化
    jackson:
      date-format: "yyyy-MM-dd HH:mm:ss"
      include-null: false

    # CORS 跨域
    cors:
      enabled: true
      allowed-origins: "*"
      allowed-methods: GET,POST,PUT,DELETE,PATCH,OPTIONS
      allowed-headers: "*"
      allow-credentials: false
      max-age: 3600

    # 访问日志
    access-log:
      enabled: true
      slow-threshold-ms: 3000
      exclude-urls:
        - /actuator/**
        - /doc.html
        - /swagger-ui/**
        - /v3/api-docs/**
      sensitive-params:
        - password
        - token
        - secret

    # HTTP 客户端
    http-client:
      enabled: true
      connect-timeout: 5
      read-timeout: 30
      max-connections: 100
      max-connections-per-route: 20
      max-retries: 2
      retry-initial-delay-ms: 100
      retry-max-delay-ms: 3000
      retry-jitter-enabled: true
      access-log-enabled: true
      max-response-body-log-length: 1024
      error-handling-enabled: true
      trace-propagation-enabled: true
      sensitive-header-masking-enabled: true

    # 限流
    rate-limit:
      enabled: true

    # 幂等性
    idempotent:
      enabled: true

    # 可观测性
    observability:
      enabled: true
```

### 6.3 统一响应封装

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        // 返回值自动包装为 Result<User>
        return userService.findById(id);
    }

    @PostMapping
    public User createUser(@Valid @RequestBody CreateUserRequest request) {
        // 校验失败时自动返回 Result<Void> + 400
        return userService.create(request);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        // void 返回值自动包装为 Result<Void>.ok()
        userService.delete(id);
    }
}
```

### 6.4 限流注解

```java
@RestController
@RequestMapping("/api/sms")
public class SmsController {

    // 默认：每秒最多 100 次请求（按 IP 限流）
    @RateLimit
    @PostMapping("/send")
    public Result<Void> sendCode(@RequestParam String phone) {
        smsService.sendCode(phone);
        return Result.ok();
    }

    // 自定义：按用户维度限流，每 60 秒最多 5 次
    @RateLimit(key = "#userId", capacity = 5, windowSeconds = 60)
    @PostMapping("/send")
    public Result<Void> sendCode(@RequestParam Long userId,
                                  @RequestParam String phone) {
        smsService.sendCode(phone);
        return Result.ok();
    }
}
```

### 6.5 幂等性注解

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    // 默认：5 秒内相同请求只执行一次
    @Idempotent
    @PostMapping
    public Result<Order> createOrder(@RequestBody CreateOrderRequest request) {
        return Result.success(orderService.create(request));
    }

    // 自定义：使用订单号作为幂等 key，10 秒窗口
    @Idempotent(key = "#request.orderNo", ttl = 10, timeUnit = TimeUnit.SECONDS)
    @PostMapping("/by-no")
    public Result<Order> createOrder(@RequestBody CreateOrderRequest request) {
        return Result.success(orderService.create(request));
    }
}
```

### 6.6 业务代码中使用 RequestContext

```java
@Service
public class OrderService {

    public Order create(CreateOrderRequest request) {
        Long userId = RequestContext.getUserId();
        String traceId = RequestContext.getTraceId();
        String clientIp = RequestContext.getRequestIp();

        log.info("用户 {} 发起下单请求, traceId={}, ip={}", userId, traceId, clientIp);
        // ...
    }
}
```

---

## 7. 设计决策

| 决策 | 理由 |
|------|------|
| `@ConditionalOnWebApplication(SERVLET)` | 明确限定 Servlet 环境，避免在 Reactive (WebFlux) 应用中加载导致类冲突 |
| ThreadLocal RequestContext + Filter 清理 | 比 `RequestContextHolder` 更轻量，且在 Filter 的 finally 块中确保清理，无内存泄漏风险 |
| UUIDv7 作为 TraceID/RequestID | 时间有序，天然支持按时间排序的日志检索，且在分布式环境中几乎无碰撞 |
| TraceFilter order = MIN_VALUE | 确保 TraceID 在所有其他 Filter 之前生成，后续 Filter 和 Interceptor 均可使用 |
| `@ConditionalOnMissingBean(OpLogContextExtractor.class)` | 允许业务层提供自定义实现（如从消息队列上下文提取），不强制依赖 Web 模块的实现 |
| 滑动窗口 + synchronized(WindowCounter) | 按 key 粒度加锁，不同 key 之间完全并发；单 key 内同步保证计数准确 |
| 惰性清理（每 100 次请求） | 避免后台定时任务增加复杂度；在请求路径上顺便清理，过期条目对正确性无影响 |
| 幂等失败时移除 key | 确保业务失败（如参数错误）不会"消费"幂等窗口，客户端修正后可重试 |
| JDK HttpClient (HTTP/2) 作为出站底层 | 零额外依赖，JDK 内置连接池管理，HTTP/2 多路复用提升吞吐 |
| RestClient 拦截器链模式 | 职责单一，每个拦截器可独立启用/禁用；顺序可配置，易于扩展 |
| `SensitiveHeaderInterceptor` 修改请求 Header | 在拦截器链中顺序靠前（第 4 个），日志拦截器（第 3 个）之后不会再次读取，确保日志中看到的是脱敏后的值 |
| Long → String (Jackson) | JavaScript `Number` 最大安全整数为 2^53-1，Java `Long` 可能溢出，序列化为 String 避免 JS 精度丢失 |
| `NON_NULL` (Jackson 默认) | 减少无效 JSON 传输，降低网络带宽占用 |
| `@ConditionalOnMissingBean(ObjectMapper.class)` | 业务层可提供自定义 ObjectMapper（如注册自定义 Serializer），框架不强制覆盖 |
