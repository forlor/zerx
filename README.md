<p align="center">
  <h1 align="center">Zerx</h1>
  <p align="center">
    <strong>面向企业级 Java 应用开发的多模块 Maven 脚手架</strong>
  </p>
  <p align="center">
    <a href="#特性">特性</a> &middot; <a href="#项目结构">项目结构</a> &middot; <a href="#快速开始">快速开始</a> &middot; <a href="#模块说明">模块说明</a> &middot; <a href="#开发规范">开发规范</a>
  </p>
  <p align="center">
    <img src="https://img.shields.io/badge/JDK-21+-green" alt="JDK 21+">
    <img src="https://img.shields.io/badge/Spring_Boot-3.3.5-brightgreen" alt="Spring Boot 3.3.5">
    <img src="https://img.shields.io/badge/Maven-3.9+-orange" alt="Maven 3.9+">
    <img src="https://img.shields.io/badge/License-Apache_2.0-blue" alt="License">
  </p>
</p>

---

## 特性

**Zerx** 是一个轻量级、多模块的 Java 基础开发脚手架，采用 Core / Spring 分层架构设计，帮助团队快速构建企业级应用。

### 核心理念

- **Core / Spring 分层** — 核心层（zerx-core）保持零/轻量外部依赖，可脱离 Spring 独立使用；Spring 层（zerx-spring）通过自动配置提供胶水集成
- **零污染核心** — zerx-common 纯 JDK 实现，不引入任何第三方依赖，可在任何 Java 环境中使用
- **架构守护** — 基于 ArchUnit 的模块边界测试，在 CI 中自动拦截违规依赖
- **开箱即用的规范** — 集成 Checkstyle、SpotBugs、Maven Enforcer，从源头保证代码质量和风格一致
- **自动配置** — 所有 Spring 模块均通过 `@AutoConfiguration` 自动装配，引入 starter 即生效，零配置启动

### 技术栈

| 类别 | 技术选型 |
|------|---------|
| 语言 | Java 21（LTS，Virtual Threads、Record Pattern、Sequenced Collections） |
| 构建 | Maven 3.9+，多模块聚合 |
| 框架 | Spring Boot 3.3.5（zerx-spring 层） |
| 数据访问 | Spring Data JDBC |
| 缓存 | Caffeine + Redis（多级缓存） |
| 安全 | Spring Security + JWT（HS256 / RS256） |
| 日志 | SLF4J 2.x |
| 测试 | JUnit 5 + ArchUnit + H2 |
| 质量 | Checkstyle + SpotBugs + Maven Enforcer |

## 项目结构

