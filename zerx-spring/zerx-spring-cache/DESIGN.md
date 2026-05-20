# Zerx Spring Cache Module Design

## 1. 模块概述

Zerx Spring Cache 是一套分层的多级缓存框架，提供 Cache-Aside 自动加载、三重高可用防护（防穿透、防击穿、防雪崩）、声明式注解、Spring Cache 标准适配以及 Micrometer 可观测性。内置三种 CacheStore 实现（Caffeine 本地缓存、Redis 分布式缓存、L1+L2 多级缓存），通过单一配置项即可切换。

### 设计原则

| 原则 | 实现 |
|------|------|
| API/Impl 分离 | 业务层仅依赖轻量级 API 模块（接口 + 注解 + 常量），不引入 Caffeine / Redis 等底层依赖 |
| 零配置默认 | 仅引入 starter 即可获得完整的 Caffeine 本地缓存 + Cache-Aside + 防穿透/击穿/雪崩能力 |
| Store/Ops 分层 | `CacheStore` 负责纯 KV 存储（get/set/evict）；`CacheOps` 在其上封装 Cache-Aside 加载逻辑和防护机制 |
| SPI 可扩展 | `BloomFilter`、`CacheWarmer` 接口由业务层实现并注册为 Bean，框架自动发现集成 |
| 三种缓存类型 | 通过 `zerx.cache.type` 一键切换：`CAFFEINE`（默认）/ `REDIS` / `MULTILEVEL` |

---

## 2. 模块结构

```
zerx-spring-cache/
├── zerx-spring-cache-api/                # 纯接口 — 零底层依赖
│   ├── pom.xml
│   └── src/main/java/com/zerx/spring/cache/
│       ├── CacheStore                   底层 KV 存储接口
│       ├── CacheOps                     Cache-Aside 高级操作接口
│       ├── BloomFilter<T>               布隆过滤器接口（防穿透）
│       ├── CacheConstants               全局常量（NULL_MARKER、Pub/Sub 频道前缀、抖动范围）
│       ├── CacheException               缓存异常基类
│       └── annotation/
│           ├── ZerxCacheable            声明式缓存注解（方法返回值自动缓存）
│           ├── ZerxCacheEvict           声明式失效注解（方法执行后删除缓存）
│           └── ZerxCachePut             声明式更新注解（方法执行后写入缓存）
│
└── zerx-spring-cache-impl/              # 实现 — Caffeine / Redis / 多级缓存
    ├── pom.xml
    └── src/main/java/com/zerx/spring/cache/
        ├── autoconfigure/
        │   └── ZerxCacheAutoConfiguration   自动配置 + Bean 注册
        ├── properties/
        │   └── ZerxCacheProperties          @ConfigurationProperties
        ├── store/
        │   ├── CaffeineCacheStore           L1 本地缓存（Caffeine）
        │   ├── RedisCacheStore              L2 分布式缓存（Redis）
        │   ├── MultilevelCacheStore         多级缓存（L1 + L2）
        │   └── CacheStoreSupport            共享工具方法（TTL 抖动、键前缀）
        ├── ops/
        │   ├── CacheOpsImpl                 Cache-Aside 实现（防穿透/击穿/雪崩）
        │   └── StripedLock                  分段锁（64 条带）
        ├── bloom/
        │   └── DefaultBloomFilter<T>        基于位图的布隆过滤器
        ├── BloomFilters                    布隆过滤器工厂
        ├── aspect/
        │   └── ZerxCacheAspect              声明式注解 AOP 切面（SpEL 支持）
        ├── manager/
        │   └── ZerxCacheManager             Spring Cache 抽象适配
        ├── config/
        │   └── CacheInvalidationListener    Redis Pub/Sub 跨节点 L1 失效监听
        └── warmer/
            ├── CacheWarmer                  缓存预热器 SPI 接口
            └── CacheWarmUpRunner            预热触发器（ApplicationReadyEvent）
```

### 依赖关系

