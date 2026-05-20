# Zerx Common 模块设计文档

## 1. 模块概述

Zerx Common 是 Zerx 框架的纯 JDK 基础工具库，零第三方依赖（无 Spring、无 Lombok、无 Guava），为所有上层模块提供统一的异常体系、通用数据模型、加解密工具、领域事件总线等基础能力。作为 `zerx-core` 层的核心组件，它是整个框架的"根依赖"。

### 设计原则

| 原则 | 实现 |
|------|------|
| 零依赖 | `pom.xml` 编译期无任何第三方 jar；ArchUnit 规则在构建时强制校验 |
| JDK 21 前沿特性 | UUIDv7 (RFC 9562)、虚拟线程 (`Thread.ofVirtual`)、record 类型、sealed 层次 |
| 异常体系统一 | 5 级分段错误码 (1xxxx-5xxxx)，所有自定义异常继承 `ZerxException` |
| SPI 友好解耦 | `OpLogContextExtractor` 作为 `@FunctionalInterface`，跨模块零耦合注册 |
| 领域事件驱动 | 发布-订阅模式解耦业务逻辑与副作用，支持同步分发和注解驱动监听 |
| 架构约束自动化 | ArchUnit 12 条规则强制零依赖、命名规范、异常层次、无循环依赖 |

---

## 2. 模块结构

```
zerx-core/zerx-common/
├── pom.xml                                    # 零第三方依赖（仅 JUnit 测试）
└── src/
    ├── main/java/com/zerx/common/
    │   ├── constants/
    │   │   └── CommonConstant                 全局常量定义
    │   ├── enums/
    │   │   ├── BaseEnum<C>                    业务枚举统一接口
    │   │   ├── CommonStatus                   通用状态枚举
    │   │   └── YesNo                          是否枚举
    │   ├── exception/
    │   │   ├── ZerxException                  异常抽象基类
    │   │   ├── BusinessException              业务逻辑异常 (2xxxx)
    │   │   ├── ValidationException            参数校验异常 (3xxxx)
    │   │   ├── AuthorizationException         权限相关异常 (4xxxx)
    │   │   ├── NotFoundException              资源未找到异常
    │   │   ├── ExternalServiceException       外部服务异常 (5xxxx)
    │   │   └── ErrorCode                      分段错误码定义
    │   ├── model/
    │   │   ├── Result<T>                      统一响应结构体 (record)
    │   │   ├── PageRequest<T>                 分页请求 (record)
    │   │   ├── PageResult<T>                  分页响应 (record)
    │   │   ├── Pair<L,R>                      二元组 (record)
    │   │   ├── Triple<F,S,T>                  三元组 (record)
    │   │   └── ValidationResult               校验结果
    │   ├── functional/
    │   │   ├── ThrowableFunction<T,R>         支持受检异常的 Function
    │   │   ├── ThrowableConsumer<T>           支持受检异常的 Consumer
    │   │   ├── ThrowableSupplier<T>           支持受检异常的 Supplier
    │   │   └── ThrowableRunnable              支持受检异常的 Runnable
    │   ├── logging/
    │   │   ├── LogHelper                      异常日志结构化格式化
    │   │   ├── LogRateLimiter                 日志限流器（滑动窗口）
    │   │   ├── SensitiveLogFilter             日志敏感数据自动脱敏
    │   │   ├── OpLogContextExtractor          操作日志上下文 SPI (@FunctionalInterface)
    │   │   └── package-info
    │   ├── event/
    │   │   ├── EventBus                       领域事件总线接口
    │   │   ├── SimpleEventBus                 内存同步事件总线实现
    │   │   ├── DomainEvent                    领域事件基类
    │   │   ├── DomainEventListener<T>         事件监听器接口 (@FunctionalInterface)
    │   │   └── package-info
    │   ├── crypto/
    │   │   ├── AesUtil                        AES 对称加解密 (GCM/CBC)
    │   │   ├── HmacUtil                       HMAC 消息认证
    │   │   ├── DigestUtil                     摘要算法 (SHA/MD5)
    │   │   └── package-info
    │   ├── concurrent/
    │   │   ├── ThreadUtil                     虚拟线程 / 线程池构建
    │   │   ├── StopWatch                      任务计时器
    │   │   └── package-info
    │   ├── cache/
    │   │   ├── LruCache<K,V>                  LRU 缓存（计算型 / 同步安全）
    │   │   └── package-info
    │   ├── retry/
    │   │   ├── Retryer<T>                     通用重试工具（指数退避 / 抖动）
    │   │   └── package-info
    │   └── util/                              (18 个工具类)
    │       ├── StringUtil                     字符串工具
    │       ├── CollectionUtil                 集合工具
    │       ├── DateUtil                       日期时间工具
    │       ├── ReflectUtil                    反射工具
    │       ├── UuidUtil                       UUIDv7 (RFC 9562) / UUIDv4
    │       ├── IoUtil                         IO 流操作工具
    │       ├── NumberUtil                     数值工具
    │       ├── AssertUtil                     断言工具
    │       ├── EnumUtil                       枚举工具
    │       ├── SensitiveDataUtil              敏感数据脱敏
    │       ├── ExceptionUtil                  异常链解析
    │       ├── SystemUtil                     系统信息工具
    │       ├── FileUtil                       文件操作工具
    │       ├── ArrayUtil                      数组工具
    │       ├── ConvertUtil                    类型转换
    │       ├── Base64Util                     Base64 编解码
    │       ├── RandomUtil                     随机数工具
    │       └── SnowflakeId                    雪花算法 ID 生成器 (CAS 无锁)
    └── test/java/com/zerx/common/
        ├── constants/   (1)
        ├── enums/       (3)
        ├── exception/   (7)
        ├── functional/  (4)
        ├── logging/     (4)
        ├── event/       (3)
        ├── crypto/      (3)
        ├── concurrent/  (2)
        ├── cache/       (1)
        ├── retry/       (1)
        └── util/        (16)
```