```
zerx/
├── zerx-core/                              # 核心层（零/轻量依赖）
│   ├── zerx-common/                        #   通用工具 + 异常体系 + 基础类型（纯 JDK）
│   │   └── src/main/java/com/zerx/common/
│   │       ├── cache/                      #     LruCache 本地缓存
│   │       ├── concurrent/                 #     ThreadUtil + StopWatch
│   │       ├── constants/                  #     全局常量
│   │       ├── crypto/                     #     AES / HMAC / 摘要工具
│   │       ├── enums/                      #     基础枚举（BaseEnum、CommonStatus 等）
│   │       ├── event/                      #     领域事件（EventBus + DomainEvent）
│   │       ├── exception/                  #     统一异常体系（ZerxException + 5 子类）
│   │       ├── functional/                 #     Throwable 函数式接口（4 个）
│   │       ├── logging/                    #     LogHelper + LogRateLimiter + SensitiveLogFilter
│   │       ├── model/                      #     通用模型（Result、PageRequest、Pair 等）
│   │       ├── retry/                      #     Retryer 重试工具
│   │       └── util/                       #     工具类（18 个）
│   ├── zerx-architecture-test/             #   ArchUnit 架构规则测试（12 条规则，test-jar）
│   └── zerx-core-bom/                      #   核心层 BOM
├── zerx-spring/                            # Spring 层（自动配置）
│   ├── zerx-spring-web/                    #   Web 层增强
│   │   └── src/main/java/com/zerx/spring/web/
│   │       ├── advise/                     #     统一响应体 + 全局异常处理
│   │       ├── annotation/                 #     @ZerxResponseResult
│   │       ├── autoconfigure/              #     ZerxWebAutoConfiguration
│   │       ├── client/interceptor/         #     HTTP Client 拦截器（5 个）
│   │       ├── config/                     #     Jackson / CORS / HttpClient / 可观测性
│   │       ├── context/                    #     RequestContext 请求上下文
│   │       ├── filter/                     #     TraceFilter + AccessLogFilter
│   │       ├── interceptor/                #     RequestContextInterceptor
│   │       ├── sensitive/                  #     SensitiveDataMasker
│   │       └── properties/                 #     ZerxWebProperties
│   ├── zerx-spring-data/                   #   数据访问增强
│   │   └── src/main/java/com/zerx/spring/data/
│   │       ├── archive/                    #     归档机制（Archiver + JdbcArchiveRepository）
│   │       ├── audit/                      #     ZerxAuditorAware 审计桥接
│   │       ├── autoconfigure/              #     ZerxDataAutoConfiguration + ArchivePurgeTask
│   │       ├── config/                     #     SlowSqlInterceptor + CamelCaseNamingStrategy
│   │       ├── datascope/                  #     @DataScope 数据权限（AOP + ThreadLocal）
│   │       ├── domain/                     #     BaseEntity（ID + 审计 + 乐观锁）
│   │       ├── properties/                 #     ZerxDataProperties
│   │       ├── query/                      #     DynamicQuery 链式 SQL 构建器
│   │       ├── repository/                 #     ZerxRepository + ZerxRepositoryHelper
│   │       └── util/                       #     NamingUtils
│   ├── zerx-spring-cache/                  #   多级缓存
│   │   └── src/main/java/com/zerx/spring/cache/
│   │       ├── autoconfigure/              #     ZerxCacheAutoConfiguration
│   │       ├── config/                     #     CacheInvalidationListener
│   │       ├── ops/                        #     CacheOps + Caffeine / Redis / Multilevel
│   │       └── properties/                 #     ZerxCacheProperties
│   └── zerx-spring-security/               #   认证授权
│       └── src/main/java/com/zerx/spring/security/
│           ├── autoconfigure/              #     ZerxSecurityAutoConfiguration
│           ├── config/                     #     ZerxSecurityConfiguration
│           ├── filter/                     #     ZerxJwtAuthenticationFilter
│           ├── handler/                    #     AccessDenied + AuthenticationEntryPoint
│           ├── props/                      #     ZerxSecurityProperties
│           ├── service/                    #     ZerxPasswordService
│           └── token/                      #     JWT（HS256 / RS256）+ TokenPair + TokenClaims
├── docs/                                   # 项目文档
├── .editorconfig                           # 跨编辑器格式统一
├── .gitattributes                          # Git 行为统一
├── checkstyle.xml                          # Checkstyle 规则（30+ 条）
├── checkstyle-suppressions.xml             # Checkstyle 豁免规则
├── spotbugs-exclude.xml                    # SpotBugs 排除规则
├── CONTRIBUTING.md                         # 开发规范文档
└── pom.xml                                 # 父 POM（版本管理 + 插件配置）
```

## 快速开始

### 环境要求

- **JDK** 21 或更高版本
- **Maven** 3.9.0 或更高版本
- **IDE** IntelliJ IDEA（推荐）或 VS Code

### 构建

```bash
# 克隆项目
git clone https://github.com/forlor/zerx.git
cd zerx

# 编译
mvn clean compile

# 运行测试（含 ArchUnit 架构验证）
mvn clean test

# 完整构建（含 Checkstyle + SpotBugs）
mvn clean verify

# 安装到本地仓库
mvn clean install
```

### 使用

#### 引入核心层（零依赖）

```xml
<dependency>
    <groupId>com.zerx</groupId>
    <artifactId>zerx-common</artifactId>
    <version>1.0.0</version>
</dependency>
```

> zerx-common 零第三方依赖，引入后不会带来任何额外的 jar 包冲突。

#### 引入 Spring 层（自动配置）

