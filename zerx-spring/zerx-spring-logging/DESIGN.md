# Zerx Spring Logging 模块设计文档

## 1. 模块概述

Zerx Spring Logging 是一套声明式操作日志与日志增强框架，提供注解驱动的操作审计、Logback 日志脱敏、日志限流以及零依赖的 JSON 结构化日志输出能力。模块设计遵循零侵入、声明式、可观测性三大原则，可独立于 Web 模块运行，也可与 zerx-spring-web 无缝集成获取完整的用户上下文。

### 设计原则

| 原则 | 实现方式 |
|------|----------|
| 零侵入 | 业务代码仅需添加 `@ZerxOpLog` 注解，无需修改业务逻辑；Logback 脱敏通过 Encoder 包装器实现，无需修改 pattern |
| 声明式 | 通过注解 + AOP 切面自动记录操作日志，支持 SpEL 动态描述、敏感参数配置 |
| 可观测性 | 操作日志涵盖链路追踪 ID、用户信息、耗时、异常等 16 个字段；JSON 格式输出便于 ELK/Loki 采集 |
| 可选依赖 | Logback 声明为 `optional`，非 Logback 环境下仅操作日志切面可用；SPI 持久化接口可选实现 |
| 与 Web 解耦 | 操作上下文通过 `OpLogContextExtractor` 函数式接口提取，不依赖任何特定上下文载体（HttpServletRequest / RequestContext） |

---

## 2. 模块结构

```
zerx-spring-logging/
├── pom.xml
└── src/main/java/com/zerx/spring/logging/
    ├── annotation/
    │   └── ZerxOpLog                    @ZerxOpLog 注解（声明式操作日志）
    ├── aspect/
    │   └── ZerxOpLogAspect              AOP 切面（拦截、上下文提取、SpEL 解析、限流）
    ├── event/
    │   └── ZerxOpLogEvent               操作日志领域事件（16 字段 record）
    ├── service/
    │   └── ZerxOpLogService             SPI 持久化接口
    ├── properties/
    │   └── ZerxLoggingProperties        配置属性（OpLog / SensitiveLog / RateLimit / JsonFormat）
    ├── converter/
    │   └── SensitiveMessageConverter     Logback %zerxMsg 转换器
    ├── appender/
    │   ├── ZerxSensitiveLogEncoder       Logback Encoder 包装器（脱敏过滤）
    │   └── ZerxJsonEncoder               零依赖 JSON 结构化日志 Encoder
    └── autoconfigure/
        ├── ZerxLoggingAutoConfiguration  自动配置（Bean 注册 + 条件装配）
        └── ZerxJsonEncoderInitializer    SmartLifecycle（自动替换 Console Encoder）
```

### 依赖关系

```
zerx-spring-logging
  ├── zerx-common                        (OpLogContextExtractor, SensitiveLogFilter, LogRateLimiter)
  ├── spring-boot-starter-aop            (操作日志切面)
  ├── spring-expression                  (SpEL 表达式解析)
  ├── jackson-databind                   (参数/返回值 JSON 序列化)
  └── logback-classic (optional)         (脱敏 Encoder / JSON Encoder / %zerxMsg)
```

模块不依赖 `zerx-spring-web`。当 Web 模块存在时，由 Web 模块自动注册 `OpLogContextExtractor` 实现，从 `RequestContext` 中提取用户上下文。

---

## 3. 架构设计

### 3.1 操作日志记录流程