```
zerx-spring-cache-api
  └── spring-boot (仅 @FunctionalInterface 等 JDK 标准注解，零 Spring 依赖)

zerx-spring-cache-impl
  ├── zerx-spring-cache-api
  ├── com.github.ben-manes.caffeine:caffeine   (本地缓存)
  ├── org.springframework.data:spring-data-redis  (Redis，仅 REDIS/MULTILEVEL 类型时需要)
  ├── org.springframework.boot:spring-boot-autoconfigure
  ├── io.micrometer:micrometer-core             (可选，指标采集)
  └── org.aspectj:aspectjweaver                 (声明式注解 AOP)
```

API 模块零 Spring 依赖。业务代码只需引用 `zerx-spring-cache-api` 即可使用 `CacheStore`、`CacheOps`、`BloomFilter` 接口和声明式注解。

---

## 3. 架构设计

### 3.1 读取流程（Cache-Aside）

```
CacheOps.get(key, loader, ttl)
    │
    ▼
┌──────────────────────────────────────┐
│  1. BloomFilter 前置判空              │
│     mightContain(key) == false?      │
│     ├─ YES → 直接返回 null（防穿透）  │
│     └─ NO  → 继续                     │
└──────────────┬───────────────────────┘
               │
               ▼
┌──────────────────────────────────────┐
│  2. CacheStore.get(key) — 无锁查询    │
│     ├─ 命中 NULL_MARKER → 返回 null   │
│     ├─ 命中正常值    → 直接返回       │
│     └─ 未命中        → 进入慢路径      │
└──────────────┬───────────────────────┘
               │ (miss)
               ▼
┌──────────────────────────────────────┐
│  3. StripedLock.get(key) — 获取分段锁 │
│     支持 tryLock 超时 + lock 无限等待 │
└──────────────┬───────────────────────┘
               │
               ▼
┌──────────────────────────────────────┐
│  4. 双重检查（Double-Check Locking）  │
│     再次 CacheStore.get(key)          │
│     ├─ 命中 → 释放锁，返回            │
│     └─ 未命中 → 执行 loader            │
└──────────────┬───────────────────────┘
               │
               ▼
┌──────────────────────────────────────┐
│  5. 执行 loader → 写缓存              │
│     ├─ value != null → store.set()   │
│     └─ value == null → store.set(    │
│           NULL_MARKER, nullValueTtl)  │
│     释放锁，返回 value                │
└──────────────────────────────────────┘
```

### 3.2 写入流程

```
CacheOps.set(key, value, ttl)
    │
    ▼
┌──────────────────────────────────────┐
│  CacheStore.set(key, value, ttl)     │
│                                      │
│  底层自动处理：                        │
│  ├─ 键前缀拼接（withPrefix）          │
│  ├─ TTL 抖动（withJitter ±10%）      │
│  └─ 序列化（Redis 实现）              │
└──────────────────────────────────────┘
```

### 3.3 多级缓存流程

```
读取：
    L1 (Caffeine) ──命中──→ 返回
         │ miss
         ▼
    L2 (Redis)   ──命中──→ 回填 L1（使用 L1 TTL）→ 返回
         │ miss
         ▼
    上层 CacheOps 调用 loader → 写入（先 L2 后 L1）

写入（保证 L2 为准）：
    1. L2.set(key, value, ttl)        ← 先写 L2
       ├─ 成功 → 2. L1.set(key, value, l1Ttl)  ← 再写 L1
       │            └─ 失败不影响（L1 miss 可从 L2 回填）
       └─ 失败 → 抛异常，不写 L1（避免脏数据）

失效：
    1. L1.evict(key)                  ← 删除本地
    2. L2.evict(key)                  ← 删除远程
    3. Redis Pub/Sub 通知其他节点清 L1  ← 跨节点一致性
```

---

## 4. 核心组件

### 4.1 CacheStore — 底层 KV 存储接口

定义于 `zerx-spring-cache-api`，提供纯粹的缓存键值操作，不包含 Cache-Aside 加载逻辑。