```xml
<!-- 引入 Web 层（包含统一响应、全局异常、请求上下文等） -->
<dependency>
    <groupId>com.zerx</groupId>
    <artifactId>zerx-spring-web</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 引入数据访问层（Spring Data JDBC 增强） -->
<dependency>
    <groupId>com.zerx</groupId>
    <artifactId>zerx-spring-data</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 引入缓存层（Caffeine + Redis 多级缓存） -->
<dependency>
    <groupId>com.zerx</groupId>
    <artifactId>zerx-spring-cache</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 引入安全层（JWT 认证 + Spring Security） -->
<dependency>
    <groupId>com.zerx</groupId>
    <artifactId>zerx-spring-security</artifactId>
    <version>1.0.0</version>
</dependency>
```

> 所有 Spring 模块通过 `@AutoConfiguration` 自动装配，引入依赖即生效。

## 模块说明

### zerx-common

核心工具模块，纯 JDK 实现，提供企业级 Java 项目中最常用的基础能力。

**工具类（util，18 个）**

| 类名 | 说明 |
|------|------|
| `StringUtil` | 字符串处理：判空、截取、驼峰/下划线转换、脱敏模板、正则工具等 60+ 方法 |
| `CollectionUtil` | 集合处理：判空、分组、过滤、批量操作、流式工具等 40+ 方法 |
| `DateUtil` | 日期时间：格式化、解析、计算、时区转换、JDK 21 时间 API 封装等 50+ 方法 |
| `ReflectUtil` | 反射工具：字段访问、方法调用、注解解析，支持 JDK 21 Record 类型 |
| `UuidUtil` | UUID 生成：UUIDv7（RFC 9562 draft，标准/快速/单调模式）、UUIDv4 |
| `IoUtil` | IO 工具：流拷贝、文件读写、安全关闭 |
| `NumberUtil` | 数值工具：BigDecimal 精度运算、格式化、安全类型转换 |
| `AssertUtil` | 参数断言：notNull、notBlank、gt、lt、between、state |
| `EnumUtil` | 枚举工具：按 code/description 查找、toMap、toOptions（配合 BaseEnum） |
| `SensitiveDataUtil` | 数据脱敏：手机号、邮箱、身份证、银行卡、姓名、密码、IP 地址 |
| `ExceptionUtil` | 异常工具：堆栈提取、根因分析、异常链遍历 |
| `SystemUtil` | 系统环境：OS 判断、JDK 版本、JVM 内存、系统路径、主机信息 |
| `FileUtil` | 文件工具：路径处理、文件操作、文件类型判断 |
| `ArrayUtil` | 数组工具：判空、合并、翻转、包含检查 |
| `ConvertUtil` | 类型转换：基本类型、集合、Map 之间的安全转换 |
| `Base64Util` | Base64 编解码：标准 URL 安全模式 |
| `RandomUtil` | 随机数生成：字符串、数字、UUID、指定字符集 |
| `SnowflakeId` | 雪花算法分布式 ID 生成器 |

**扩展能力包**

| 包名 | 说明 |
|------|------|
| `crypto` | `AesUtil`（AES 加解密）、`HmacUtil`（HMAC 签名）、`DigestUtil`（MD5 / SHA 摘要） |
| `event` | `EventBus`（同步事件总线）、`DomainEvent`（领域事件基类）、`DomainEventListener`（注解驱动监听） |
| `concurrent` | `ThreadUtil`（线程池封装、虚拟线程）、`StopWatch`（耗时统计） |
| `retry` | `Retryer`（可配置重试策略：次数、间隔、退避、异常过滤） |
| `logging` | `LogHelper`（日志便捷方法）、`LogRateLimiter`（限频日志）、`SensitiveLogFilter`（敏感参数过滤） |
| `cache` | `LruCache`（轻量级 LRU 本地缓存，零依赖） |

**异常体系（exception）**

```
ZerxException (RuntimeException, 抽象基类)
├── BusinessException           (2xxxx, 业务逻辑异常)
├── ValidationException        (3xxxx, 参数校验异常, 支持字段级)
├── AuthorizationException     (4xxxx, 认证授权异常)
├── NotFoundException          (404, 资源不存在)
└── ExternalServiceException   (5xxxx, 外部服务异常, 支持 serviceName)
```

**通用模型（model）**