```
Controller 方法调用（标注 @ZerxOpLog）
    │
    ▼
┌──────────────────────────────────────────────────┐
│  ZerxOpLogAspect.around()                         │
│                                                   │
│  1. 检查开关：enabled + opLog.enabled              │
│  2. 提取方法签名、参数名、参数值                     │
│  3. OpLogContextExtractor.extract()                │
│     → userId / username / clientIp                │
│  4. resolveDescription() → SpEL 动态解析           │
│  5. joinPoint.proceed() → 执行目标方法              │
│                                                   │
│  ┌─ 成功 ─────────────────────────────────────┐   │
│  │ 构建 ZerxOpLogEvent.success(...)           │   │
│  │ recordParams → 敏感参数脱敏 → 截断           │   │
│  │ recordResult → JSON 序列化 → 截断            │   │
│  └────────────────────────────────────────────┘   │
│  ┌─ 失败 ─────────────────────────────────────┐   │
│  │ 构建 ZerxOpLogEvent.failure(...)           │   │
│  │ 异常信息记录                                │   │
│  │ 原始异常重新抛出                             │   │
│  └────────────────────────────────────────────┘   │
│                                                   │
│  6. publishEvent(event)                           │
│     ├─ rateLimiter.tryAcquire(key) → 限流检查     │
│     ├─ opLogService.save(event)   → SPI 持久化    │
│     └─ logToLogger(event)        → SLF4J 输出     │
└──────────────────────────────────────────────────┘
```

### 3.2 日志脱敏流程

```
Logback 日志事件 (ILoggingEvent)
    │
    ├─ 方式一：SensitiveMessageConverter
    │   pattern 中使用 %zerxMsg 代替 %msg
    │   → super.convert() 获取原始消息
    │   → SensitiveLogFilter.filter() 脱敏
    │
    └─ 方式二：ZerxSensitiveLogEncoder
        包裹任意 Encoder（如 PatternLayoutEncoder）
        → delegate.encode() 获取原始字节
        → new String(bytes) → SensitiveLogFilter.filter()
        → 返回脱敏后的字节
```

### 3.3 JSON 结构化日志流程

```
应用启动
    │
    ▼
┌──────────────────────────────────────────┐
│  ZerxJsonEncoderInitializer (SmartLifecycle)│
│                                           │
│  条件：json-format.enabled = true          │
│  阶段：Integer.MAX_VALUE（最后执行）        │
│                                           │
│  1. 创建 ZerxJsonEncoder                   │
│     ├─ includeMdc                          │
│     ├─ includeLoggerName                   │
│     ├─ includeThreadName                   │
│     └─ prettyPrint                         │
│  2. 遍历 rootLogger 的 Appender            │
│  3. 找到 ConsoleAppender                   │
│  4. 停止旧 Encoder → 替换为 ZerxJsonEncoder│
└──────────────────────────────────────────┘
    │
    ▼
日志输出 → ZerxJsonEncoder.encode()
    → timestamp / level / logger / thread / message / mdc / exception
    → 纯 StringBuilder 拼接（零依赖）
    → 每条日志一行 JSON
```

---

## 4. 核心组件

### 4.1 @ZerxOpLog 注解

标注在 Controller 方法上的声明式操作日志注解，AOP 切面自动拦截并发布 `ZerxOpLogEvent`。

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `value` | String | —（必填） | 操作描述，支持 SpEL 表达式 |
| `type` | Type | `OTHER` | 操作类型 |
| `module` | String | `""` | 操作模块 |
| `recordParams` | boolean | `false` | 是否记录入参 |
| `recordResult` | boolean | `false` | 是否记录返回值 |
| `sensitiveParams` | String[] | `{}` | 需要脱敏的参数名（精确匹配） |

**操作类型枚举（9 种）：**

| 枚举值 | 说明 |
|--------|------|
| `LOGIN` | 登录 |
| `LOGOUT` | 登出 |
| `CREATE` | 新增 |
| `UPDATE` | 修改 |
| `DELETE` | 删除 |
| `EXPORT` | 导出 |
| `IMPORT` | 导入 |
| `QUERY` | 查询 |
| `OTHER` | 其他 |

**SpEL 表达式支持：**

- `#参数名` — 引用方法参数（如 `#req.username`）
- `#p0`, `#p1` — 按位置引用方法参数
- `T(类名)` — 静态方法/常量引用
- 纯静态文本（不含 `#` 和 `T(`）直接返回，不触发 SpEL 解析

### 4.2 ZerxOpLogAspect

AOP 环绕切面，负责操作日志的完整生命周期处理。