```java
public interface CacheStore {
    Optional<Object> get(String key);
    void set(String key, Object value, Duration ttl);
    void evict(String key);
    void evictByPrefix(String prefix);
    boolean hasKey(String key);
    Map<String, Object> multiGet(Collection<String> keys);
    void multiSet(Map<String, Object> entries, Duration ttl);
    void multiEvict(Collection<String> keys);
}
```

#### 实现类对比

| 实现 | 存储引擎 | 键前缀 | TTL 抖动 | 批量写入 | 前缀清除 |
|------|---------|--------|---------|---------|---------|
| `CaffeineCacheStore` | Caffeine | ✅ | ✅ ±10% | 逐条写入 | stream + invalidateAll |
| `RedisCacheStore` | Redis | ✅ | ✅ ±10% | Pipeline | SCAN（非 KEYS） |
| `MultilevelCacheStore` | L1 Caffeine + L2 Redis | ✅ | 由子 Store 处理 | 逐层委托 | 逐层委托 + Pub/Sub |

### 4.2 CacheOps — Cache-Aside 高级接口

定义于 `zerx-spring-cache-api`，在 `CacheStore` 之上封装自动加载和三大防护。

```java
public interface CacheOps {
    // 读取（miss 时自动加载）
    <T> T get(String key, Supplier<T> loader, long ttl, TimeUnit timeUnit);
    <T> Optional<T> getOptional(String key, Supplier<T> loader, long ttl, TimeUnit unit);
    <T> T get(String key);  // 仅查缓存

    // 写入 / 删除
    void set(String key, Object value, long ttl, TimeUnit timeUnit);
    void set(String key, Object value, Duration ttl);
    void evict(String key);
    void evictByPrefix(String keyPrefix);

    // 批量
    <T> Map<String, T> getAll(Collection<String> keys,
            Function<Collection<String>, Map<String, T>> loader, long ttl, TimeUnit unit);

    // 访问底层 Store
    CacheStore getStore();
}
```

`CacheOpsImpl` 是默认实现，内建布隆过滤器集成、分段锁防击穿、NULL_MARKER 防穿透、Micrometer 指标采集。

### 4.3 BloomFilter — 布隆过滤器

定义于 `zerx-spring-cache-api`，提供概率性判空能力。

```java
public interface BloomFilter<T> {
    boolean mightContain(T value);   // false → 一定不存在
    void put(T value);
    default void putAll(Iterable<T> values);
    long expectedInsertions();
    double fpp();
}
```

`DefaultBloomFilter<T>` 实现特性：
- **位图存储**：`long[]` 数组，内存占用 = `expectedInsertions × (-ln(fpp) / (ln2)² / 8)` bytes
- **双重哈希**：128-bit hash 派生两个基础哈希值，再通过 `hash1 + i × hash2` 线性组合生成 k 个哈希索引
- **最优参数**：自动计算位图大小和哈希函数数量（k = ⌈ln(2) × bits / n⌉）
- **线程安全**：`StampedLock` 读写分离，高并发读使用乐观读（`tryOptimisticRead`）
- **工厂方法**：`BloomFilters.create(expectedInsertions, fpp)` 快速创建

| expectedInsertions | fpp | 位图大小 | 哈希次数 |
|-------------------|-----|---------|---------|
| 10,000 | 0.01 | ~12 KB | 7 |
| 100,000 | 0.01 | ~120 KB | 7 |
| 1,000,000 | 0.01 | ~1.2 MB | 7 |
| 1,000,000 | 0.03 | ~0.85 MB | 5 |

### 4.4 声明式注解

三个注解均定义于 `zerx-spring-cache-api`，通过 `ZerxCacheAspect` AOP 切面拦截处理。

| 注解 | 用途 | SpEL Key | TTL | 特殊属性 |
|------|------|---------|-----|---------|
| `@ZerxCacheable` | 方法返回值自动缓存 | ✅ `key()` | ✅ `ttl()` + `timeUnit()` | `condition`、`unless`、`nullCache` |
| `@ZerxCacheEvict` | 方法执行后删除缓存 | ✅ `key()` | — | `prefixEvict` |
| `@ZerxCachePut` | 方法执行后强制写入 | ✅ `key()` | ✅ `ttl()` + `timeUnit()` | — |