### 依赖关系

```
zerx-common
  └── JDK 21（编译期唯一依赖）
      ├── java.base
      ├── java.crypto (javax.crypto)
      ├── java.naming (com.zerx.common 的 javax 命名空间)
      └── java.logging（通过 ArchUnit 白名单允许）
```

运行时 `pom.xml` 无任何第三方依赖。单元测试仅依赖 `junit-jupiter`（test scope）。

---

## 3. 核心能力

### 3.1 异常体系 (`exception/`)

统一的异常层次结构，所有自定义异常均为非受检异常（`RuntimeException`），携带 `ErrorCode` 实例。

```
RuntimeException
  └── ZerxException (abstract)
        ├── BusinessException            业务逻辑异常
        │     默认 ErrorCode: BUSINESS_ERROR (20001)
        ├── ValidationException          参数校验异常
        │     默认 ErrorCode: PARAM_FORMAT_ERROR (30002)
        ├── AuthorizationException       权限相关异常
        │     默认 ErrorCode: FORBIDDEN (40002)
        ├── NotFoundException            资源未找到异常
        │     默认 ErrorCode: DATA_NOT_FOUND (20002)
        └── ExternalServiceException     外部服务异常
              默认 ErrorCode: EXTERNAL_SERVICE_ERROR (50001)
```

**ErrorCode 分段编码：**

| 段位 | 范围 | 说明 | 预定义数量 |
|------|------|------|-----------|
| 0xxxx | 00000-09999 | 成功 | 1 (`SUCCESS`) |
| 1xxxx | 10000-19999 | 系统级异常（网络、IO、运行时） | 6 |
| 2xxxx | 20000-29999 | 业务逻辑异常 | 8 |
| 3xxxx | 30000-39999 | 参数校验异常 | 6 |
| 4xxxx | 40000-49999 | 权限相关异常 | 6 |
| 5xxxx | 50000-59999 | 外部服务异常 | 4 |

`ErrorCode` 实现了 `BaseEnum<String>` 接口，支持通过 `ErrorCode.of()` 工厂方法注册自定义错误码（`ConcurrentHashMap` 去重），全局可通过 `ErrorCode.fromCode()` 反查。

### 3.2 统一响应模型 (`model/`)

| 类 | 类型 | 用途 |
|----|------|------|
| `Result<T>` | record | 统一 API 响应，`ok()` / `fail()` 静态工厂 |
| `PageRequest<T>` | record | 分页请求参数 |
| `PageResult<T>` | record | 分页响应结果 |
| `Pair<L,R>` | record | 二元组，替代 Map.Entry 的不可变载体 |
| `Triple<F,S,T>` | record | 三元组 |
| `ValidationResult` | class | 校验结果（成功/失败 + 错误消息列表） |

所有 model 均为不可变类型（record 或 final class），支持序列化。

### 3.3 UUIDv7 与分布式 ID (`util/UuidUtil`, `util/SnowflakeId`)

**UuidUtil — RFC 9562 UUIDv7：**