| 职责 | 实现细节 |
|------|----------|
| 开关检查 | `properties.isEnabled()` + `properties.getOpLog().isEnabled()` 双重检查 |
| 上下文提取 | 通过 `OpLogContextExtractor.extract()` 获取 userId / username / clientIp |
| SpEL 解析 | `SpelExpressionParser` + `StandardEvaluationContext`，失败时回退到原始字符串 |
| 参数序列化 | `ObjectMapper.writeValueAsString()` + 长度截断（可配置 `maxParamLength`） |
| 敏感参数脱敏 | `sensitiveParams` 匹配的参数值替换为 `"******"` |
| 限流保护 | `LogRateLimiter.tryAcquire(className.methodName)` 按「类名.方法名」维度限流 |
| SPI 持久化 | `ZerxOpLogService.save(event)`，异常仅记录日志不影响主流程 |
| 日志输出 | 受 `logToLogger` 控制；成功 `INFO`，失败 `WARN`；可选 `recordExceptionStackTrace` |

### 4.3 ZerxOpLogEvent

操作日志领域事件，Java `record` 类型，包含 16 个字段。

| 字段 | 类型 | 说明 |
|------|------|------|
| `traceId` | String | 链路追踪 ID（来自 MDC） |
| `userId` | Long | 操作用户 ID |
| `username` | String | 操作用户名 |
| `module` | String | 操作模块 |
| `type` | ZerxOpLog.Type | 操作类型 |
| `description` | String | 操作描述（SpEL 解析后） |
| `className` | String | 目标类名 |
| `methodName` | String | 目标方法名 |
| `paramNames` | String[] | 参数名数组 |
| `paramValues` | Object[] | 参数值数组 |
| `result` | Object | 返回值（失败时为 null） |
| `durationMs` | long | 执行耗时（毫秒） |
| `exception` | Throwable | 异常信息（成功时为 null） |
| `clientIp` | String | 客户端 IP |
| `timestamp` | Instant | 事件时间戳 |
| `extra` | Map<String, Object> | 扩展信息（含截断后的 params / result） |

**工厂方法：**

```java
ZerxOpLogEvent.success(traceId, userId, username, module, type, description,
    className, methodName, paramNames, paramValues, result, durationMs, clientIp, extra);

ZerxOpLogEvent.failure(traceId, userId, username, module, type, description,
    className, methodName, paramNames, paramValues, exception, durationMs, clientIp, extra);
```

### 4.4 ZerxOpLogService

操作日志持久化 SPI 接口，业务层实现后注册为 Spring Bean 即可被切面自动注入。

```java
public interface ZerxOpLogService {
    void save(ZerxOpLogEvent event);
}
```

**设计要点：**
- 接口为空实现可选（未注册时操作日志仅输出到 SLF4J）
- 建议使用 `@Async` 异步执行，避免影响主线程性能
- 持久化异常被切面捕获并记录错误日志，不影响业务主流程

### 4.5 SensitiveMessageConverter

Logback `MessageConverter` 子类，在 `<pattern>` 中使用 `%zerxMsg` 代替 `%msg` 实现日志消息自动脱敏。

```
conversionRule: conversionWord="zerxMsg"
                converterClass="SensitiveMessageConverter"

pattern: %d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %zerxMsg%n
```

内部委托 `SensitiveLogFilter.filter()` 进行正则匹配脱敏。

### 4.6 ZerxSensitiveLogEncoder

Logback `EncoderBase` 包装器，对任意 Encoder 的输出字节进行脱敏过滤。

```xml
<encoder class="com.zerx.spring.logging.appender.ZerxSensitiveLogEncoder">
    <delegate class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
        <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </delegate>
</encoder>
```

**设计优势：** 无需修改现有 pattern 配置，通过包装器模式实现无侵入脱敏。如果过滤前后字符串相同（无需脱敏），直接返回原始字节数组避免不必要的编码开销。

### 4.7 ZerxJsonEncoder

零外部依赖的 JSON 结构化日志 Encoder，使用纯 `StringBuilder` 拼接实现。

**输出字段：**