**SpEL 上下文变量**：
- 方法参数：`#paramName`（按参数名）或 `#p0`、`#a0`（按索引）
- 返回值（仅 `unless`）：`#result`

**Key 格式**：`{name}:{SpEL解析值}`，SpEL 为空时使用 `methodName:paramsHash`

### 4.5 CacheWarmer — 缓存预热 SPI

```java
@FunctionalInterface
public interface CacheWarmer {
    void warmUp();
    default int order() { return Integer.MAX_VALUE; }  // 数字越小越先执行
}
```

业务层实现 `CacheWarmer` 接口并注册为 Spring Bean，`CacheWarmUpRunner` 在 `ApplicationReadyEvent` 后自动按 `order()` 升序执行。预热失败不影响应用启动。

### 4.6 CacheInvalidationListener — 跨节点 L1 失效

```
节点 A 写入/删除 → MultilevelCacheStore
    │
    ▼
Redis Pub/Sub 发布 → zerx:cache:invalidate:{fullKey}
    │
    ▼
节点 B/C/D 监听 → CacheInvalidationListener.onMessage()
    │
    ▼
本地 L1 CacheStore.evict(key) / evictByPrefix(key)
```

频道格式：`zerx:cache:invalidate:{fullKey}`，消息体为操作类型（`evict` / `evict_prefix`）。Pub/Sub 失败不阻塞主流程。

### 4.7 ZerxCacheManager — Spring Cache 适配

实现 `CacheManager` 接口，桥接 `CacheStore`，使 Spring 原生 `@Cacheable`、`@CacheEvict`、`@CachePut` 也能使用 Zerx 缓存基础设施。

- 支持 per-cache-name TTL：通过 `zerx.cache.custom-ttls.<cacheName>=30m` 配置
- `ZerxCacheAdapter` 内部同样使用 `StripedLock` + 双重检查实现防击穿
- 识别 `NULL_MARKER` 值并正确处理（返回 null 而非标记对象）

### 4.8 Micrometer 指标

`CacheOpsImpl` 在构造时可传入 `MeterRegistry`，自动注册以下指标：

| 指标名 | 类型 | 说明 |
|--------|------|------|
| `zerx.cache.hits` | Counter | 缓存命中次数 |
| `zerx.cache.misses` | Counter | 缓存未命中次数 |
| `zerx.cache.loads` | Counter | Loader 执行次数 |
| `zerx.cache.evictions` | Counter | 缓存删除次数 |
| `zerx.cache.load.duration` | Timer | Loader 执行耗时 |

当 `zerx.cache.caffeine.record-stats=true` 且 `MeterRegistry` 存在时，自动绑定 `CaffeineCacheMetrics`。

---

## 5. 三大防护机制

### 5.1 防穿透（Anti-Penetration）

三层递进防线，拦截不存在的数据对缓存和数据源的冲击：

```
请求进入
    │
    ▼
┌──────────────────────────────────────────────┐
│ 第一层：BloomFilter 前置判空                    │
│ mightContain(key) == false → 直接返回 null     │
│ 内存成本极低（百万级数据仅需 ~1.2 MB）           │
│ 缺点：存在假阳性，无法 100% 拦截                 │
└──────────────┬───────────────────────────────┘
               │ mightContain == true（可能存在）
               ▼
┌──────────────────────────────────────────────┐
│ 第二层：NULL_MARKER 空值缓存                    │
│ CacheStore 缓存 key → "__ZERX_CACHE_NULL__"   │
│ TTL 由 nullValueTtl 控制（默认 5 分钟）         │
│ 命中 NULL_MARKER → 返回 null，不穿透到数据源    │
└──────────────┬───────────────────────────────┘
               │ NULL_MARKER 未命中或已过期
               ▼
┌──────────────────────────────────────────────┐
│ 第三层：数据源查询                               │
│ 执行 loader 从数据库加载                         │
│ ├─ 数据存在 → 正常写入缓存                       │
│ └─ 数据不存在 → 写入 NULL_MARKER（回到第二层）    │
└──────────────────────────────────────────────┘
```