```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                         unix_ts_ms (48bit)                    |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|          unix_ts_ms           |ver |       rand_a (12bit)     |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|var|                       rand_b (62bit)                      |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                           rand_b                              |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

| 方法 | 特点 | 适用场景 |
|------|------|---------|
| `uuidv7()` | SecureRandom，加密安全 | 数据库主键、安全令牌 |
| `fastUuidv7()` | 普通 Random，性能优先 | 日志追踪 ID、临时标识 |
| `monotonicUuidv7()` | 同毫秒内严格递增（synchronized） | 消息排序、严格有序主键 |
| `uuidv7Batch(n)` | 批量生成（上限 10000） | 预生成 ID |

**SnowflakeId — CAS 无锁雪花算法：**

| 特性 | 值 |
|------|-----|
| ID 结构 | 41 bit 时间戳 + 10 bit workerId + 13 bit sequence |
| 机器数 | 最多 1024 台（workerId: 0-1023） |
| 单机吞吐 | >400 万/秒（AtomicLong CAS 无锁） |
| 默认纪元 | 2024-01-01 00:00:00 +08:00 |

### 3.4 领域事件系统 (`event/`)

发布-订阅模式，解耦领域逻辑与副作用（通知、审计、日志）。

```
业务代码
  │
  ▼
EventBus.publish(DomainEvent)
  │
  ▼
SimpleEventBus (同步分发)
  │
  ├─ DomainEventListener<A>.onEvent(A)    ← 注解驱动发现
  ├─ DomainEventListener<B>.onEvent(B)
  └─ DomainEventListener<C>.onEvent(C)
```

| 组件 | 职责 |
|------|------|
| `EventBus` | 发布-订阅接口定义 |
| `SimpleEventBus` | 内存同步实现（ConcurrentHashMap） |
| `DomainEvent` | 事件基类（eventId/eventType/aggregateType/occurredAt/version） |
| `DomainEventListener<T>` | `@FunctionalInterface` 监听器，支持 `of(eventType, consumer)` 工厂方法 |

`DomainEvent` 自动使用 `UuidUtil.uuidv7String()` 生成事件 ID，天然有序。

### 3.5 加解密工具 (`crypto/`)

| 工具 | 算法 | 特点 |
|------|------|------|
| `AesUtil` | AES-GCM / AES-CBC | ThreadLocal 缓存 Cipher 实例；GCM 认证加密防篡改；IV 自动生成并拼接 |
| `HmacUtil` | HMAC-SHA256 / SHA384 / SHA512 | 标准 HMAC 消息认证码 |
| `DigestUtil` | SHA-256 / SHA-384 / SHA-512 / MD5 | 摘要计算与盐值摘要 |

所有加密操作密文格式：`IV (bytes) + Ciphertext (+ Tag)`，HEX 编码输出。

### 3.6 并发工具 (`concurrent/`)

| 工具 | 能力 |
|------|------|
| `ThreadUtil` | JDK 21 虚拟线程 (`startVirtual`, `virtualThreadFactory`, `newVirtualExecutor`)；平台线程池构建（`newFixedPool`, `newCachedPool`, `newScheduledPool`）；安全休眠 |
| `StopWatch` | 任务分段计时，支持 `split()` / `totalMillis()` / `toSummaryString()` |

### 3.7 缓存与重试 (`cache/`, `retry/`)

**LruCache：** 基于 `LinkedHashMap(accessOrder=true)` 的 LRU 缓存。

| 变体 | 创建方式 | 特点 |
|------|---------|------|
| 基础缓存 | `new LruCache<>(maxSize)` | 非线程安全 |
| 计算型缓存 | `LruCache.computing(maxSize, loader)` | 未命中自动计算 |
| 同步安全 | `LruCache.synchronizedLruCache(maxSize)` | synchronized 方法包装 |

内置命中率统计（`hitRate()`），惰性淘汰。

**Retryer：** Builder 模式通用重试工具。

| 退避策略 | 说明 |
|---------|------|
| `NONE` | 无退避，立即重试 |
| `FIXED` | 固定间隔 |
| `LINEAR` | 线性增长 |
| `EXPONENTIAL` | 指数退避（推荐） |

支持异常类型过滤 (`retryOn`)、自定义谓词 (`retryWhen`)、抖动 (`jitter`, ±25%)。`InterruptedException` 直接上抛不重试，`Error` 不捕获。

### 3.8 日志工具 (`logging/`)

| 工具 | 能力 |
|------|------|
| `LogHelper` | 异常结构化格式化：错误码前缀 + cause chain + 业务上下文 KV |
| `LogRateLimiter` | 滑动窗口计数器限流：按 key 独立计数，自动抑制与摘要输出，惰性清理过期 key |
| `SensitiveLogFilter` | 日志自动脱敏：内置 6 类规则（手机号/邮箱/身份证/银行卡/密码/IPv4），支持自定义规则注册 |
| `OpLogContextExtractor` | `@FunctionalInterface` SPI 接口，由上层模块（如 zerx-spring-web）注入用户上下文 |

`OpLogContextExtractor.OpLogContext` 是 record 类型 `(Long userId, String username, String clientIp)`，实现了 common 层与 Spring 层的完全解耦。

### 3.9 函数式异常接口 (`functional/`)

弥补 JDK 标准 `Function`/`Consumer`/`Supplier`/`Runnable` 不支持受检异常的缺陷。

| 接口 | 对应 JDK 接口 | 关键方法 |
|------|-------------|---------|
| `ThrowableFunction<T,R>` | `Function<T,R>` | `unchecked()` 转标准 Function；`andThen()` 函数组合 |
| `ThrowableConsumer<T>` | `Consumer<T>` | `unchecked()` 转标准 Consumer |
| `ThrowableSupplier<T>` | `Supplier<T>` | `unchecked()` 转标准 Supplier |
| `ThrowableRunnable` | `Runnable` | `unchecked()` 转标准 Runnable |

```java
// 在 Stream 中处理可能抛异常的操作
List<String> lines = files.stream()
    .map(ThrowableFunction.wrap(file -> Files.readString(file)))
    .toList();