| 字段 | 说明 | 是否可选 |
|------|------|----------|
| `timestamp` | ISO 格式时间戳 | 必须 |
| `level` | 日志级别 | 必须 |
| `logger` | Logger 名称 | 可配置 |
| `thread` | 线程名 | 可配置 |
| `message` | 日志消息 | 必须 |
| `mdc` | MDC 上下文（含 traceId 等） | 可配置 |
| `exception` | 异常消息 | 仅异常时输出 |
| `exceptionClass` | 异常类名 | 仅异常时输出 |

**特性：**
- 零依赖（不依赖 Jackson / Gson），纯 JDK 实现
- 内置 JSON 转义（`"` `\\` `\n` `\r` `\t`）
- 支持嵌套 Map 序列化
- 每条日志一行（追加 `\n`）

### 4.8 ZerxJsonEncoderInitializer

`SmartLifecycle` 实现，在 Spring 容器启动后自动替换 Logback Console Appender 的 Encoder。

| 配置 | 说明 |
|------|------|
| `phase` | `Integer.MAX_VALUE`（在所有其他 Lifecycle 之后执行） |
| `autoStartup` | `true` |
| 条件 | `zerx.logging.json-format.enabled = true` + Logback 在 classpath |

---

## 5. 与 Web 模块解耦

### 5.1 OpLogContextExtractor 设计

`OpLogContextExtractor` 定义在 `zerx-common` 模块中，是一个 `@FunctionalInterface`，不依赖任何 Web 容器或 Spring 上下文。

```java
@FunctionalInterface
public interface OpLogContextExtractor {
    OpLogContext extract();

    record OpLogContext(Long userId, String username, String clientIp) {}
}
```

### 5.2 解耦机制

```
┌─────────────────────────────────────────────────────┐
│  zerx-spring-logging（日志模块）                      │
│                                                     │
│  ZerxOpLogAspect                                     │
│    └─ ObjectProvider<OpLogContextExtractor>           │
│       └─ contextExtractor.extract()                  │
│          → 可能为 null（非 Web 环境正常工作）            │
└─────────────────────────────────────────────────────┘
                    ▲
                    │ 注入
                    │
┌───────────────────┴─────────────────────────────────┐
│  zerx-spring-web（Web 模块）                          │
│                                                     │
│  自动注册 OpLogContextExtractor Bean                 │
│    └─ 实现：从 RequestContext 提取                    │
│       ├─ userId → RequestContext.getUserId()         │
│       ├─ username → RequestContext.getUsername()      │
│       └─ clientIp → RequestContext.getClientIp()     │
└─────────────────────────────────────────────────────┘
```

| 场景 | OpLogContextExtractor | userId / username / clientIp |
|------|----------------------|------------------------------|
| Web 环境 + zerx-spring-web | Web 模块自动注册 | 从 RequestContext 提取 |
| 非 Web 环境（定时任务 / MQ） | 不注册 | 均为 `null` |
| 自定义环境 | 业务自行注册 Bean | 从 SecurityContext / TenantContext 等提取 |

**关键设计：** `ZerxOpLogAspect` 使用 `ObjectProvider.getIfAvailable()` 注入，当容器中无 `OpLogContextExtractor` Bean 时不会启动失败，操作日志事件中 `userId` / `username` / `clientIp` 字段为 `null`。

---

## 6. 自动配置

### 6.1 激活条件

| 条件 | 注解 |
|------|------|
| `zerx.logging.enabled` 为 `true` | `@ConditionalOnProperty(matchIfMissing = true)` |

### 6.2 注册的 Bean

| Bean | 条件 | 说明 |
|------|------|------|
| `ZerxOpLogAspect` | 默认注册 | 操作日志 AOP 切面 |
| `LogRateLimiter` | `rate-limit.enabled = true` | 日志限流器 |
| `SensitiveMessageConverter` | Logback 在 classpath | Logback `%zerxMsg` 转换器 |
| `ZerxJsonEncoderInitializer` | `json-format.enabled = true` + Logback | JSON Encoder 自动替换器 |

### 6.3 可选依赖注入

`ZerxOpLogAspect` 通过 `ObjectProvider` 注入以下可选依赖：

