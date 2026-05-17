# zerx Spring 生态体系设计文档 v1.1

> **ZERX SPRING ECOSYSTEM DESIGN**
> 版本：v1.1.0 | 日期：2025-01-01 | 作者：Forlor
> 基于 v1.0 审查报告修订，修复全部 P0/P1 问题
> GitHub: github.com/forlor/zerx
> JDK 21 | Maven 3.9+ | Spring Boot 3.3+ | Jakarta EE 10 | Spring Data JDBC

---

## 目录

- [第一章 概述](#第一章-概述)
  - [1.1 文档目的](#11-文档目的)
  - [1.2 技术栈与版本约束](#12-技术栈与版本约束)
  - [1.3 设计原则](#13-设计原则)
  - [1.4 模块总览](#14-模块总览)
  - [1.5 模块间依赖关系](#15-模块间依赖关系)
- [第二章 Spring Boot 框架实用性封装](#第二章-spring-boot-框架实用性封装)
  - [2.1 Spring Data JDBC](#21-spring-data-jdbc)
  - [2.2 Spring Security](#22-spring-security)
  - [2.3 Spring Cache](#23-spring-cache)
  - [2.4 参数校验与全局异常处理](#24-参数校验与全局异常处理)
  - [2.5 统一 Web 响应与可观测性](#25-统一-web-响应与可观测性)
  - [2.6 事件驱动封装](#26-事件驱动封装)
  - [2.7 虚拟线程集成](#27-虚拟线程集成)
- [第三章 业务组件](#第三章-业务组件)
  - [3.1 短信验证码](#31-短信验证码)
  - [3.2 Excel 操作](#32-excel-操作)
  - [3.3 对象存储](#33-对象存储)
  - [3.4 分布式锁](#34-分布式锁)
  - [3.5 幂等性控制](#35-幂等性控制)
  - [3.6 限流熔断](#36-限流熔断)
- [第四章 模块依赖关系](#第四章-模块依赖关系)
  - [4.1 依赖分层](#41-依赖分层)
  - [4.2 框架模块间依赖](#42-框架模块间依赖)
  - [4.3 Maven 模块结构](#43-maven-模块结构)
- [第五章 实施路线](#第五章-实施路线)
  - [5.1 第一阶段 P0](#51-第一阶段-p0)
  - [5.2 第二阶段 P1](#52-第二阶段-p1)
  - [5.3 第三阶段 P2](#53-第三阶段-p2)

---

## 第一章 概述

### 1.1 文档目的

本文档是 zerx Spring 生态体系的正式架构设计说明书 v1.1 版本。v1.1 基于 v1.0 的外部审查报告进行了全面修订，核心目标是解决审查中发现的 7 项 P0 阻断性问题和 13 项 P1 优先级问题，确保框架在生产环境中的安全性、可靠性和可维护性达到企业级标准。

文档面向以下读者：核心框架开发者（理解模块边界与扩展点）、业务开发团队（了解如何按需引入和使用组件）、技术架构师（评估框架选型与技术决策）、安全审计人员（审查认证授权与密钥管理方案）。文档中每个章节均包含设计意图说明、关键 API 示例、配置项清单以及与替代方案的对比分析，力求让读者不仅知道"怎么做"，更要理解"为什么这样做"。

v1.1 相较于 v1.0 的重大变更包括：全面从 `javax.*` 迁移至 `jakarta.*` 命名空间以适配 Jakarta EE 10 规范；Spring Security 配置从传统的 `WebSecurityConfigurerAdapter` 继承模式全面切换为 `SecurityFilterChain` Lambda DSL 风格；数据访问层的复杂查询场景不再依赖第三方库（明确不引入 jOOQ），而是自研 `DynamicQuery` 链式 SQL 构建器并深度集成 `JdbcTemplate`；JWT 认证体系从密钥管理、Token 精简、Refresh Token 旋转到 RBAC 权限加载进行了全方位重构；缓存层面引入了基于 Redis Pub/Sub 的多级缓存一致性方案以及完整的击穿/雪崩/穿透防护机制；数据访问层彻底移除了 `CrudServiceTemplate`，回归 Repository 直用模式。这些变更在后续章节中逐一详述。

---

### 1.2 技术栈与版本约束

zerx Spring 生态体系对底层技术栈有明确的版本约束，以确保所有模块在统一环境下经过充分测试。以下版本矩阵是框架运行的最低要求，未列出的传递依赖版本由 Spring Boot BOM 和 zerx BOM 统一管理，禁止业务项目手动覆盖。

| 技术组件 | 版本约束 | 用途说明 | 备注 |
|----------|---------|----------|------|
| **JDK** | **21 (LTS)** | 运行时环境 | 虚拟线程、Record、Sealed Class、Pattern Matching |
| **Maven** | 3.9+ | 构建工具 | 多模块聚合、BOM 管理 |
| **Spring Boot** | 3.3+ | 应用框架 | 自动配置、Actuator、Starter 体系 |
| **Spring Framework** | 6.2+ | 核心框架 | `SecurityFilterChain` Lambda DSL |
| **Jakarta EE** | **10** | 规范命名空间 | `jakarta.servlet`、`jakarta.validation`、`jakarta.persistence` |
| **Spring Data JDBC** | 3.3+ | 数据访问 | 聚合根、审计、Single Query Loading |
| **Micrometer** | **1.13+** | 可观测性 | Observation API + Prometheus + OTel |
| **Micrometer Tracing** | **1.3+** | 分布式链路追踪 | OpenTelemetry Bridge |
| **jjwt** | 0.12.6 | JWT 签发/验证 | 支持 RS256 非对称签名 |
| **Caffeine** | 3.1.8 | 本地缓存 (L1) | 高性能、近最优淘汰策略 |
| **Bucket4j** | 8.7.0 | 本地限流 (L1) | 令牌桶/滑动窗口 |
| **Redisson** | 3.35.0 | Redis 客户端 | 分布式锁、限流、Pub/Sub |
| **EasyExcel** | **≥3.3.x** | Excel 操作 | Jakarta 兼容版本（≥3.3.0） |
| **Resilience4j** | 2.2+ | 熔断降级 | CircuitBreaker + FallbackHandler |
| **SpringDoc OpenAPI** | 2.6.0 | API 文档 | OpenAPI 3.0、Swagger UI |
| **BCrypt** | Spring Security 内置 | 密码存储 | work factor ≥ 12 |

**javax → jakarta 迁移说明（P0-1 修复）**：Spring Boot 3.x 全面切换至 Jakarta EE 10 命名空间。所有 `javax.servlet.*`、`javax.validation.*`、`javax.persistence.*` 包名必须替换为对应的 `jakarta.*` 前缀。zerx 所有 Starter 的自动配置类、Filter、Interceptor、Validator 均已使用 `jakarta.*` API。业务项目从旧版（Spring Boot 2.x / javax）迁移至 zerx v1.1 时，需全局替换 import 语句；对于第三方库仍使用 javax 的情况（如旧版 Hibernate Validator），需通过 BOM 强制升级至 Jakarta 版本。框架在启动时会通过 `ClassLoader` 检测是否存在残留的 javax 依赖并打印 WARN 日志。

**Micrometer 新增（P1-10 修复）**：v1.1 正式引入 Micrometer Observation API 作为全链路可观测性的核心抽象。所有 HTTP 请求、缓存操作、数据库查询、外部调用均通过 `ObservationRegistry` 创建 Span，自动传递 TraceID 和 Baggage。Micrometer 同时负责将指标数据导出至 Prometheus，将链路数据桥接至 OpenTelemetry 后端（如 Jaeger、Zipkin）。

---

### 1.3 设计原则

zerx Spring 生态体系遵循以下核心设计原则，这些原则贯穿所有模块的架构决策和 API 设计：

**注解驱动，业务零侵入**：所有框架级能力（缓存、日志、限流、幂等、分布式锁）通过自定义注解 + AOP 切面实现，业务代码仅需在方法或类上添加注解即可启用。例如 `@ZerxCacheable`、`@ZerxRateLimiter`、`@ZerxDistributedLock`、`@ZerxIdempotent`。注解的属性设计遵循"合理默认值"原则，大部分场景零配置即可工作。

**显式优于隐式**：借鉴 Spring Data JDBC 的设计哲学，拒绝 JPA 式的自动脏检查和隐式持久化。所有缓存操作遵循 Cache-Aside 模式（显式读、显式写、显式删），所有权限加载按需从 Redis 获取而非预嵌入 JWT，所有安全配置通过 `SecurityFilterChain` Lambda DSL 显式声明。开发者对每一条 SQL、每一个缓存操作、每一次鉴权都有清晰的认知。

**安全左移**：安全设计从框架层开始内置，而非事后补丁。密码存储强制 BCrypt work factor ≥12；JWT 支持非对称 RS256 签名且密钥管理覆盖生成/轮换/泄露应急/双密钥过渡/多环境隔离全生命周期；多级缓存通过 Redis Pub/Sub 保证一致性；令牌黑名单在集群环境下强制 Redis 存储；HTTP 安全头通过统一配置注入。

**单一职责与按需取用**：每个模块对应一个独立的 Spring Boot Starter JAR，模块间通过接口而非实现类通信。业务项目可以只引入 `zerx-spring-web` 而不引入 `zerx-spring-security`，也可以只用 `zerx-component-excel` 而不引入任何安全模块。零依赖的 `zerx-common` 可以脱离 Spring 在任何 Java 21 项目中独立使用。

**可观测性内置**：通过 Micrometer Observation API，所有框架级操作（HTTP 请求处理、缓存读写、数据库查询、限流拦截、熔断降级）自动产生 Span 和 Metrics，无需业务代码手动埋点。TraceID 从网关层透传至数据库层，全链路可追踪。

---

### 1.4 模块总览

zerx Spring 生态体系包含 12 个模块，分为框架封装层（7 个）和业务组件层（5 个核心 + 7 个扩展，此处列出 12 个已确认核心模块）。每个模块均为独立的 Maven artifact，遵循 Spring Boot Starter 规范。

| 序号 | 模块名称 | 所属层 | 主要依赖 | 核心能力 | 优先级 |
|:----:|----------|:------:|----------|----------|:------:|
| 1 | `zerx-spring-web` | 框架封装 | `spring-boot-starter-web`、`zerx-common`、`micrometer` | 统一响应 `ZerxResult<T>`、全局异常处理、参数校验增强、请求上下文 | P0 |
| 2 | `zerx-spring-data` | 框架封装 | `spring-boot-starter-data-jdbc`、`zerx-common` | `BaseEntity`、审计、逻辑删除、多数据源、`DynamicQuery` 链式构建器、`JdbcTemplate` 集成 | P0 |
| 3 | `zerx-spring-security` | 框架封装 | `spring-boot-starter-security`、`jjwt`、`zerx-spring-web` | `SecurityFilterChain` Lambda DSL、JWT (HS256/RS256)、RBAC、Refresh Token 旋转、账号安全 | P0 |
| 4 | `zerx-spring-cache` | 框架封装 | `spring-boot-starter-cache`、`caffeine`、`spring-boot-starter-data-redis` | Cache-Aside、Redis Pub/Sub 一致性、击穿/雪崩/穿透防护、`CacheOps` 工具类 | P0 |
| 5 | `zerx-spring-monitor` | 框架封装 | `spring-boot-starter-actuator`、`micrometer-registry-prometheus`、`micrometer-tracing-bridge-otel` | 自定义 HealthCheck、Prometheus 指标、OTel 链路追踪 | P1 |
| 6 | `zerx-spring-doc` | 框架封装 | `springdoc-openapi-starter-webmvc-ui`、`zerx-spring-web` | OpenAPI 3.0 文档生成、Swagger UI、分组配置 | P1 |
| 7 | `zerx-spring-logging` | 框架封装 | `zerx-spring-web`、`zerx-common` | TraceID 生成与传播、请求/响应日志、敏感参数脱敏、慢请求告警 | P1 |
| 8 | `zerx-component-excel` | 业务组件 | `easyexcel`（≥3.3.x Jakarta）、`zerx-common` | 大数据量导入导出、校验监听器、字典翻译、加密列 | P1 |
| 9 | `zerx-component-oss` | 业务组件 | `zerx-common`、各厂商 SDK（可选） | 统一存储接口、策略模式多厂商适配（MinIO/阿里云/腾讯/S3） | P1 |
| 10 | `zerx-component-lock` | 业务组件 | `redisson`、`zerx-spring-cache` | 分布式锁（RedLock 风险缓解 + 指数退避 + 可观测性） | P1 |
| 11 | `zerx-component-idempotent` | 业务组件 | `zerx-spring-cache`、`zerx-spring-web` | 幂等性控制（Token 机制 + Redis 存储 + AOP 切面） | P2 |
| 12 | `zerx-component-ratelimit` | 业务组件 | `bucket4j`、`zerx-spring-cache`、`resilience4j` | 双层限流（Bucket4j L1 + Redis L2）、熔断 FallbackHandler | P1 |

---

### 1.5 模块间依赖关系

模块间严格遵循单向依赖原则。`zerx-spring-web` 是框架层的"粘合剂"模块，被 `zerx-spring-security` 和所有业务组件依赖；`zerx-spring-cache` 被 `zerx-spring-security`（Token 黑名单/权限缓存）和多个业务组件依赖。关键依赖链路如下：

```
zerx-common (零依赖，最底层)
    ▲
    │
zerx-spring-web ──────────────────────────────────┐
    ▲        │                                     │
    │        ▼                                     ▼
    │   zerx-spring-security ◄── zerx-spring-cache ◄─── zerx-component-lock
    │        │                  ▲                     zerx-component-idempotent
    │        ▼                  │                     zerx-component-ratelimit
    │   zerx-spring-data       │
    │        │                  │
    │        ▼                  │
    │   zerx-spring-monitor ───┘
    │        │
    │        ▼
    │   zerx-spring-logging
    │
    ▼
zerx-component-excel
zerx-component-oss
```

**核心依赖规则**：

1. **`zerx-spring-web` ← `zerx-spring-security` ← 业务代码**：Web 模块提供统一响应体和全局异常处理；Security 模块在此基础上叠加认证授权；业务 Controller 继承 Security 模块的用户上下文。
2. **`zerx-spring-cache` 被两者依赖**：Security 模块依赖 Cache 模块实现 Token 黑名单（强制 Redis）和权限按需加载；业务组件依赖 Cache 实现分布式锁、幂等性 Token 存储、限流计数器。
3. **禁止循环依赖**：`zerx-spring-web` 不依赖 `zerx-spring-security`，`zerx-spring-cache` 不依赖 `zerx-spring-web`。跨模块通信通过事件（`ApplicationEvent`）或回调接口。
4. **`zerx-spring-data` 独立于安全链路**：数据访问层不感知认证授权上下文（审计字段 `createBy`/`updateBy` 通过 `AuditorAware` 桥接，但不直接依赖 Security 模块），确保缓存穿透测试和单元测试可以脱离安全环境运行。

---

## 第二章 Spring Boot 框架实用性封装

### 2.1 Spring Data JDBC

#### 2.1.1 选型说明（含不引入 jOOQ 决策）

zerx 数据访问层选用 **Spring Data JDBC** 作为核心 ORM 方案，不使用 MyBatis/MyBatis-Plus、Spring Data JPA 或 jOOQ。选型理由已在 v1.0 中详述（轻量无代理、DDD 聚合根原生支持、显式持久化、无 N+1 陷阱），此处补充关于复杂查询方案的决策。

**为什么不引入 jOOQ**（P0-3 修复）：jOOQ 是优秀的类型安全 SQL 构建库，但在 zerx 场景下存在以下问题：（1）许可证限制：jOOQ 的类型安全 DSL 和代码生成在开源许可证下不可用，仅 Open Source Edition 支持部分开源数据库；（2）与 Spring Data JDBC 的集成不够自然——Spring Data JDBC 通过 `@Query` 注解或派生查询工作，jOOQ 有自己的 `DSLContext` 和 `Record` 映射体系，两套体系混用会增加认知负担；（3）引入 jOOQ 等于在 Spring Data JDBC 之外又维护一套 SQL 抽象层，违背"轻量"设计原则。因此 v1.1 决定自研 `DynamicQuery` 链式构建器（§2.1.7）配合 Spring Data JDBC 内置的 `JdbcTemplate`（§2.1.8）来覆盖所有复杂查询场景。

**技术决策矩阵**：

| 方案 | 类型安全 | 动态条件 | 学习成本 | 许可证 | 与 Spring Data JDBC 集成 |
|------|:--------:|:--------:|:--------:|:------:|:------------------------:|
| Spring Data JDBC 派生查询 | ✅ | ❌ | 低 | Apache 2.0 | 原生 |
| `@Query` 注解 | ❌ | ✅ (SpEL) | 低 | Apache 2.0 | 原生 |
| **自研 DynamicQuery** | 部分 | ✅ | 低 | 自有 | 原生（JdbcTemplate） |
| MyBatis-Plus | 部分 | ✅ | 中 | Apache 2.0 | 需额外集成 |
| jOOQ | ✅ | ✅ | 中高 | 双许可证 | 需额外集成 |

#### 2.1.2 架构设计

zerx-spring-data 的架构分为四层：

```
┌──────────────────────────────────────────────────┐
│ 业务 Service 层                                   │
│  注入 Repository 或 DynamicQuery                  │
├──────────────────────────────────────────────────┤
│ Repository 层                                     │
│  extends CrudRepository + ZerxRepository Fragment  │
│  派生查询 / @Query / DTO 投影                     │
├──────────────────────────────────────────────────┤
│ DynamicQuery 链式构建器（新增）                    │
│  类型安全的条件拼接 → 生成 SQL + 参数              │
│  底层委托 JdbcTemplate 执行                       │
├──────────────────────────────────────────────────┤
│ 基础设施层                                        │
│  BaseEntity / Auditing / SoftDelete / MultiDS     │
│  JdbcAggregateTemplate / EntityCallback           │
└──────────────────────────────────────────────────┘
```

**移除 CrudServiceTemplate（P0-7 修复）**：v1.0 中曾设计过 `CrudServiceTemplate` 泛型基类，试图为 Service 层提供通用 CRUD 模板方法。审查发现该设计违反了"显式优于隐式"原则——开发者无法直观理解继承带来的隐式行为，且模板方法限制了业务逻辑的灵活性。v1.1 彻底移除 `CrudServiceTemplate`，Service 层直接注入 Repository 使用。对于确实需要复用的批量操作（如批量保存、存在性检查），通过 `ZerxRepository` Fragment 接口提供静态工具方法，业务 Service 按需调用而非继承。

#### 2.1.3 BaseEntity 聚合根基类

所有持久化实体继承 `BaseEntity`，提供统一的 ID 策略、审计字段和逻辑删除支持。BaseEntity 使用 Jakarta Persistence 注解（`jakarta.persistence.Id` 等），并兼容 Spring Data JDBC 的映射规则。

```java
package com.zerx.spring.data.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

/**
 * 聚合根基类。所有 Spring Data JDBC 实体必须继承此类。
 * 使用 Jakarta 命名空间注解。
 */
public abstract class BaseEntity {

    @Id
    private Long id;

    @CreatedDate
    private LocalDateTime createTime;

    @LastModifiedDate
    private LocalDateTime updateTime;

    @org.springframework.data.annotation.CreatedBy
    private Long createBy;

    @org.springframework.data.annotation.LastModifiedBy
    private Long updateBy;

    @ReadOnlyProperty
    private Boolean deleted = false;

    // --- 行为方法（非 setter，保护不变量）---

    /** 标记逻辑删除。由 SoftDeleteCallback 触发，业务代码不应直接调用。 */
    public void markDeleted() {
        this.deleted = true;
    }

    // --- getter/setter 仅限框架内部使用 ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public Long getCreateBy() { return createBy; }
    public Long getUpdateBy() { return updateBy; }
    public Boolean getDeleted() { return deleted; }
}
```

**建模规范**：
1. 实体类名与表名遵循 `UpperCamelCase` → `snake_case` 自动映射（可通过 `@Table` 显式指定）。
2. 子实体（非聚合根）**不继承** `BaseEntity`，使用普通 POJO + `@MappedCollection` 注解。
3. 值对象使用 JDK 21 `record`，通过 `@MappedCollection` 嵌入聚合根。
4. 避免使用 Lombok `@Data`（与代理和缓存冲突），推荐 `@Getter` + `@Setter` + `@EqualsAndHashCode(of = "id")`。

#### 2.1.4 Repository 设计

Repository 层采用 Spring Data JDBC 的 Fragment 模式，将通用查询能力抽取到 `ZerxRepository` 接口，业务 Repository 同时继承 `CrudRepository` 和 `ZerxRepository`。

```java
package com.zerx.spring.data.repository;

import org.springframework.data.repository.NoRepositoryBean;
import java.util.List;

/**
 * zerx 通用 Repository Fragment 接口。
 * 提供逻辑删除感知的通用查询方法。
 */
@NoRepositoryBean
public interface ZerxRepository<T, ID> {
    /** 根据ID查询（自动过滤已删除记录） */
    Optional<T> findByIdAndDeletedFalse(ID id);
    /** 查询所有未删除记录 */
    List<T> findAllByDeletedFalse();
    /** 统计未删除记录数 */
    long countByDeletedFalse();
    /** 判断是否存在（自动过滤已删除） */
    boolean existsByIdAndDeletedFalse(ID id);
}
```

```java
package com.zerx.spring.data.repository;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

/**
 * 业务 Repository 示例。
 */
public interface UserRepository extends CrudRepository<User, Long>,
    ZerxRepository<User, Long> {

    // 派生查询（Spring Data JDBC 自动实现）
    Optional<User> findByUsername(String username);
    Optional<User> findByPhone(String phone);
    boolean existsByUsername(String username);
    List<User> findByStatusOrderByCreateTimeDesc(String status);

    // 自定义查询（DTO 投影）
    @Query("SELECT id, username, email, phone FROM sys_user WHERE status = :status AND deleted = 0")
    List<UserSummary> findSummariesByStatus(@Param("status") String status);

    // 分页查询（Spring Data JDBC 原生支持）
    @Query("SELECT * FROM sys_user WHERE deleted = 0 ORDER BY create_time DESC LIMIT :limit OFFSET :offset")
    List<User> findPage(@Param("limit") int limit, @Param("offset") int offset);
}
```

#### 2.1.5 多数据源

支持主从读写分离和异构数据源（如 MySQL + PostgreSQL）的场景。通过 `@ZerxDataSource` 注解在 Service 方法级别切换数据源，底层使用 Spring 的 `AbstractRoutingDataSource` 实现。

```java
/**
 * 数据源切换注解。标注在 Service 方法上，运行时切换至指定数据源。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ZerxDataSource {
    /** 数据源名称，对应配置中的 key */
    String value();
}

// 使用示例
@Service
public class OrderService {
    @ZerxDataSource("slave")  // 读从库
    public Order getById(Long id) { ... }

    @ZerxDataSource("master") // 写主库（默认，可省略）
    public void create(Order order) { ... }
}
```

#### 2.1.6 SQL 日志与慢查询检测

通过包装 `JdbcTemplate` 和 Spring Data JDBC 的 `StatementInterceptor`，自动记录所有执行的 SQL 语句及耗时。超过配置阈值的 SQL 以 WARN 级别输出，并记录完整的参数绑定信息（敏感参数自动脱敏）。

```yaml
zerx:
  data:
    slow-sql:
      enabled: true
      threshold: 1000ms        # 慢 SQL 阈值
      log-params: true          # 是否记录参数值
      sensitive-params:         # 敏感参数脱敏
        - password
        - token
        - secret
```

#### 2.1.7 DynamicQuery 链式构建器（P0-3 新增）

`DynamicQuery` 是 zerx 自研的动态 SQL 构建器，用于解决 Spring Data JDBC 在复杂查询场景（多条件动态拼接、多表关联、聚合统计）下的能力不足。DynamicQuery 底层委托 `JdbcTemplate` 执行，与 Spring Data JDBC 共享同一个 `DataSource`，保证事务一致性。

**设计目标**：
- 类型安全的条件拼接，避免手写字符串 SQL 时的注入风险和拼写错误。
- 支持 `WHERE`、`ORDER BY`、`GROUP BY`、`HAVING`、`LIMIT/OFFSET` 全子句动态构建。
- 条件可选——只有参数非空时才追加对应条件，实现动态查询。
- 支持多表 JOIN（INNER/LEFT/RIGHT）。
- 返回 `Map<String, Object>`、实体对象或自定义 DTO。

```java
package com.zerx.spring.data.query;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 链式 SQL 构建器。所有 SQL 参数通过 PreparedStatement 绑定，防止 SQL 注入。
 * 线程安全（无状态），每次构建创建新实例。
 */
public class DynamicQuery {

    private final JdbcTemplate jdbcTemplate;
    private final StringBuilder sql = new StringBuilder();
    private final List<Object> params = new ArrayList<>();
    private boolean whereAppended = false;
    private final String table;

    private DynamicQuery(JdbcTemplate jdbcTemplate, String table) {
        this.jdbcTemplate = jdbcTemplate;
        this.table = table;
        this.sql.append("SELECT * FROM ").append(table);
    }

    public static DynamicQuery from(JdbcTemplate jdbcTemplate, String table) {
        return new DynamicQuery(jdbcTemplate, table);
    }

    // ─── SELECT 子句 ───
    public DynamicQuery select(String... columns) {
        sql.setLength(0); // 清除默认 SELECT *
        sql.append("SELECT ").append(String.join(", ", columns)).append(" FROM ").append(table);
        return this;
    }

    // ─── WHERE 条件（可选追加）───
    public DynamicQuery eq(String column, Object value) {
        if (value != null) {
            appendWhere();
            sql.append(column).append(" = ?");
            params.add(value);
        }
        return this;
    }

    public DynamicQuery ne(String column, Object value) {
        if (value != null) {
            appendWhere();
            sql.append(column).append(" != ?");
            params.add(value);
        }
        return this;
    }

    public DynamicQuery like(String column, String value) {
        if (value != null && !value.isBlank()) {
            appendWhere();
            sql.append(column).append(" LIKE ?");
            params.add("%" + value + "%");
        }
        return this;
    }

    public DynamicQuery in(String column, Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            appendWhere();
            String placeholders = values.stream().map(v -> "?").collect(Collectors.joining(","));
            sql.append(column).append(" IN (").append(placeholders).append(")");
            params.addAll(values);
        }
        return this;
    }

    public DynamicQuery between(String column, Object min, Object max) {
        if (min != null && max != null) {
            appendWhere();
            sql.append(column).append(" BETWEEN ? AND ?");
            params.add(min);
            params.add(max);
        }
        return this;
    }

    public DynamicQuery ge(String column, Object value) {
        if (value != null) {
            appendWhere();
            sql.append(column).append(" >= ?");
            params.add(value);
        }
        return this;
    }

    public DynamicQuery le(String column, Object value) {
        if (value != null) {
            appendWhere();
            sql.append(column).append(" <= ?");
            params.add(value);
        }
        return this;
    }

    public DynamicQuery isNull(String column) {
        appendWhere();
        sql.append(column).append(" IS NULL");
        return this;
    }

    public DynamicQuery isNotNull(String column) {
        appendWhere();
        sql.append(column).append(" IS NOT NULL");
        return this;
    }

    public DynamicQuery raw(String clause, Object... values) {
        appendWhere();
        sql.append(clause);
        if (values != null) params.addAll(List.of(values));
        return this;
    }

    // ─── ORDER BY ───
    public DynamicQuery orderBy(String column, boolean asc) {
        sql.append(" ORDER BY ").append(column).append(asc ? " ASC" : " DESC");
        return this;
    }

    // ─── GROUP BY / HAVING ───
    public DynamicQuery groupBy(String... columns) {
        sql.append(" GROUP BY ").append(String.join(", ", columns));
        return this;
    }

    public DynamicQuery having(String clause, Object... values) {
        sql.append(" HAVING ").append(clause);
        if (values != null) params.addAll(List.of(values));
        return this;
    }

    // ─── 分页 ───
    public DynamicQuery limit(int limit) {
        sql.append(" LIMIT ?");
        params.add(limit);
        return this;
    }

    public DynamicQuery offset(int offset) {
        sql.append(" OFFSET ?");
        params.add(offset);
        return this;
    }

    // ─── JOIN（多表关联）───
    public DynamicQuery leftJoin(String table, String onClause, Object... params) {
        sql.append(" LEFT JOIN ").append(table).append(" ON ").append(onClause);
        if (params != null) this.params.addAll(List.of(params));
        return this;
    }

    public DynamicQuery innerJoin(String table, String onClause, Object... params) {
        sql.append(" INNER JOIN ").append(table).append(" ON ").append(onClause);
        if (params != null) this.params.addAll(List.of(params));
        return this;
    }

    // ─── 执行查询 ───
    public List<Map<String, Object>> list() {
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public <T> List<T> list(RowMapper<T> rowMapper) {
        return jdbcTemplate.query(sql.toString(), rowMapper, params.toArray());
    }

    public <T> Optional<T> one(RowMapper<T> rowMapper) {
        List<T> result = list(rowMapper);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    public long count() {
        // 重写为 SELECT COUNT(*)
        String countSql = "SELECT COUNT(*) FROM (" + sql + ") _cnt";
        Long count = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        return count != null ? count : 0;
    }

    // ─── 调试 ───
    public String getSql() { return sql.toString(); }
    public Object[] getParams() { return params.toArray(); }

    // ─── 内部方法 ───
    private void appendWhere() {
        if (!whereAppended) {
            sql.append(" WHERE ");
            whereAppended = true;
        } else {
            sql.append(" AND ");
        }
    }
}
```

**使用示例**：

```java
@Service
public class UserQueryService {
    private final JdbcTemplate jdbcTemplate;

    /**
     * 多条件动态查询用户列表。
     * 只有参数非空时才追加对应 WHERE 条件。
     */
    public List<UserVO> searchUsers(UserSearchDTO dto) {
        return DynamicQuery.from(jdbcTemplate, "sys_user")
            .select("id", "username", "email", "phone", "status", "create_time")
            .eq("status", dto.getStatus())
            .like("username", dto.getKeyword())
            .like("phone", dto.getPhone())
            .between("create_time", dto.getStartTime(), dto.getEndTime())
            .eq("deleted", 0)
            .orderBy("create_time", false)
            .limit(dto.getPageSize())
            .offset((dto.getPageNum() - 1) * dto.getPageSize())
            .list(new UserVORowMapper());
    }

    /**
     * 多表关联统计报表。
     */
    public List<MonthlySalesDTO> monthlyReport(int year) {
        return DynamicQuery.from(jdbcTemplate, "orders o")
            .select("DATE_FORMAT(o.create_time, '%Y-%m') AS month",
                    "COUNT(*) AS order_count",
                    "SUM(o.amount) AS total_amount")
            .innerJoin("users u", "u.id = o.user_id")
            .eq("YEAR(o.create_time)", year)
            .eq("o.deleted", 0)
            .eq("o.status", "PAID")
            .groupBy("DATE_FORMAT(o.create_time, '%Y-%m')")
            .orderBy("month", true)
            .list(new MonthlySalesRowMapper());
    }
}
```

#### 2.1.8 JdbcTemplate 透明集成

Spring Data JDBC 与 `JdbcTemplate` 共享同一个 `DataSource`，天然支持在同一事务中混用。在 zerx 中，`JdbcTemplate` 作为"复杂查询的最终兜底方案"存在——当 DynamicQuery 链式构建器无法满足需求时（如窗口函数、递归 CTE、存储过程调用），开发者可以直接注入 `JdbcTemplate` 编写原生 SQL。

```java
@Service
public class ComplexQueryService {
    private final JdbcTemplate jdbcTemplate;

    // 窗口函数：用户消费排名
    public List<ConsumptionRankDTO> getConsumptionRank(int topN) {
        return jdbcTemplate.query("""
            SELECT user_id, username, total_amount,
                   RANK() OVER (ORDER BY total_amount DESC) AS rank_num
            FROM (
                SELECT u.id AS user_id, u.username, SUM(o.amount) AS total_amount
                FROM orders o
                INNER JOIN sys_user u ON u.id = o.user_id
                WHERE o.deleted = 0 AND o.status = 'PAID'
                GROUP BY u.id, u.username
            ) t
            LIMIT ?
            """, new ConsumptionRankRowMapper(), topN);
    }

    // 存储过程调用
    public void callMonthlySettlement(int year, int month) {
        jdbcTemplate.execute("{call sp_monthly_settlement(?, ?)}",
            (CallableStatementCallback<Void>) cs -> {
                cs.setInt(1, year);
                cs.setInt(2, month);
                cs.execute();
                return null;
            });
    }
}
```

**集成要点**：
- 事务共享：`JdbcTemplate` 操作与 Spring Data JDBC 的 `save()`/`delete()` 在同一个 `@Transactional` 事务中。
- 连接池共享：两者使用同一个 HikariCP 连接池，不会产生额外连接开销。
- SQL 日志共享：JdbcTemplate 执行的 SQL 同样经过慢查询检测和敏感参数脱敏。
- DynamicQuery 底层就是 JdbcTemplate 的封装，不存在两套执行路径。

---

### 2.2 Spring Security

#### 2.2.1 SecurityFilterChain Lambda DSL（P0-2 修复）

v1.1 将安全配置从传统的 `WebSecurityConfigurerAdapter` 继承模式（已在 Spring Security 6.x 中废弃）全面迁移为 `SecurityFilterChain` Lambda DSL 风格。Lambda DSL 的优势在于：代码更简洁直观、IDE 自动补全友好、Filter 链的顺序和条件一目了然、避免因 `WebSecurityConfigurerAdapter` 被移除导致的升级风险。

```java
package com.zerx.spring.security.config;

import com.zerx.spring.security.filter.ZerxJwtAuthenticationFilter;
import com.zerx.spring.security.handler.ZerxAuthenticationEntryPoint;
import com.zerx.spring.security.handler.ZerxAccessDeniedHandler;
import com.zerx.spring.security.props.ZerxSecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class ZerxSecurityConfiguration {

    private final ZerxJwtAuthenticationFilter jwtAuthFilter;
    private final ZerxAuthenticationEntryPoint authEntryPoint;
    private final ZerxAccessDeniedHandler accessDeniedHandler;
    private final ZerxSecurityProperties props;

    public ZerxSecurityConfiguration(
            ZerxJwtAuthenticationFilter jwtAuthFilter,
            ZerxAuthenticationEntryPoint authEntryPoint,
            ZerxAccessDeniedHandler accessDeniedHandler,
            ZerxSecurityProperties props) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authEntryPoint = authEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.props = props;
    }

    @Bean
    public SecurityFilterChain zerxSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            // ─── CSRF：无状态 JWT 模式下禁用 ───
            .csrf(AbstractHttpConfigurer::disable)

            // ─── CORS：使用配置源 ───
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ─── Session：无状态，不创建 HTTP Session ───
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ─── HTTP 安全头（P1-4 修复）───
            .headers(headers -> headers
                .contentTypeOptions(contentType -> {}  // X-Content-Type-Options: nosniff
                )
                .frameOptions(frame -> frame.deny())   // X-Frame-Options: DENY
                .xssProtection(xss -> xss.headerValue(
                    org.springframework.security.config.annotation.web.configurers
                        .headers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
            )

            // ─── 请求授权规则 ───
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(props.getPermitUrls().toArray(new String[0]))
                    .permitAll()
                .requestMatchers("/actuator/health", "/actuator/info")
                    .permitAll()
                .requestMatchers("/actuator/**")
                    .hasRole("ADMIN")
                .anyRequest()
                    .authenticated()
            )

            // ─── 异常处理 ───
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authEntryPoint)    // 未认证 → 401
                .accessDeniedHandler(accessDeniedHandler)     // 无权限 → 403
            )

            // ─── 添加 JWT Filter（在 UsernamePasswordAuthenticationFilter 之前）───
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(props.getCors().getAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

#### 2.2.2 核心接口 Zerx 前缀 + TokenService 接口化（P1-7/P1-8 修复）

所有 zerx 自定义的安全接口统一使用 `Zerx` 前缀命名，避免与 Spring Security 原生类混淆。`TokenService` 从具体实现类抽取为接口，支持不同签名算法（HS256/RS256）的灵活切换。

```java
package com.zerx.spring.security.token;

import java.time.Instant;
import java.util.Optional;

/**
 * Token 服务接口（P1-7 修复：接口化）。
 * 支持对称（HS256）和非对称（RS256）签名算法的实现切换。
 */
public interface ZerxTokenService {

    /** 生成 Access Token */
    String generateAccessToken(Long userId, String jti);

    /** 生成 Refresh Token */
    String generateRefreshToken(Long userId, String jti);

    /** 解析 Token，返回 ZerxTokenClaims */
    ZerxTokenClaims parseToken(String token);

    /** 验证 Token 是否有效（签名、过期、黑名单） */
    boolean validateToken(String token);

    /** 将 Token 加入黑名单 */
    void blacklistToken(String jti, Instant expiresAt);

    /** 判断 Token 是否在黑名单中 */
    boolean isBlacklisted(String jti);
}

/**
 * Token 声明（JWT Payload 精简版，P1-1 修复）。
 * 仅包含 userId 和 jti（JWT ID），权限从 Redis 按需加载。
 */
public record ZerxTokenClaims(
    Long userId,
    String jti,
    Instant issuedAt,
    Instant expiresAt,
    String tokenType  // "access" | "refresh"
) {}

/**
 * Token 生成结果。
 */
public record ZerxTokenPair(
    String accessToken,
    String refreshToken,
    long accessExpiresIn,
    long refreshExpiresIn,
    String jti
) {}
```

**实现类**：

```java
// 对称签名实现
public class ZerxHs256TokenService implements ZerxTokenService { ... }

// 非对称签名实现（P1-6 修复：支持 RS256）
public class ZerxRs256TokenService implements ZerxTokenService { ... }
```

**自定义接口 Zerx 前缀（P1-8 修复）完整清单**：

| 原 Spring Security 概念 | zerx 对应接口/类 | 说明 |
|------------------------|------------------|------|
| `AuthenticationProvider` | `ZerxAuthenticationProvider` | 认证提供者 |
| `UserDetailsService` | `ZerxUserDetailsService` | 用户详情加载 |
| `SecurityFilterChain` | `ZerxSecurityConfiguration` | 安全配置 |
| `JwtAuthenticationFilter` | `ZerxJwtAuthenticationFilter` | JWT 认证过滤器 |
| `AuthenticationEntryPoint` | `ZerxAuthenticationEntryPoint` | 未认证处理 |
| `AccessDeniedHandler` | `ZerxAccessDeniedHandler` | 无权限处理 |
| `UserDetails` | `ZerxUserDetails` | 用户详情载体 |
| `TokenService` | `ZerxTokenService` | Token 服务接口 |
| `PasswordValidator` | `ZerxPasswordValidator` | 密码强度校验 |
| `LoginAttemptService` | `ZerxLoginAttemptService` | 登录锁定管理 |
| `TokenBlacklistService` | `ZerxTokenBlacklistService` | Token 黑名单 |

#### 2.2.3 RBAC 权限模型

zerx 采用标准的 RBAC（Role-Based Access Control）模型，支持用户→角色→权限的三层关系。权限数据不在 JWT 中携带（避免 Token 膨胀），而是从 Redis 按需加载并本地短时间缓存。

**数据模型**：

```
sys_user（用户）
  ├── N:N → sys_role（角色）        通过 sys_user_role 中间表
  └─────────────────────────────
sys_role（角色）
  └── N:N → sys_permission（权限）  通过 sys_role_permission 中间表
```

**权限加载流程**：

1. JWT Filter 解析 Token 获取 `userId`。
2. 从 Redis `zerx:perm:{userId}` 读取权限集合（TTL 5 分钟）。
3. 若 Redis 未命中，从数据库加载并回写 Redis。
4. 权限注入 `SecurityContext`，供 `@PreAuthorize` 使用。
5. 权限变更时，通过 `ZerxPermissionCacheEvictEvent` 清除对应 Redis Key。

```java
// 权限注解使用
@PreAuthorize("hasAuthority('sys:user:add')")
@PostMapping("/user")
public ZerxResult<Void> createUser(@RequestBody @Valid UserDTO dto) { ... }

@PreAuthorize("hasAnyAuthority('sys:user:view', 'sys:user:edit')")
@GetMapping("/user/{id}")
public ZerxResult<UserVO> getUser(@PathVariable Long id) { ... }

// 角色判断
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/user/{id}")
public ZerxResult<Void> deleteUser(@PathVariable Long id) { ... }
```

#### 2.2.4 JWT 精简 Payload（P1-1 修复）

JWT Access Token 的 Payload 仅包含两个业务字段：`userId`（用户 ID）和 `jti`（JWT ID，UUID 唯一标识）。所有其他信息（用户名、角色、权限）通过 `userId` 从 Redis 按需加载。这种设计的优势：

- **Token 体积小**：Payload 仅约 50 字节，减少网络传输开销和 Header 解析时间。
- **权限实时性**：用户权限变更后，新请求立即从 Redis 获取最新权限，无需等待 Token 过期。
- **安全性高**：Token 泄露后攻击者无法直接获取用户角色信息；Token 黑名单只需存储 `jti`（约 36 字节/条），Redis 内存占用极低。
- **标准声明**：保留 `iss`（发行者）、`iat`（签发时间）、`exp`（过期时间）、`sub`（用户ID）、`jti`（JWT ID）、`type`（access/refresh）。

**Payload 结构**：

```json
{
  "sub": "12345",
  "jti": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "iat": 1704067200,
  "exp": 1704074400,
  "iss": "zerx",
  "type": "access"
}
```

#### 2.2.5 密码安全（P0-4 修复）

密码存储使用 Spring Security 内置的 `BCryptPasswordEncoder`，强制 work factor ≥ 12。BCrypt 是目前业界推荐的密码哈希算法，内置盐值、自适应计算成本、抗 GPU/ASIC 暴力破解。

```java
@Configuration
public class ZerxPasswordConfiguration {

    @Bean
    public PasswordEncoder zerxPasswordEncoder() {
        // work factor = 12，每次哈希约 250ms（可接受的登录延迟）
        // 不允许低于 12，以抵御 2025 年的 GPU 暴力破解能力
        return new BCryptPasswordEncoder(12);
    }
}
```

**密码强度策略**（P1-3 修复）：

```java
package com.zerx.spring.security.validator;

/**
 * 密码强度校验器。注册和修改密码时强制校验。
 */
public interface ZerxPasswordValidator {
    /**
     * 校验密码强度，不通过时抛出 ZerxPasswordPolicyException。
     * 规则：
     *   - 长度 ≥ 8 位
     *   - 包含大写字母、小写字母、数字
     *   - 包含至少一个特殊字符 (!@#$%^&*)
     *   - 不与最近 3 次历史密码重复（可选）
     *   - 不与用户名/手机号相同
     */
    void validate(String rawPassword, String username);
}

// 默认实现
public class DefaultZerxPasswordValidator implements ZerxPasswordValidator {
    private static final Pattern STRONG_PASSWORD = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,}$"
    );

    @Override
    public void validate(String rawPassword, String username) {
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new ZerxPasswordPolicyException("密码长度不能少于 8 位");
        }
        if (!STRONG_PASSWORD.matcher(rawPassword).matches()) {
            throw new ZerxPasswordPolicyException(
                "密码必须包含大写字母、小写字母、数字和特殊字符");
        }
        if (rawPassword.equalsIgnoreCase(username)) {
            throw new ZerxPasswordPolicyException("密码不能与用户名相同");
        }
    }
}
```

#### 2.2.6 密钥管理（P0-5 修复）

JWT 密钥管理覆盖密钥的完整生命周期：生成、存储、轮换、泄露应急、双密钥过渡、多环境隔离。这是安全体系中最关键的运维环节。

**1. 密钥生成**：

```bash
# HS256 对称密钥生成（256 位 Base64）
openssl rand -base64 32

# RS256 非对称密钥对生成
openssl genrsa -out private.pem 2048
openssl rsa -pubout -in private.pem -out public.pem
# 提取 Base64 编码的公私钥供配置使用
```

**2. 多环境隔离**：

```yaml
# application-dev.yml（开发环境）
zerx:
  security:
    jwt:
      algorithm: HS256
      secret: ${ZERX_JWT_SECRET_DEV}       # 环境变量注入，禁止硬编码
      access-token-expire: 86400            # 开发环境 24 小时
      refresh-token-expire: 2592000         # 30 天

# application-prod.yml（生产环境）
zerx:
  security:
    jwt:
      algorithm: RS256                      # 生产环境使用非对称签名
      private-key: ${ZERX_JWT_PRIVATE_KEY}  # 从密钥管理服务或 Vault 获取
      public-key: ${ZERX_JWT_PUBLIC_KEY}
      access-token-expire: 7200             # 生产环境 2 小时
      refresh-token-expire: 604800          # 7 天
```

**3. 密钥轮换**：

```java
/**
 * 支持多密钥验证的 TokenService。
 * 旧密钥仅用于验证（不允许签发），新密钥用于签发和验证。
 */
public class ZerxRotatingTokenService implements ZerxTokenService {

    private final Key activeKey;     // 当前活跃密钥（签发 + 验证）
    private final List<Key> retiredKeys; // 已退役密钥（仅验证）
    private final String keyId;       // 密钥版本标识

    @Override
    public String generateAccessToken(Long userId, String jti) {
        // 始终使用 activeKey 签发
        return Jwts.builder()
            .header().keyId(keyId).and()
            .subject(userId.toString())
            .id(jti)
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(accessTtl)))
            .issuer("zerx")
            .signWith(activeKey)
            .compact();
    }

    @Override
    public ZerxTokenClaims parseToken(String token) {
        // 解析 header 中的 kid 获取密钥版本
        // 先尝试 activeKey，失败后遍历 retiredKeys
        // 全部失败则抛出 JwtException
    }
}
```

**4. 泄露应急**：

```
应急流程：
1. 发现密钥泄露 → 立即从配置中心（Nacos/Apollo/Vault）删除旧密钥
2. 部署新密钥到所有节点（通过配置中心推送，无需重启）
3. 清空 Redis 中所有已存储的 Refresh Token（KEYS zerx:refresh:* → DEL）
4. 可选：通过 Token 黑名单强制所有使用旧密钥签发的 Token 失效
5. 通知用户重新登录
```

**5. 双密钥过渡**：轮换期间同时支持新旧两个密钥验证。新 Token 使用新密钥签发，旧 Token 在剩余有效期内仍可使用旧密钥验证。过渡期结束后移除旧密钥。通过 JWT Header 的 `kid` 字段区分密钥版本。

#### 2.2.7 Refresh Token 机制（P1-2 修复）

Refresh Token 采用 Redis 存储的方案，配合 Token 旋转（Rotation）和重放攻击检测。

**存储结构**：

```
Redis Key:   zerx:refresh:{userId}:{deviceId}
Redis Value: { "token": "refresh_token_hash", "jti": "...", "createdAt": "...", "ip": "..." }
Redis TTL:   7 天（与 Refresh Token 有效期一致）
```

**Token 旋转**：每次使用 Refresh Token 刷新 Access Token 时，旧 Refresh Token 立即失效，生成新的 Refresh Token。这确保了即使 Refresh Token 被窃取，攻击者只能使用一次。

**重放攻击检测**：通过 Redis 存储已消费的 Refresh Token 的 SHA-256 哈希值（TTL = 原始 Refresh Token 剩余有效期）。如果同一 Refresh Token 被提交两次，说明发生了重放攻击，系统立即将该用户所有设备的 Refresh Token 全部失效。

```java
@Service
public class ZerxRefreshTokenService {

    private final StringRedisTemplate redisTemplate;
    private final ZerxTokenService tokenService;

    /**
     * 刷新 Token（含旋转 + 重放检测）。
     */
    public ZerxTokenPair refresh(String refreshToken, String ipAddress, String userAgent) {
        // 1. 验证 Refresh Token 签名和有效期
        ZerxTokenClaims claims = tokenService.parseToken(refreshToken);
        if (!"refresh".equals(claims.tokenType())) {
            throw new ZerxAuthException("非 Refresh Token");
        }

        // 2. 重放检测：检查该 Token 是否已被使用
        String consumedKey = "zerx:refresh:consumed:" + claims.jti();
        Boolean alreadyConsumed = redisTemplate.hasKey(consumedKey);
        if (Boolean.TRUE.equals(alreadyConsumed)) {
            // 重放攻击！清除该用户所有设备的 Refresh Token
            evictAllUserTokens(claims.userId());
            throw new ZerxAuthException("检测到 Token 重放，已强制登出所有设备");
        }

        // 3. 将当前 Refresh Token 标记为已消费
        long remainingTtl = claims.expiresAt().getEpochSecond() - Instant.now().getEpochSecond();
        redisTemplate.opsForValue().set(consumedKey, "1",
            Duration.ofSeconds(Math.max(remainingTtl, 60)));

        // 4. 验证 Redis 中是否存在该 Token（未过期、未被手动注销）
        String deviceId = extractDeviceId(userAgent);
        String storedKey = "zerx:refresh:" + claims.userId() + ":" + deviceId;
        String storedHash = redisTemplate.opsForValue().get(storedKey);
        String currentHash = DigestUtils.sha256Hex(refreshToken);
        if (!currentHash.equals(storedHash)) {
            throw new ZerxAuthException("Refresh Token 已失效");
        }

        // 5. 生成新的 Token 对（旋转）
        String newJti = UUID.randomUUID().toString();
        ZerxTokenPair newPair = new ZerxTokenPair(
            tokenService.generateAccessToken(claims.userId(), newJti),
            tokenService.generateRefreshToken(claims.userId(), newJti),
            accessTokenTtl,
            refreshTokenTtl,
            newJti
        );

        // 6. 更新 Redis：存储新 Refresh Token
        String newHash = DigestUtils.sha256Hex(newPair.refreshToken());
        redisTemplate.opsForValue().set(storedKey, newHash,
            Duration.ofSeconds(refreshTokenTtl));

        return newPair;
    }
}
```

**设备数限制**（P1-2 修复）：每个用户最多允许 N 个设备同时在线（默认 5 个，可配置）。新增设备时，如果已达上限，淘汰最早登录的设备。

```yaml
zerx:
  security:
    refresh-token:
      max-devices: 5                  # 最大同时在线设备数
      eviction-strategy: OLDEST       # OLDEST（淘汰最早的）/ NONE（拒绝新设备）
```

#### 2.2.8 账号安全（P1-3 修复）

**登录锁定**：连续登录失败 N 次（默认 5 次）后锁定账号 M 分钟（默认 30 分钟）。锁定信息存储在 Redis 中，支持集群共享。

```java
@Service
public class ZerxLoginAttemptService {

    private final StringRedisTemplate redisTemplate;

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;

    /**
     * 登录失败时调用。累计失败次数。
     * @return true 表示已锁定
     */
    public boolean loginFailed(String username, String ip) {
        String key = "zerx:login:attempt:" + username;
        Long attempts = redisTemplate.opsForValue().increment(key);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(LOCK_DURATION_MINUTES));
        }
        if (attempts != null && attempts >= MAX_ATTEMPTS) {
            // 锁定账号
            String lockKey = "zerx:login:locked:" + username;
            redisTemplate.opsForValue().set(lockKey, ip,
                Duration.ofMinutes(LOCK_DURATION_MINUTES));
            return true;
        }
        return false;
    }

    /**
     * 登录成功时调用。清除失败计数。
     */
    public void loginSucceeded(String username) {
        redisTemplate.delete("zerx:login:attempt:" + username);
        redisTemplate.delete("zerx:login:locked:" + username);
    }

    /**
     * 判断是否已锁定。
     */
    public boolean isLocked(String username) {
        return Boolean.TRUE.equals(
            redisTemplate.hasKey("zerx:login:locked:" + username));
    }

    /**
     * 获取剩余失败次数。
     */
    public int getRemainingAttempts(String username) {
        String key = "zerx:login:attempt:" + username;
        String val = redisTemplate.opsForValue().get(key);
        int attempts = val != null ? Integer.parseInt(val) : 0;
        return Math.max(0, MAX_ATTEMPTS - attempts);
    }
}
```

**异常登录告警**：当检测到以下异常行为时，发送告警（通过事件机制，业务可自定义告警渠道如邮件/短信/Webhook）：
- 同一账号在短时间内从不同 IP/地区登录。
- 单个 IP 短时间内尝试多个不同账号登录。
- 登录失败次数接近锁定阈值。

#### 2.2.9 HTTP 安全头（P1-4 修复）

HTTP 安全头通过 `SecurityFilterChain` 的 `.headers()` Lambda 配置统一注入，无需额外 Filter。

| 安全头 | 值 | 防护目标 | 配置方式 |
|--------|----|---------|---------|
| `X-Content-Type-Options` | `nosniff` | 防止浏览器 MIME 类型嗅探 | Lambda DSL 默认开启 |
| `X-Frame-Options` | `DENY` | 防止点击劫持 | Lambda DSL `frameOptions(frame -> frame.deny())` |
| `X-XSS-Protection` | `1; mode=block` | 浏览器内置 XSS 过滤器 | Lambda DSL 配置 |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | 强制 HTTPS | 可选开启（生产推荐） |
| `Content-Security-Policy` | 按业务定制 | XSS/数据注入防护 | 可选配置 |
| `Cache-Control` | `no-store, no-cache` | 敏感页面防缓存 | 通过 WebFilter 按路径配置 |

```java
// 生产环境增强安全头配置
@Bean
public SecurityFilterChain productionSecurityFilterChain(HttpSecurity http) throws Exception {
    http
        .headers(headers -> headers
            .contentTypeOptions(opt -> {})                          // X-Content-Type-Options: nosniff
            .frameOptions(frame -> frame.deny())                    // X-Frame-Options: DENY
            .xssProtection(xss -> xss.headerValue(                 // X-XSS-Protection: 1; mode=block
                XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
            .httpStrictTransportSecurity(hsts -> hsts              // HSTS
                .includeSubDomains(true)
                .maxAgeInSeconds(31536000)
                .preload(true))
            .contentSecurityPolicy(csp -> csp                      // CSP（按业务定制）
                .policyDirectives("default-src 'self'; script-src 'self' 'nonce-{nonce}'"))
        );
    return http.build();
}
```

---

### 2.3 Spring Cache（P0-6 修复）

zerx-spring-cache 基于 Spring Cache 抽象实现，核心模式为 **Cache-Aside**（旁路缓存），并通过 Redis Pub/Sub 保证集群环境下的多级缓存一致性，同时内置缓存击穿、雪崩、穿透三重防护机制。

**Cache-Aside 模式**：
- **读**：先查缓存，命中则返回；未命中则查数据库，回写缓存，设置 TTL。
- **写**：先更新数据库，后删除缓存（而非更新缓存，避免数据不一致）。
- **删**：删除数据库，同时删除缓存。

#### Redis Pub/Sub 一致性

在多级缓存（L1 Caffeine + L2 Redis）架构下，单个节点的本地缓存更新后，需要通知集群中其他节点失效对应的本地缓存条目。zerx 通过 Redis Pub/Sub 实现这一机制。

```java
/**
 * 缓存一致性通知。
 * 发布者：执行缓存删除/更新的节点。
 * 订阅者：集群中所有节点（包括发布者自身）。
 */
@Component
public class ZerxCacheSyncPublisher {

    private final StringRedisTemplate redisTemplate;

    /**
     * 发布缓存失效通知。
     * @param cacheName 缓存名称（对应 @Cacheable 的 value）
     * @param key 缓存键
     */
    public void publishEvict(String cacheName, String key) {
        String channel = "zerx:cache:evict:" + cacheName;
        redisTemplate.convertAndSend(channel, key);
    }
}

@Component
public class ZerxCacheSyncSubscriber {

    private final CacheManager cacheManager;  // Caffeine CacheManager

    /**
     * 订阅缓存失效通知，清除本地 Caffeine 缓存。
     */
    @RedisListener(channels = "zerx:cache:evict:*")
    public void onEvict(Message message, String channel) {
        String cacheName = channel.replace("zerx:cache:evict:", "");
        String key = new String(message.getBody());
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }
}
```

#### 缓存击穿防护

**问题**：热点 Key 在过期瞬间，大量并发请求同时穿透到数据库。

**方案**：互斥锁（`SETNX`）。只允许一个线程回源数据库，其他线程等待并重试从缓存读取。

```java
public <T> T getWithBreakdownProtection(String key, Callable<T> loader,
                                          Duration ttl, Duration lockTimeout) {
    // 1. 先查缓存
    T value = cacheOps.get(key);
    if (value != null) return value;

    // 2. 获取互斥锁
    String lockKey = "zerx:cache:lock:" + key;
    Boolean locked = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, "1", lockTimeout);
    
    if (Boolean.TRUE.equals(locked)) {
        try {
            // 3. 双重检查（可能在等待锁期间已被其他线程回填）
            value = cacheOps.get(key);
            if (value != null) return value;

            // 4. 回源数据库
            value = loader.call();
            if (value != null) {
                cacheOps.set(key, value, ttl);
            } else {
                // 空值缓存（防穿透，见下文）
                cacheOps.set(key, NULL_PLACEHOLDER, Duration.ofMinutes(5));
            }
        } finally {
            redisTemplate.delete(lockKey);
        }
    } else {
        // 5. 未获取锁，短暂休眠后重试
        Thread.sleep(100);
        return getWithBreakdownProtection(key, loader, ttl, lockTimeout);
    }
    return value;
}
```

#### 缓存雪崩防护

**问题**：大量 Key 在同一时间点过期，导致数据库瞬时压力飙升。

**方案**：基础 TTL + 随机偏移量（jitter）。

```java
/**
 * 计算实际 TTL：基础 TTL + [0, jitterMax] 随机秒数。
 * 例如 TTL 30 分钟，jitter 5 分钟，实际过期时间在 30~35 分钟之间。
 */
public Duration ttlWithJitter(Duration baseTtl, Duration jitterMax) {
    long baseSeconds = baseTtl.toSeconds();
    long jitterSeconds = ThreadLocalRandom.current()
        .nextLong(0, jitterMax.toSeconds() + 1);
    return Duration.ofSeconds(baseSeconds + jitterSeconds);
}
```

#### 缓存穿透防护

**问题**：恶意请求查询不存在的数据，每次请求都穿透到数据库。

**方案**：布隆过滤器 + 空值缓存。

```java
/**
 * 布隆过滤器：预加载所有合法主键 ID。
 * 查询前先经过布隆过滤器，不存在的 ID 直接返回 null，不查数据库。
 */
@Component
public class ZerxBloomFilter {

    private final RBloomFilter<Long> bloomFilter;

    @PostConstruct
    public void init() {
        // 预期元素数量 100 万，误判率 0.01%
        bloomFilter.tryInit(1_000_000L, 0.0001);
        // 启动时加载所有合法 ID（从数据库或缓存预热）
       预热(bloomFilter);
    }

    public boolean mightContain(Long id) {
        return bloomFilter.contains(id);
    }
}

/**
 * 空值缓存：查数据库返回 null 时，缓存一个空值占位符（短 TTL）。
 * 后续相同请求直接命中空值缓存，避免穿透。
 */
public static final String NULL_PLACEHOLDER = "zerx:null:placeholder";
```

---

### 2.4 参数校验与全局异常处理

参数校验基于 **Jakarta Bean Validation 3.0**（`jakarta.validation.constraints.*`），全局异常处理通过 `@RestControllerAdvice` 统一拦截，将所有异常转换为标准 `ZerxResult<T>` 响应格式。

**校验注解示例**：

```java
public record UserCreateDTO(
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 32, message = "用户名长度为 2-32 位")
    String username,

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,}$",
             message = "密码必须包含大小写字母、数字和特殊字符，且长度≥8位")
    String password,

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    String phone,

    @Email(message = "邮箱格式不正确")
    String email
) {}
```

**全局异常处理器**：

```java
@RestControllerAdvice
public class ZerxGlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ZerxResult<Void> handleBusiness(BusinessException ex) {
        return ZerxResult.fail(ex.getErrorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ZerxResult<Void> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return ZerxResult.fail(ErrorCode.PARAM_VALIDATION_FAILED, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ZerxResult<Void> handleConstraint(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.joining("; "));
        return ZerxResult.fail(ErrorCode.PARAM_VALIDATION_FAILED, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ZerxResult<Void> handleNotReadable(HttpMessageNotReadableException ex) {
        return ZerxResult.fail(ErrorCode.REQUEST_BODY_INVALID);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ZerxResult<Void> handleAccessDenied(AccessDeniedException ex) {
        return ZerxResult.fail(ErrorCode.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ZerxResult<Void> handleUnauthorized(AuthenticationException ex) {
        return ZerxResult.fail(ErrorCode.UNAUTHORIZED);
    }

    @ExceptionHandler(Exception.class)
    public ZerxResult<Void> handleException(Exception ex) {
        log.error("未捕获异常", ex);
        return ZerxResult.fail(ErrorCode.SYSTEM_ERROR);
    }
}
```

---

### 2.5 统一 Web 响应与可观测性（P1-10 修复）

**统一响应体 `ZerxResult<T>`**：

```java
public record ZerxResult<T>(
    int code,
    String message,
    T data,
    long timestamp,
    String traceId  // 链路追踪 ID（Micrometer Observation 自动注入）
) {
    public static <T> ZerxResult<T> ok(T data) {
        return new ZerxResult<>(200, "success", data,
            System.currentTimeMillis(), TraceContext.getTraceId());
    }
    public static <T> ZerxResult<T> fail(ErrorCode errorCode) {
        return new ZerxResult<>(errorCode.httpStatus(), errorCode.message(), null,
            System.currentTimeMillis(), TraceContext.getTraceId());
    }
    public static <T> ZerxResult<T> fail(ErrorCode errorCode, String detail) {
        return new ZerxResult<>(errorCode.httpStatus(), detail, null,
            System.currentTimeMillis(), TraceContext.getTraceId());
    }
}
```

**Micrometer Observation API 集成**：

v1.1 全面引入 Micrometer Observation API 作为可观测性的核心抽象。所有 HTTP 请求通过 `ServerHttpObservationFilter` 自动创建 Observation Span，框架层的缓存操作、数据库查询、外部调用也通过 `ObservationRegistry` 产生子 Span。Observation 数据同时导出至 Prometheus（指标）和 OpenTelemetry（链路追踪）。

```java
// 自动配置：启用 HTTP Server Observation
@Bean
public ServerHttpObservationFilter serverHttpObservationFilter(
        ObservationRegistry registry) {
    return new ServerHttpObservationFilter(registry);
}

// 手动创建 Observation（用于业务代码）
@Service
public class OrderService {
    private final ObservationRegistry registry;

    public Order createOrder(OrderDTO dto) {
        return Observation.createNotStarted("order.create", registry)
            .lowCardinalityKeyValue("order.type", dto.getType())
            .highCardinalityKeyValue("order.id", dto.getId())
            .observe(() -> {
                // 业务逻辑
                Order order = orderRepository.save(toEntity(dto));
                // 发布领域事件
                eventPublisher.publishEvent(new OrderCreatedEvent(order));
                return order;
            });
    }
}
```

**配置**：

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: always
  metrics:
    tags:
      application: ${spring.application.name}
  tracing:
    sampling:
      probability: 1.0  # 开发环境全量采样；生产环境建议 0.1
    propagation:
      type: w3c          # W3C Trace Context 标准传播格式

# Prometheus 指标导出
  prometheus:
    metrics:
      export:
        enabled: true
```

---

### 2.6 事件驱动封装

zerx 提供轻量的事件驱动机制，基于 Spring 的 `ApplicationEventPublisher` + `@EventListener`，用于模块间解耦通信。

```java
// 定义事件（使用 JDK 21 record）
public record PermissionChangedEvent(Long userId, Set<String> newPermissions) {}

// 发布事件
@Service
public class PermissionService {
    @Autowired
    private ApplicationEventPublisher publisher;

    public void updatePermissions(Long userId, Set<String> permissions) {
        // ... 保存到数据库 ...
        // 通知 Security 模块清除权限缓存
        publisher.publishEvent(new PermissionChangedEvent(userId, permissions));
    }
}

// 监听事件（异步处理，避免阻塞业务主流程）
@Async
@EventListener
public void onPermissionChanged(PermissionChangedEvent event) {
    // 清除 Redis 权限缓存
    redisTemplate.delete("zerx:perm:" + event.userId());
}
```

**关键事件清单**：

| 事件 | 发布者 | 监听者 | 触发时机 |
|------|--------|--------|---------|
| `PermissionChangedEvent` | PermissionService | SecurityCacheManager | 用户权限变更 |
| `UserLockedEvent` | LoginAttemptService | NotificationService | 账号被锁定 |
| `UserLoginEvent` | AuthService | AuditLogService | 用户登录成功 |
| `CacheEvictEvent` | CacheOps | CacheSyncSubscriber | 缓存失效（集群同步） |
| `RefreshTokenRotatedEvent` | RefreshTokenService | DeviceSessionService | Refresh Token 旋转 |
| `SecurityAlertEvent` | LoginAttemptService | AlertService | 异常登录行为 |

---

### 2.7 虚拟线程集成（P1-9 新增）

JDK 21 的虚拟线程（Virtual Threads）可以大幅提升 I/O 密集型应用的吞吐量。zerx-spring 对虚拟线程的集成采用渐进式策略，默认启用，遇到阻塞 API 时可按需回退。

**启用方式**：

```yaml
# application.yml
spring:
  threads:
    virtual:
      enabled: true  # Spring Boot 3.2+ 原生支持
```

等价于 Java 配置：

```java
@Bean
public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
    return protocolHandler -> {
        protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    };
}
```

**注意事项**：
- 虚拟线程与 `synchronized` 的交互：虚拟线程在遇到 `synchronized` 块时会"钉住"（pin）载体线程，降低并发效率。zerx 框架内部的锁机制优先使用 `ReentrantLock`（虚拟线程友好）而非 `synchronized`。
- 虚拟线程与 `ThreadLocal`：虚拟线程支持 `ThreadLocal`，但每个虚拟线程都有自己的 `ThreadLocal` 副本，内存开销需关注。SecurityContext 的传播改为使用 `InheritableThreadLocal` 或 `ScopedValue`（JDK 21 Preview）。
- 数据库连接池：虚拟线程模式下建议使用 HikariCP，因为它使用的是 `Connection` 对象而非线程绑定。Spring Data JDBC 和 JdbcTemplate 天然兼容虚拟线程。
- 不适合虚拟线程的场景：CPU 密集型计算、原生方法（JNI）调用、使用 `synchronized` 的旧库。

---

## 第三章 业务组件

### 3.1 短信验证码

短信验证码组件采用策略模式设计，支持阿里云、腾讯云、华为云等主流短信服务商的平滑切换。核心能力包括：验证码生成、发送频率限制（同号 60 秒间隔）、验证码有效期管理、验证失败次数限制（5 次失败后失效）、验证码防刷（同 IP/同号码日发送上限）。

```java
public interface ZerxSmsService {
    /** 发送验证码 */
    void sendCode(String phone, String scene);
    /** 校验验证码 */
    boolean verifyCode(String phone, String scene, String code);
}

// 短信发送器策略接口
public interface ZerxSmsSender {
    SendResult send(String phone, String templateId, Map<String, String> params);
}

// 阿里云实现
public class AliyunSmsSender implements ZerxSmsSender { ... }

// 腾讯云实现
public class TencentSmsSender implements ZerxSmsSender { ... }
```

**Redis 存储结构**：

```
zerx:sms:code:{phone}:{scene}    → "123456"  TTL 5 分钟
zerx:sms:rate:{phone}            → "3"       TTL 60 秒（发送间隔）
zerx:sms:fail:{phone}:{scene}    → "2"       TTL 5 分钟（失败计数）
zerx:sms:daily:{phone}           → "5"       TTL 24 小时（日发送计数）
```

---

### 3.2 Excel 操作

基于 **EasyExcel ≥3.3.x**（Jakarta EE 兼容版本），提供大数据量导入导出能力。EasyExcel 3.3.0+ 已完成从 `javax.servlet` 到 `jakarta.servlet` 的迁移，与 zerx v1.1 的 Jakarta 命名空间完全兼容。

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>easyexcel</artifactId>
    <version>3.3.4</version>  <!-- Jakarta 兼容版本 -->
</dependency>
```

**核心能力**：大数据量分批写入（避免 OOM）、导入校验监听器（实时反馈错误行）、字典翻译（`@DictFormat`）、密码加密列（`@ExcelEncrypt`）、模板导出、动态列表头。

```yaml
zerx:
  excel:
    threshold: 5000               # 内存阈值，超过则分批写入
    read-batch-size: 1000         # 导入每批读取行数
    write-batch-size: 5000        # 导出每批写入行数
```

---

### 3.3 对象存储

统一文件上传/下载/管理接口，策略模式支持 MinIO、阿里云 OSS、腾讯 COS、通用 S3 协议等多厂商。

```java
public interface ZerxOssStorageService {
    OssResult upload(MultipartFile file);
    OssResult upload(String path, InputStream stream, String contentType, long size);
    void download(String filePath, OutputStream outputStream);
    void delete(String filePath);
    String getUrl(String filePath);
    boolean exists(String filePath);
}

// 策略实现
├── MinioOssStorageService     ← zerx.oss.type=minio
├── AliyunOssStorageService    ← zerx.oss.type=aliyun
├── TencentCosStorageService   ← zerx.oss.type=tencent
└── S3OssStorageService        ← zerx.oss.type=s3
```

**安全措施**：文件类型白名单校验、文件大小限制、路径遍历防护（禁止 `../`）、上传文件病毒扫描（可选集成 ClamAV）、预签名 URL 限时限次访问。

---

### 3.4 分布式锁

基于 Redisson 实现，同时提供对 Redis RedLock 算法风险的认知和缓解措施。

**RedLock 风险认知**：Redis RedLock 在网络分区（Clock Drift）场景下存在理论上的安全性争议（Martin Kleppmann vs Antirez 论战）。zerx 采取务实的折中方案：单 Redis 实例使用 Redisson 标准锁（性能优先）；高可用场景使用 Redis Cluster 多节点锁，并配合业务幂等性作为兜底。

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ZerxDistributedLock {
    /** 锁的 Key（支持 SpEL） */
    String key();
    /** 等待获取锁的最长时间（默认 0，不等待） */
    long waitTime() default 0;
    /** 持有锁的最长时间（默认 30 秒） */
    long leaseTime() default 30;
    /** 时间单位 */
    TimeUnit unit() default TimeUnit.SECONDS;
}

// 使用示例
@ZerxDistributedLock(key = "'order:lock:' + #orderId", waitTime = 3, leaseTime = 10)
public void processOrder(Long orderId) { ... }
```

**指数退避**：获取锁失败时采用指数退避重试策略（初始 100ms，最大 1s，最大重试 3 次），避免羊群效应。

**可观测性**：每次加锁/解锁操作通过 Micrometer Observation API 产生 Span，记录锁等待时间、持有时间、竞争次数等指标。

---

### 3.5 幂等性控制

基于 Redis Token 机制实现接口幂等性保证，防止重复提交。

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ZerxIdempotent {
    /** 幂等 Key 的 SpEL 表达式（默认取请求参数的 idempotentToken） */
    String key() default "#request.idempotentToken";
    /** 幂等 Token 有效期（默认 24 小时） */
    long ttl() default 86400;
    /** 时间单位 */
    TimeUnit unit() default TimeUnit.SECONDS;
    /** 提示消息 */
    String message() default "请勿重复提交";
}

// 前端流程：
// 1. GET /api/idempotent/token → 返回 token（同时存入 Redis）
// 2. POST /api/order + Header: X-Idempotent-Token: {token}
// 3. 服务端校验 token 是否存在且未使用 → 处理业务 → 标记 token 已使用
```

---

### 3.6 限流熔断（P1-11/P1-12 修复）

#### 双层限流架构

```
请求 → [Bucket4j 本地限流 L1] → [Redis 分布式限流 L2] → 业务处理
              ↓                         ↓
         本地内存 Bucket              Redis Lua 脚本
         （单机精确）                （集群维度）
```

- **L1 本地限流（Bucket4j）**：基于 JVM 内存中的令牌桶算法，单机维度，响应时间 < 1μs。适用于单机级别的 QPS 限制（如单接口 1000 QPS/节点）。
- **L2 分布式限流（Redis + Lua）**：基于 Redis 的滑动窗口/令牌桶算法，集群维度，响应时间 < 5ms。适用于全局限流（如用户维度 10 次/分钟）。

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ZerxRateLimiter {
    /** 限流 Key（支持 SpEL） */
    String key() default "";
    /** L1 本地限流：每秒请求数 */
    double localPermitsPerSecond() default 100;
    /** L2 分布式限流：时间窗口内最大请求数 */
    long distributedMaxRequests() default 0;  // 0 表示不启用
    /** L2 时间窗口（秒） */
    long distributedWindowSeconds() default 60;
    /** 被限流时的提示消息 */
    String message() default "请求过于频繁，请稍后再试";
}
```

#### 熔断降级 FallbackHandler（P1-12 修复）

基于 Resilience4j 的 `CircuitBreaker`，当下游服务异常率超过阈值时自动熔断，返回 Fallback 响应。

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ZerxCircuitBreaker {
    /** 熔断器名称 */
    String name() default "";
    /** 失败率阈值（百分比），超过则开启熔断 */
    double failureRateThreshold() default 50;
    /** 慢调用时间阈值（秒） */
    double slowCallDurationThreshold() default 3;
    /** 慢调用率阈值（百分比） */
    double slowCallRateThreshold() default 80;
    /** 最小调用次数（达到后才计算失败率） */
    int minimumNumberOfCalls() default 100;
    /** 熔断开启持续时间（秒） */
    int waitDurationInOpenState() default 30;
    /** 半开状态允许的测试调用数 */
    int permittedNumberOfCallsInHalfOpenState() default 5;
    /** Fallback 处理类 */
    Class<? extends ZerxFallbackHandler> fallbackHandler() default
        ZerxFallbackHandler.Default.class;
}

/**
 * Fallback 处理器接口。
 */
public interface ZerxFallbackHandler<T> {
    ZerxResult<T> fallback(Throwable cause);

    class Default<T> implements ZerxFallbackHandler<T> {
        @Override
        public ZerxResult<T> fallback(Throwable cause) {
            return ZerxResult.fail(ErrorCode.SERVICE_UNAVAILABLE,
                "服务暂时不可用，请稍后再试");
        }
    }
}

// 使用示例
@ZerxCircuitBreaker(
    name = "paymentService",
    failureRateThreshold = 60,
    fallbackHandler = PaymentFallbackHandler.class
)
public PaymentResult processPayment(PaymentRequest request) {
    return paymentGateway.charge(request);
}
```

---

## 第四章 模块依赖关系

### 4.1 依赖分层

zerx 模块严格分为四层，层间单向依赖，禁止跨层反向依赖或同层循环依赖。

```
┌─────────────────────────────────────────────────┐
│  Layer 4: 业务应用层                              │
│  ┌────────────────────────────────────────────┐  │
│  │  你的业务代码（Controller/Service/etc）     │  │
│  └────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────┤
│  Layer 3: 业务组件层 (zerx-component)             │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────┐  │
│  │ excel    │ │ oss      │ │ ratelimit        │  │
│  │ sms      │ │ lock     │ │ idempotent       │  │
│  └──────────┘ └──────────┘ └──────────────────┘  │
├─────────────────────────────────────────────────┤
│  Layer 2: 框架封装层 (zerx-spring)                │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌────────────────┐  │
│  │ web  │ │security│ │cache │ │ data | monitor │  │
│  └──┬───┘ └──┬───┘ └──┬───┘ └───────┬────────┘  │
│     │        │        │              │           │
│     └────────┴────────┴──────────────┘           │
├─────────────────────────────────────────────────┤
│  Layer 1: 基础层 (zerx-core)                     │
│  ┌────────────────────────────────────────────┐  │
│  │ zerx-common (零依赖，60 个源文件，2108 测试)│  │
│  └────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

### 4.2 框架模块间依赖（P1-13 修复）

以下为框架封装层内部 7 个模块的详细依赖关系。这是 v1.1 新增的精细化依赖图谱，明确每个模块的依赖方向和原因。

```
                    zerx-common
                   ┌────────────┐
                   │ 零依赖      │
                   │ JDK 21 only│
                   └─────┬──────┘
                         │
          ┌──────────────┼──────────────┐
          │              │              │
    ┌─────▼─────┐  ┌────▼─────┐  ┌────▼──────────┐
    │spring-web │  │spring-data│  │spring-monitor  │
    │           │  │          │  │               │
    │统一响应    │  │BaseEntity│  │Micrometer     │
    │全局异常    │  │DynamicQry│  │HealthCheck    │
    │参数校验    │  │JdbcTemplate│ │Prometheus     │
    │CORS       │  │审计/逻辑删│  │OTel Bridge    │
    └─────┬─────┘  └────┬─────┘  └──────┬────────┘
          │              │               │
    ┌─────▼──────────────▼───────────────▼───────┐
    │           zerx-spring-cache                  │
    │                                              │
    │ Cache-Aside + Redis Pub/Sub                  │
    │ 击穿/雪崩/穿透防护                            │
    └─────────────────────┬────────────────────────┘
                          │
    ┌─────────────────────▼────────────────────────┐
    │         zerx-spring-security                  │
    │                                              │
    │ SecurityFilterChain Lambda DSL               │
    │ JWT (HS256/RS256) + TokenService 接口        │
    │ RBAC + Refresh Token 旋转 + 账号安全          │
    │ HTTP 安全头 + Token 黑名单（强制 Redis）      │
    └─────────────────────┬────────────────────────┘
                          │
    ┌─────────────────────▼────────────────────────┐
    │        zerx-spring-logging                    │
    │                                              │
    │ TraceID 生成与传播 + 请求/响应日志            │
    │ 敏感参数脱敏 + 慢请求告警                     │
    └──────────────────────────────────────────────┘
```

**依赖规则详解**：

| 模块 | 依赖 | 依赖原因 |
|------|------|---------|
| `spring-web` | `zerx-common` | 统一异常体系、工具类 |
| `spring-data` | `zerx-common` | 基础类型、工具类 |
| `spring-cache` | `zerx-common` | 配置解析、工具类 |
| `spring-security` | `spring-web` | 统一异常响应、请求上下文 |
| `spring-security` | `spring-cache` | Token 黑名单（强制 Redis）、权限缓存 |
| `spring-monitor` | `spring-data` | 数据库健康检查 |
| `spring-logging` | `spring-web` | 请求上下文、响应包装 |
| `spring-logging` | `spring-security` | 用户上下文（可选，日志记录操作人） |

### 4.3 Maven 模块结构

```xml
<!-- zerx-parent pom.xml -->
<groupId>com.zerx</groupId>
<artifactId>zerx-parent</artifactId>
<version>1.1.0</version>
<packaging>pom</packaging>

<modules>
    <module>zerx-core</module>
    <module>zerx-spring</module>
    <module>zerx-component</module>
</modules>

<!-- zerx-spring pom.xml（聚合 POM） -->
<modules>
    <module>zerx-spring-web</module>
    <module>zerx-spring-data</module>
    <module>zerx-spring-cache</module>
    <module>zerx-spring-security</module>
    <module>zerx-spring-monitor</module>
    <module>zerx-spring-doc</module>
    <module>zerx-spring-logging</module>
    <module>zerx-spring-bom</module>
</modules>

<!-- zerx-component pom.xml（聚合 POM） -->
<modules>
    <module>zerx-component-excel</module>
    <module>zerx-component-oss</module>
    <module>zerx-component-lock</module>
    <module>zerx-component-idempotent</module>
    <module>zerx-component-ratelimit</module>
    <module>zerx-component-sms</module>
    <module>zerx-component-bom</module>
</modules>
```

---

## 第五章 实施路线

### 5.1 第一阶段 P0（预计 4 周）

P0 为阻断性问题，必须在框架可用之前全部解决。

| 序号 | 任务 | 对应问题 | 产出 | 验收标准 |
|:----:|------|---------|------|---------|
| 1 | javax → jakarta 全量迁移 | P0-1 | 所有 import 语句替换；依赖版本升级；启动时残留检测 | 编译通过；启动无 WARN |
| 2 | SecurityFilterChain Lambda DSL 重构 | P0-2 | `ZerxSecurityConfiguration` 类；集成测试 | 所有安全规则通过测试 |
| 3 | DynamicQuery 链式构建器开发 | P0-3 | `DynamicQuery` 类 + 单元测试（覆盖 eq/like/in/between/orderBy/groupBy/join/count） | 测试覆盖率 ≥ 95% |
| 4 | JdbcTemplate 透明集成验证 | P0-3 | 集成测试（DynamicQuery 与 CrudRepository 在同一事务中混用） | 事务回滚正确 |
| 5 | BCrypt work factor ≥ 12 实施 | P0-4 | `ZerxPasswordConfiguration`；密码强度校验器 | 所有密码 hash 使用 BCrypt(12) |
| 6 | JWT 密钥管理全生命周期实现 | P0-5 | `ZerxTokenService` 接口 + HS256/RS256 实现；密钥轮换；多环境配置 | 生成/轮换/应急流程验证 |
| 7 | 多级缓存 Redis Pub/Sub 一致性 | P0-6 | `ZerxCacheSyncPublisher`/`Subscriber`；击穿/雪崩/穿透防护 | 集群环境下缓存一致性验证 |
| 8 | 移除 CrudServiceTemplate | P0-7 | 代码删除；Service 层回归测试 | 无编译错误；全量测试通过 |

### 5.2 第二阶段 P1（预计 6 周）

P1 为优先级增强，在 P0 完成后立即启动。

| 序号 | 任务 | 对应问题 | 产出 | 验收标准 |
|:----:|------|---------|------|---------|
| 1 | JWT Payload 精简 + 权限 Redis 按需加载 | P1-1 | JWT Payload 仅含 userId+jti；权限从 Redis 加载 | Token 体积 ≤ 200 字节；权限实时更新 |
| 2 | Refresh Token Redis + 旋转 + 重放检测 | P1-2 | `ZerxRefreshTokenService`；设备数限制 | 旋转/重放/设备淘汰测试通过 |
| 3 | 账号安全（登录锁定/密码强度/异常告警） | P1-3 | `ZerxLoginAttemptService` + 告警事件 | 锁定/告警/解锁流程验证 |
| 4 | HTTP 安全头配置 | P1-4 | `SecurityFilterChain` headers Lambda 配置 | 安全头扫描工具验证 |
| 5 | Token 黑名单集群强制 Redis | P1-5 | 黑名单存储 Redis（非本地缓存）；集群共享验证 | 多节点黑名单同步 |
| 6 | JWT RS256 非对称签名支持 | P1-6 | `ZerxRs256TokenService` | RS256 签发/验证/密钥轮换测试 |
| 7 | TokenService 接口化 | P1-7 | `ZerxTokenService` 接口 + 多实现 | 接口替换测试 |
| 8 | 自定义接口 Zerx 前缀 | P1-8 | 全部自定义类重命名为 Zerx 前缀 | 无命名冲突 |
| 9 | 启用 JDK 21 虚拟线程 | P1-9 | 配置 + 性能测试 | I/O 密集型场景吞吐量提升 |
| 10 | Micrometer Observation API 集成 | P1-10 | HTTP/Cache/DB/Observation 自动埋点 | Prometheus 指标可见；OTel 链路完整 |
| 11 | 双层限流 Bucket4j L1 + Redis L2 | P1-11 | `ZerxRateLimiter` 注解 + AOP | 单机/集群限流测试 |
| 12 | 熔断 FallbackHandler | P1-12 | `ZerxCircuitBreaker` + `ZerxFallbackHandler` | 熔断/半开/恢复状态转换验证 |
| 13 | 框架模块间依赖关系图 | P1-13 | 本文档 §4.2 | 依赖无循环；分层清晰 |

### 5.3 第三阶段 P2（预计 4 周）

P2 为扩展增强，视业务需求排期。

| 序号 | 任务 | 产出 | 验收标准 |
|:----:|------|------|---------|
| 1 | 短信验证码组件 | `zerx-component-sms` + 阿里云/腾讯云实现 | 发送/校验/频率限制/防刷测试 |
| 2 | 幂等性控制组件 | `zerx-component-idempotent` | Token 生成/校验/过期测试 |
| 3 | 操作日志 AOP | `zerx-component-log` | 注解驱动日志记录测试 |
| 4 | 数据权限组件 | `zerx-component-datascope` | 行级数据权限过滤测试 |
| 5 | 多租户组件 | `zerx-component-tenant` | 租户隔离/共享模式测试 |
| 6 | 支付集成组件 | `zerx-component-pay` | 微信/支付宝对接测试 |
| 7 | 邮件服务组件 | `zerx-component-mail` | SMTP/模板邮件发送测试 |
| 8 | 消息队列封装 | `zerx-spring-mq` | RabbitMQ/Kafka 统一抽象测试 |

---

> **文档修订记录**
>
> | 版本 | 日期 | 修订内容 |
> |------|------|---------|
> | v1.1.0 | 2025-01-01 | 修复全部 P0（7 项）和 P1（13 项）问题；新增 DynamicQuery、JWT 密钥管理、Redis Pub/Sub 缓存一致性、虚拟线程、Micrometer Observation、双层限流、熔断降级等章节 |
> | v1.0.0 | 2024-12-01 | 初始版本，12 模块基础架构设计 |