### 5.2 防击穿（Anti-Stampede）

通过 `StripedLock`（固定 64 条带分段锁）+ 双重检查锁定实现：

```
线程 A ──miss──→ StripedLock.get(key) ──获取锁──→ 双重检查 ──miss──→ loader() ──写缓存──→ 释放锁
线程 B ──miss──→ StripedLock.get(key) ──等待锁──────────────────→ 获取锁 ──双重检查命中──→ 返回
线程 C ──miss──→ StripedLock.get(key) ──等待锁─────────────────────────────────────→ 获取锁 ──命中──→ 返回
```

| 特性 | 说明 |
|------|------|
| 固定 64 条带 | 避免 ConcurrentHashMap 无限增长的内存泄漏 |
| 位运算索引 | `STRIPE_COUNT = 64`，使用 `hashCode & INDEX_MASK` 替代取模 |
| 可配超时 | `zerx.cache.lockTimeout`（默认 5s），超时抛出 `LockTimeoutException` |
| JVM 级别 | 多节点部署建议结合 Redis 分布式锁扩展 |

### 5.3 防雪崩（Anti-Avalanche）

通过 TTL 随机抖动避免大量缓存同时过期：

```java
// CacheStoreSupport.withJitter()
double jitter = 0.9 + ThreadLocalRandom.current().nextDouble() * (1.1 - 0.9);
return Math.max(1, (long) (ttlNanos * jitter));
```

- 抖动范围：**±10%**（`JITTER_MIN = 0.9`，`JITTER_MAX = 1.1`）
- 应用位置：所有 `CacheStore` 实现的 `set()` 方法内部
- 效果：TTL=30min 的缓存，实际过期时间在 27min ~ 33min 之间均匀分布

---

## 6. 自动配置

### 6.1 激活条件

| 条件 | 注解 |
|------|------|
| `zerx.cache.enabled` 为 `true` | `@ConditionalOnProperty(matchIfMissing = true)` |

### 6.2 CacheStore Bean 注册

| 条件 | 注册 Bean |
|------|----------|
| `type = CAFFEINE`（默认） | `CaffeineCacheStore` |
| `type = REDIS` + Redis classpath | `RedisCacheStore` + `zerxCacheRedisTemplate` |
| `type = MULTILEVEL` + Redis classpath | `l1CacheStore` + `l2CacheStore` + `MultilevelCacheStore` + Pub/Sub 容器 |

所有 `CacheStore` Bean 均支持 `@ConditionalOnMissingBean` 覆盖。

### 6.3 通用 Bean

| Bean | 条件 | 说明 |
|------|------|------|
| `CacheOps` | `@ConditionalOnMissingBean` | 自动注入可选的 `BloomFilter<String>` |
| `ZerxCacheManager` | `@ConditionalOnMissingBean` | Spring Cache 抽象适配 |
| `ZerxCacheAspect` | `@ConditionalOnMissingBean` | 声明式注解 AOP 切面 |
| `CacheWarmUpRunner` | `@ConditionalOnBean(CacheWarmer.class)` | 仅当存在预热器时注册 |
| `CaffeineCacheMetrics` | `record-stats=true` + Micrometer classpath | Caffeine 统计指标绑定 |

### 6.4 Imports

`AutoConfiguration.imports` 注册：

```
com.zerx.spring.cache.autoconfigure.ZerxCacheAutoConfiguration
```

---

## 7. 配置属性