| 依赖 | 来源 | 未提供时的行为 |
|------|------|----------------|
| `ZerxOpLogService` | 业务层实现 | 操作日志仅输出到 SLF4J，不持久化 |
| `LogRateLimiter` | 自动配置或业务覆盖 | 不限流，所有日志均通过 |
| `OpLogContextExtractor` | Web 模块或业务层 | userId/username/clientIp 为 null |

### 6.4 注册文件

`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`：

```
com.zerx.spring.logging.autoconfigure.ZerxLoggingAutoConfiguration
```

---

## 7. 配置属性

配置根路径：`zerx.logging`

### 7.1 ZerxLoggingProperties

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | boolean | `true` | 模块总开关 |

### 7.2 OpLog 子配置（`zerx.logging.op-log`）

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | boolean | `true` | 操作日志开关 |
| `log-to-logger` | boolean | `true` | 是否同时输出到 SLF4J |
| `record-exception-stack-trace` | boolean | `false` | 是否记录异常堆栈 |
| `max-param-length` | int | `1024` | 参数序列化最大长度 |
| `max-result-length` | int | `2048` | 返回值序列化最大长度 |

### 7.3 SensitiveLog 子配置（`zerx.logging.sensitive-log`）

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | boolean | `false` | 日志脱敏开关 |
| `filter-mobile` | boolean | `true` | 脱敏手机号 |
| `filter-email` | boolean | `true` | 脱敏邮箱 |
| `filter-id-card` | boolean | `true` | 脱敏身份证号 |
| `filter-bank-card` | boolean | `true` | 脱敏银行卡号 |
| `filter-password` | boolean | `true` | 脱敏密码 |
| `filter-ipv4` | boolean | `false` | 脱敏 IPv4 地址 |

### 7.4 RateLimit 子配置（`zerx.logging.rate-limit`）

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | boolean | `false` | 限流开关 |
| `max-per-second` | int | `100` | 每秒允许通过的最大日志数 |

### 7.5 JsonFormat 子配置（`zerx.logging.json-format`）

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | boolean | `false` | JSON 格式开关 |
| `include-mdc` | boolean | `true` | 是否包含 MDC 上下文 |
| `include-logger-name` | boolean | `true` | 是否包含 Logger 名称 |
| `include-thread-name` | boolean | `false` | 是否包含线程名 |
| `pretty-print` | boolean | `false` | 是否美化输出（开发环境推荐） |

### 7.6 配置示例

```yaml
zerx:
  logging:
    enabled: true
    op-log:
      enabled: true
      log-to-logger: true
      record-exception-stack-trace: false
      max-param-length: 1024
      max-result-length: 2048
    sensitive-log:
      enabled: false
      filter-mobile: true
      filter-email: true
      filter-id-card: true
      filter-bank-card: true
      filter-password: true
      filter-ipv4: false
    rate-limit:
      enabled: false
      max-per-second: 100
    json-format:
      enabled: false
      include-mdc: true
      include-logger-name: true
      include-thread-name: false
      pretty-print: false
```

---

## 8. 使用示例

### 8.1 基础操作日志

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @ZerxOpLog(value = "创建订单", type = ZerxOpLog.Type.CREATE, module = "订单管理")
    @PostMapping
    public Result<Order> createOrder(@RequestBody CreateOrderReq req) {
        return Result.success(orderService.create(req));
    }
}
```

### 8.2 SpEL 动态描述

```java
@ZerxOpLog(value = "'用户 ' + #req.username + ' 登录系统'", type = ZerxOpLog.Type.LOGIN, module = "认证管理")
@PostMapping("/login")
public Result<TokenPair> login(@RequestBody LoginReq req) {
    return Result.success(authService.login(req));
}
```

### 8.3 记录参数与返回值（含敏感参数脱敏）

```java
@ZerxOpLog(value = "修改用户信息", type = ZerxOpLog.Type.UPDATE, module = "用户管理",
           recordParams = true, recordResult = true, sensitiveParams = {"password", "idCard"})