| 类名 | 说明 |
|------|------|
| `Result<T>` | 统一响应体，支持 code/message/data/timestamp |
| `PageRequest<T>` | 分页请求，支持排序（多字段、升降序） |
| `PageResult<T>` | 分页响应，支持 totalCount/pageSize/currentPage/data |
| `Pair<L, R>` | 二元组，支持 swap、toEntry、isFull |
| `Triple<F, S, T>` | 三元组，支持 firstTwo、lastTwo、split |
| `ValidationResult` | 累积式校验结果，支持收集多条错误信息 |

**函数式接口（functional）**

| 类名 | 说明 |
|------|------|
| `ThrowableFunction<T, R>` | 支持受检异常的 Function |
| `ThrowableConsumer<T>` | 支持受检异常的 Consumer |
| `ThrowableSupplier<T>` | 支持受检异常的 Supplier |
| `ThrowableRunnable` | 支持受检异常的 Runnable |

### zerx-architecture-test

基于 [ArchUnit](https://www.archunit.org/) 的架构规则测试模块，打包为 test-jar，可供所有模块复用。

**已实现的 12 条规则：**

| 分类 | 规则 |
|------|------|
| 依赖隔离 | common 不得依赖第三方包；core 层不得引入 Spring；common 不得引入 SLF4J |
| 包结构 | common 子包无循环依赖；util 内部包不对外暴露 |
| 类设计 | util 类必须是 final + 私有构造；公共枚举实现 BaseEnum |
| 异常体系 | 自定义异常继承 ZerxException；异常类不依赖 util |
| 命名规范 | util 类以 Util 结尾；functional 接口标注 @FunctionalInterface |
| 编码规范 | 禁止 System.out/err；禁止 sun.* 包 |

### zerx-spring-web

Web 层增强，提供统一响应封装、全局异常处理、请求上下文管理等企业级 Web 应用必备能力。

| 能力 | 说明 |
|------|------|
| `ZerxResponseBodyAdvice` | 基于 `@ZerxResponseResult` 注解的统一响应体自动封装 |
| `GlobalExceptionHandler` | 全局异常处理，自动映射 `ZerxException` 体系到 HTTP 状态码 |
| `RequestContext` | 请求上下文（userId、traceId、请求信息），ThreadLocal 持有，反射桥接供 data 层使用 |
| `TraceFilter` | 链路追踪过滤器，生成/传递 traceId |
| `AccessLogFilter` | 访问日志过滤器，记录请求方法、URI、耗时、状态码 |
| `JacksonAutoConfiguration` | Jackson 全局配置（Long → String、日期格式化、空值处理） |
| `ZerxCorsAutoConfiguration` | CORS 跨域配置，支持配置化 allowedOrigins |
| `SensitiveDataMasker` | 响应数据脱敏，支持手机号、邮箱、身份证等字段自动遮蔽 |
| `ZerxHttpClientAutoConfiguration` | HTTP Client 拦截器链：错误响应、重试、访问日志、敏感头、链路追踪传播 |
| `ZerxObservabilityAutoConfiguration` | Micrometer 可观测性集成（可选） |

### zerx-spring-data

数据访问增强，基于 Spring Data JDBC 提供聚合根基类、审计、归档、动态查询、数据权限、慢 SQL 检测等能力。

| 能力 | 说明 |
|------|------|
| `BaseEntity` | 聚合根基类：`@Id` + `@CreatedDate` / `@LastModifiedDate` + `@CreatedBy` / `@LastModifiedBy` + `@Version` 乐观锁 |
| `DynamicQuery` | 链式 SQL 构建器：12 种 WHERE 条件、OR 分组、JOIN、GROUP BY / HAVING、排序、分页、数据权限注入 |
| `@DataScope` | 数据权限注解 + AOP 拦截器 + `DataScopeHandler`（支持 SELF / DEPT / DEPT_AND_CHILD / ALL 四种策略，递归 CTE） |
| `Archiver<T>` | 归档机制：`BeforeDeleteCallback` 自动归档 + `JdbcArchiveRepository` 实现 + `ArchivePurgeTask` 定时清理过期数据 |
| `SlowSqlInterceptor` | 慢 SQL 检测：BeanPostProcessor 代理 JdbcTemplate，超阈值日志告警，敏感参数自动脱敏 |
| `ZerxRepositoryHelper` | 通用 Repository 工具：分页查询、批量存在性检查、总数统计 |
| `ZerxAuditorAware` | 审计桥接：反射获取 Web 层 `RequestContext.getUserId()`，无编译期依赖 |
| 命名策略 | 可配置 SNAKE_CASE / CAMEL_CASE，支持 Single Query Loading 解决 N+1 |

**配置项示例：**

```yaml
zerx:
  data:
    naming-strategy: SNAKE_CASE       # SNAKE_CASE 或 CAMEL_CASE
    single-query-loading: true        # 启用 Single Query Loading
    slow-sql:
      enabled: true                   # 启用慢 SQL 检测
      threshold: 1000ms               # 慢 SQL 阈值
      log-params: true                # 打印 SQL 参数
    archive:
      enabled: false                  # 启用归档（按需开启）
      table-suffix: "_archive"        # 归档表后缀
      entities: []                    # 需要归档的实体全限定类名
      retain-days: 90                 # 归档数据保留天数
      purge-cron: "0 0 3 * * ?"      # 过期清理 Cron
```

### zerx-spring-cache

多级缓存抽象，提供 Caffeine 本地缓存 + Redis 分布式缓存的无缝集成。

| 能力 | 说明 |
|------|------|
| `CacheOps` | 统一缓存操作接口：get、put、evict、clear |
| `CaffeineCacheOps` | Caffeine 本地缓存实现（L1） |
| `RedisCacheOps` | Redis 分布式缓存实现（L2，可选依赖） |
| `MultilevelCacheOps` | 多级缓存：L1 未命中自动查 L2，写入时双写 |
| `CacheInvalidationListener` | 缓存失效监听，支持事件驱动的缓存清除 |

> Redis 为可选依赖（`optional=true`），未引入 spring-boot-starter-data-redis 时自动退化为纯 Caffeine 本地缓存。

### zerx-spring-security

认证授权模块，基于 Spring Security 提供完整的 JWT 认证体系。

| 能力 | 说明 |
|------|------|
| `ZerxSecurityConfiguration` | SecurityFilterChain Lambda DSL 配置，支持配置化放行路径 |
| `ZerxTokenService` | JWT 令牌服务接口，支持生成 / 验证 / 解析 |
| `ZerxHs256TokenService` | HS256 对称加密 JWT 实现 |
| `ZerxRs256TokenService` | RS256 非对称加密 JWT 实现 |
| `ZerxTokenPair` | Access Token + Refresh Token 双令牌对 |
| `ZerxJwtAuthenticationFilter` | JWT 认证过滤器，自动从 Header 提取和验证令牌 |
| `ZerxPasswordService` | 密码加密服务，封装 BCrypt |
| `ZerxAccessDeniedHandler` | 自定义 403 响应处理 |
| `ZerxAuthenticationEntryPoint` | 自定义 401 响应处理 |

**配置项示例：**

```yaml
zerx:
  security:
    token:
      secret: "your-secret-key"       # HS256 密钥
      algorithm: HS256                # HS256 或 RS256
      access-token-expiration: 1h     # Access Token 有效期
      refresh-token-expiration: 7d    # Refresh Token 有效期
      issuer: zerx                    # Token 签发者
```

## 开发规范

本项目通过多层工具链保证代码风格一致性：

| 工具 | 作用 | 执行阶段 |
|------|------|---------|
| EditorConfig | 统一缩进（4 空格）、字符集（UTF-8）、换行符（LF） | IDE 打开时 |
| Checkstyle | 命名规范、注释、代码结构、禁止项 | `validate` |
| SpotBugs | 潜在 bug、性能问题、安全漏洞 | `verify` |
| Maven Enforcer | JDK 21+、Maven 3.9+、依赖约束 | `validate` |
| ArchUnit | 模块边界、包结构、类设计规范 | `test` |

详细的编码规范和 Git 提交规范请参阅 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 致谢

Zerx 的架构设计借鉴了以下优秀开源项目的理念：

- [Hutool](https://github.com/dromara/hutool) — 零依赖模块化工具库的拆分哲学
- [Spring Modulith](https://github.com/spring-projects/spring-modulith) — 模块边界自动检测
- [Apache ShardingSphere](https://github.com/apache/shardingsphere) — 微内核 + SPI 插件架构
- [Workday/base.build](https://github.com/Workday/base.build) — JPMS 模块化实践

## License

[Apache License 2.0](LICENSE)