```

---

## 4. 设计决策

| 决策 | 理由 |
|------|------|
| 零第三方依赖（pom.xml 无编译依赖） | 确保 common 层可被任何环境引用（CLI 工具、Android、GraalVM native-image）；ArchUnit 构建时强制校验 |
| 异常采用非受检异常（RuntimeException） | 避免 catch 样板代码污染业务逻辑；全局异常处理器统一兜底 |
| ErrorCode 分段编码 (1xxxx-5xxxx) | 通过错误码首位快速定位异常类别；HTTP 状态码内嵌于 ErrorCode，简化响应映射 |
| UUIDv7 替代 UUIDv4 作为默认 ID | 时间有序，B+ 树索引插入无随机写开销；RFC 9562 标准兼容 |
| SnowflakeId 使用 AtomicLong CAS 而非 synchronized | 吞吐量提升 2-3 倍；单一 AtomicLong 做组合 CAS 减少冲突概率 |
| AES-GCM 作为默认加密模式 | 认证加密（AEAD）同时提供机密性和完整性保护；无需额外 HMAC |
| AesUtil 使用 ThreadLocal 缓存 Cipher | Cipher 非线程安全且创建开销大；ThreadLocal 按线程隔离，零竞争 |
| OpLogContextExtractor 为 @FunctionalInterface | SPI 扩展点：common 层定义契约，spring-web 层注入 RequestContext 实现；非 Web 环境无需注册 |
| LruCache 基于 LinkedHashMap 而非第三方库 | 保持零依赖约束；accessOrder=true 天然支持 LRU 淘汰 |
| Retryer 的 InterruptedException 直接上抛 | 中断语义不应被重试机制吞没；遵循 Java 并发最佳实践 |
| 所有 model 使用 record 类型 | 不可变、自动生成 equals/hashCode/toString、简洁；JDK 21 原生支持 |
| BaseEnum<C> 泛型接口 | 统一枚举的 code/description 访问模式；泛型支持 Integer 和 String 编码类型 |
| SensitiveLogFilter 内置 + 自定义双层规则 | 覆盖常见敏感数据场景；自定义规则支持运行时注册/卸载 |
| ArchUnit 12 条规则覆盖 | 零依赖（第三方/Spring/SLF4J）、无循环依赖、util 类规范（final + 私有构造）、异常层次、命名规范、禁止 System.out、禁止 sun.* 包 |

---

## 5. 使用示例

### 5.1 统一异常抛出

```java
// 使用预定义错误码
if (balance.compareTo(amount) < 0) {
    throw new BusinessException(ErrorCode.BALANCE_NOT_ENOUGH);
}

// 自定义消息覆盖默认描述
throw new BusinessException(ErrorCode.BALANCE_NOT_ENOUGH,
    "账户余额不足，当前余额: " + balance);

// 携带原始异常（异常链追踪）
try {
    paymentService.charge(orderId, amount);
} catch (PaymentGatewayException e) {
    throw new ExternalServiceException(ErrorCode.EXTERNAL_SERVICE_ERROR,
        "支付网关调用失败", e);
}