配置根路径：`zerx.cache`

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | boolean | `true` | 总开关，`false` 时完全禁用缓存模块 |
| `type` | `CacheType` | `CAFFEINE` | 缓存类型：`CAFFEINE` / `REDIS` / `MULTILEVEL` |
| `key-prefix` | String | `zerx:` | 全局键前缀（幂等添加） |
| `default-ttl` | Duration | `30m` | 注解未指定 TTL 时的回退值 |
| `null-value-ttl` | Duration | `5m` | 空值标记缓存时间（防穿透），≤0 不缓存空值 |
| `lock-timeout` | Duration | `5s` | 防击穿锁等待超时 |
| `serializer` | `SerializerType` | `JACKSON` | Redis 值序列化策略：`JACKSON`（带类型头）/ `JSON`（无类型头） |
| `caffeine.max-size` | int | `10000` | Caffeine 最大条目数 |
| `caffeine.expire-after-write` | Duration | `10m` | Caffeine 写入后过期时间 |
| `caffeine.expire-after-access` | Duration | `30m` | Caffeine 访问后过期时间 |
| `caffeine.record-stats` | boolean | `false` | 是否开启 Caffeine 统计 |
| `multilevel.l1.max-size` | int | `1000` | 多级缓存 L1 最大条目数 |
| `multilevel.l1.expire-after-write` | Duration | `5m` | 多级缓存 L1 写入后过期时间 |
| `multilevel.l2.expire-after-write` | Duration | `30m` | 多级缓存 L2 写入后过期时间 |
| `custom-ttls` | `Map<String, Duration>` | `{}` | per-cache-name 自定义 TTL |

### 配置示例

```yaml
zerx:
  cache:
    enabled: true
    type: MULTILEVEL
    key-prefix: "app:"
    default-ttl: 30m
    null-value-ttl: 5m
    lock-timeout: 5s
    serializer: JACKSON
    caffeine:
      max-size: 10000
      expire-after-write: 10m
      expire-after-access: 30m
      record-stats: true
    multilevel:
      l1:
        max-size: 1000
        expire-after-write: 5m
      l2:
        expire-after-write: 30m
    custom-ttls:
      userCache: 60m
      configCache: 24h
```

---

## 8. 使用示例

### 8.1 零配置使用（Caffeine 本地缓存）

无需任何配置，引入 starter 即可使用：

```java
@Autowired
private CacheOps cacheOps;

// 读取（miss 时自动从数据库加载）
User user = cacheOps.get("user:1", () -> userRepository.findById(1L), 30, TimeUnit.MINUTES);

// 写入
cacheOps.set("user:1", userVO, 30, TimeUnit.MINUTES);

// 删除
cacheOps.evict("user:1");

// 批量读取
Map<String, User> users = cacheOps.getAll(
    List.of("user:1", "user:2", "user:3"),
    keys -> userRepository.findAllById(keys.stream().map(Long::parseLong).toList())
         .stream().collect(Collectors.toMap(u -> "user:" + u.getId(), u -> u)),
    30, TimeUnit.MINUTES);
```

### 8.2 声明式注解

```java
@Service
public class UserService {

    // 自动缓存方法返回值，miss 时执行方法体
    @ZerxCacheable(name = "user", key = "#id", ttl = 30, timeUnit = TimeUnit.MINUTES)
    public User getUser(Long id) {
        return userRepository.findById(id);
    }

    // 条件缓存：仅 id > 0 时走缓存
    @ZerxCacheable(name = "user", key = "#id", condition = "#id > 0")
    public User getUserWithCondition(Long id) {
        return userRepository.findById(id);
    }

    // 后置排除：空列表不缓存
    @ZerxCacheable(name = "userList", key = "#ids.hashCode()", unless = "#result.isEmpty()")
    public List<User> listUsers(List<Long> ids) {
        return userRepository.findAllById(ids);
    }

    // 方法执行后强制写入缓存
    @ZerxCachePut(name = "user", key = "#user.id", ttl = 30, timeUnit = TimeUnit.MINUTES)
    public User createUser(User user) {
        return userRepository.save(user);
    }

    // 方法执行后删除缓存
    @ZerxCacheEvict(name = "user", key = "#id")
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    // 按前缀批量删除
    @ZerxCacheEvict(name = "user", key = "#tenantId", prefixEvict = true)
    public void clearTenantUsers(Long tenantId) {
        // ...
    }
}
```