@PutMapping("/users/{id}")
public Result<User> updateUser(@PathVariable Long id, @RequestBody UpdateUserReq req) {
    return Result.success(userService.update(id, req));
}
```

### 8.4 实现 SPI 持久化

```java
@Service
public class DatabaseOpLogService implements ZerxOpLogService {

    @Autowired
    private OpLogRepository opLogRepository;

    @Override
    @Async
    public void save(ZerxOpLogEvent event) {
        OpLogEntity entity = new OpLogEntity();
        entity.setTraceId(event.traceId());
        entity.setUserId(event.userId());
        entity.setUsername(event.username());
        entity.setModule(event.module());
        entity.setType(event.type().name());
        entity.setDescription(event.description());
        entity.setClassName(event.className());
        entity.setMethodName(event.methodName());
        entity.setDurationMs(event.durationMs());
        entity.setSuccess(event.isSuccess());
        entity.setErrorMessage(event.exception() != null ? event.exception().getMessage() : null);
        entity.setClientIp(event.clientIp());
        entity.setTimestamp(event.timestamp());
        opLogRepository.save(entity);
    }
}
```

### 8.5 Logback 日志脱敏配置

**方式一：%zerxMsg 转换器**

```xml
<configuration>
    <conversionRule conversionWord="zerxMsg"
                    converterClass="com.zerx.spring.logging.converter.SensitiveMessageConverter"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %zerxMsg%n</pattern>
        </encoder>
    </appender>
</configuration>
```

**方式二：ZerxSensitiveLogEncoder 包装器**

```xml
<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="com.zerx.spring.logging.appender.ZerxSensitiveLogEncoder">
        <delegate class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </delegate>
    </encoder>
</appender>
```

### 8.6 JSON 结构化日志

```yaml
# application.yml — 启用 JSON 格式输出
zerx:
  logging:
    json-format:
      enabled: true
      include-mdc: true
      include-thread-name: true
      pretty-print: false
```

输出示例：

```json
{"timestamp":"2025-01-15T10:30:45.123+0800","level":"INFO","logger":"c.z.s.l.aspect.ZerxOpLogAspect","thread":"http-nio-8080-exec-1","message":"[OpLog] CREATE | 创建订单 | OrderController.createOrder | 45ms","mdc":{"traceId":"abc123def456"}}
```

---

## 9. 设计决策

| 决策 | 理由 |
|------|------|
| 注解 + AOP 而非拦截器 | 注解可精确标注到方法级别，灵活性更高；拦截器只能作用于 URL 级别 |
| OpLogContextExtractor 解耦 Web | 日志模块可在非 Web 环境（定时任务、MQ 消费者）中独立使用，不强制引入 Servlet 依赖 |
| `ObjectProvider` 注入可选依赖 | 未注册 SPI 实现时不会导致启动失败，符合 Spring Boot 惯例 |
| SPI 持久化异常不冒泡 | 操作日志是辅助功能，持久化失败不应影响业务主流程 |
| record 类型表示事件 | 16 个字段全部不可变，天然线程安全；Java 14+ record 语法简洁 |
| 敏感参数配置在注解级别 | 不同方法可能有不同的敏感字段需求，注解级配置比全局配置更精准 |
| ZerxJsonEncoder 零依赖实现 | 避免引入额外 JSON 库依赖，同时保证日志输出的性能（StringBuilder 拼接优于反射序列化） |
| Logback 声明为 optional | 非 Logback 环境（如 Log4j2）下模块仍可正常提供操作日志切面功能 |
| SmartLifecycle 替换 Encoder | 使用 `Integer.MAX_VALUE` 阶段确保在所有其他 Bean 初始化完成后执行，避免与 Logback 自动配置冲突 |
| 限流维度为「类名.方法名」 | 同一类方法通常具有相似的日志特征，按此维度限流可有效防止同类操作的高频输出 |
| SpEL 解析失败回退原始字符串 | 保证操作描述的容错性，不会因表达式错误导致日志记录失败 |
| `@ConditionalOnMissingBean` 覆盖机制 | 业务可提供自定义 `ZerxOpLogAspect` / `LogRateLimiter` / `SensitiveMessageConverter` 覆盖默认实现 |