// 注册并使用自定义错误码
ErrorCode LOW_STOCK = ErrorCode.of("20010", "库存不足", 400);
throw new BusinessException(LOW_STOCK);
```

### 5.2 统一响应封装

```java
// 成功响应
return Result.ok(userDTO);
return Result.ok("查询成功", userDTO);

// 失败响应
return Result.fail(ErrorCode.DATA_NOT_FOUND);
return Result.fail("50001", "支付服务暂时不可用");
```

### 5.3 UUIDv7 生成

```java
// 标准使用（安全随机）
String orderId = UuidUtil.uuidv7String();
// → "0194a2b3-c4d5-7e6f-8a9b-0c1d2e3f4a5b"

// 高性能场景
String traceId = UuidUtil.fastUuidv7().toString();

// 严格有序（数据库主键）
String pk = UuidUtil.monotonicUuidv7String();

// 从 UUIDv7 提取时间戳
long ts = UuidUtil.extractTimestamp(uuid);  // 毫秒级 Unix 时间戳
```

### 5.4 领域事件发布与订阅

```java
// 定义领域事件
public record OrderCreatedEvent(Long orderId, Long userId, BigDecimal amount)
        extends DomainEvent {
    public OrderCreatedEvent(Long orderId, Long userId, BigDecimal amount) {
        super("order", "OrderCreated");
    }
}

// 发布事件
eventBus.publish(new OrderCreatedEvent(orderId, userId, amount));

// 订阅事件（Consumer 简写方式）
eventBus.subscribe("OrderCreated",
    DomainEventListener.of("OrderCreated", event -> {
        notificationService.sendOrderConfirmation(event);
    }));
```

### 5.5 AES-GCM 加解密

```java
// 生成密钥
SecretKey key = AesUtil.generateKey(256);

// 加密（自动生成随机 IV，拼接为 HEX 输出）
String ciphertext = AesUtil.encryptGcm("敏感数据", key);

// 解密
String plaintext = AesUtil.decryptGcm(ciphertext, key);

// 从 HEX 字符串构建密钥（适合配置化场景）
SecretKey configuredKey = AesUtil.keyFromHex("603deb1015ca71be2b73aef0857d7781");
```

### 5.6 虚拟线程与线程池

```java
// 启动虚拟线程
ThreadUtil.startVirtual("order-processor", () -> processOrder(order));

// 虚拟线程执行器（I/O 密集型任务）
try (ExecutorService executor = ThreadUtil.newVirtualExecutor("io-worker")) {
    List<Future<?>> futures = tasks.stream()
        .map(task -> executor.submit(() -> task.run()))
        .toList();
}

// 带命名的平台线程池
ExecutorService pool = ThreadUtil.newFixedPool(4, "biz-pool");
```

### 5.7 重试机制

```java
// 简单重试 3 次
String result = Retryer.<String>of()
    .maxAttempts(3)
    .execute(() -> callRemoteService());

// 指数退避 + 抖动
String result = Retryer.<String>of()
    .maxAttempts(5)
    .backoff(Duration.ofMillis(100), Duration.ofSeconds(5))
    .jitter()
    .retryOn(IOException.class, TimeoutException.class)
    .execute(() -> callRemoteService());
```

### 5.8 日志限流与脱敏

```java
// 创建限流器：每分钟最多 10 条同类日志
private static final LogRateLimiter limiter =
    LogRateLimiter.of(10, Duration.ofMinutes(1));

if (limiter.tryAcquire("orderTimeout")) {
    log.error("订单超时, orderId={}", orderId);
}

// 日志自动脱敏
String safe = SensitiveLogFilter.filter(
    "用户手机13812345678登录，密码=pwd123，IP=192.168.1.100");
// → "用户手机138****5678登录，密码=******，IP=192.168.*.*"
```

### 5.9 OpLogContextExtractor SPI 扩展

```java
// 在 zerx-spring-web 中自动注册
@Component
public class WebOpLogContextExtractor implements OpLogContextExtractor {
    @Override
    public OpLogContext extract() {
        return new OpLogContext(
            RequestContext.getUserId(),
            RequestContext.getUsername(),
            RequestContext.getClientIp()
        );
    }
}

// 在 zerx-common 层使用（无需知道 Spring 的存在）
OpLogContext ctx = extractor.extract();
// → OpLogContext[userId=10086, username=zhangsan, clientIp=10.0.0.1]
```