### 8.3 Spring Cache 原生注解

`ZerxCacheManager` 使 Spring 原生注解也能使用 Zerx 缓存：

```java
@Service
public class ConfigService {

    @org.springframework.cache.annotation.Cacheable(value = "configCache", key = "#key")
    public String getConfig(String key) {
        return configRepository.findByKey(key);
    }
}
```

### 8.4 布隆过滤器集成

```java
@Configuration
public class CacheConfig {

    // 注册布隆过滤器 Bean，CacheOps 自动发现集成
    @Bean
    public BloomFilter<String> userBloomFilter() {
        BloomFilter<String> filter = BloomFilters.create(1_000_000, 0.01);
        // 预热：加载所有合法用户 ID
        List<Long> allUserIds = userRepository.findAllIds();
        filter.putAll(allUserIds.stream().map(String::valueOf).toList());
        return filter;
    }
}
```

### 8.5 缓存预热

```java
@Component
public class HotDataCacheWarmer implements CacheWarmer {

    @Autowired
    private CacheOps cacheOps;
    @Autowired
    private ConfigRepository configRepository;

    @Override
    public void warmUp() {
        List<Config> hotConfigs = configRepository.findHotConfigs();
        hotConfigs.forEach(c ->
            cacheOps.set("config:" + c.getKey(), c, 1, TimeUnit.HOURS));
    }

    @Override
    public int order() {
        return 100; // 数字越小越先执行
    }
}
```

### 8.6 多级缓存配置

```yaml
zerx:
  cache:
    type: MULTILEVEL          # 启用 L1 + L2 多级缓存
    key-prefix: "myapp:"
    default-ttl: 30m
    multilevel:
      l1:
        max-size: 5000
        expire-after-write: 5m    # L1 短 TTL，保证最终一致性
      l2:
        expire-after-write: 30m   # L2 长 TTL，减少回源
```

---

## 9. 设计决策

| 决策 | 理由 |
|------|------|
| API/Impl 模块拆分 | 业务层仅依赖 API（接口 + 注解 + 常量），避免引入 Caffeine / Redis 等重量级依赖 |
| Store/Ops 两层抽象 | `CacheStore` 关注存储适配（可独立测试和替换），`CacheOps` 关注业务语义（Cache-Aside、防穿透/击穿/雪崩） |
| 固定 64 条带分段锁 | 避免 `ConcurrentHashMap` per-key 锁的无限增长内存泄漏，2 的幂便于位运算优化 |
| 多级缓存写操作先 L2 后 L1 | L2 是权威数据源；L2 失败时不写 L1，避免 L1 存在 L2 没有的脏数据 |
| Redis Pub/Sub 而非消息队列 | 轻量级跨节点通知，Pub/Sub 失败不阻塞主流程（降级为 L1 自然过期） |
| SCAN 替代 KEYS 做前缀清除 | 避免大库 KEYS 阻塞 Redis 主线程 |
| NULL_MARKER 字符串常量 | 无需额外类型，所有 CacheStore 实现统一识别 |
| TTL ±10% 抖动 | 在不影响业务有效性的前提下，将同时写入的大量缓存的过期时间打散 |
| SpEL 表达式缓存（ConcurrentHashMap） | 避免每次方法调用都重新解析 SpEL 表达式 |
| `CacheWarmer` SPI 而非配置驱动 | 业务层可注入任意 Spring Bean，灵活度远高于 YAML 配置 |
| `matchIfMissing = true` | 引入 starter 即开箱即用，无需显式配置 `enabled: true` |
| `@ConditionalOnMissingBean` 全覆盖 | 业务可随时替换任意组件（CacheStore、CacheOps、CacheManager） |
| ZerxCacheManager 内部也使用 StripedLock | 通过 `CacheManager` 使用的 Spring 原生注解同样享有防击穿保护 |
