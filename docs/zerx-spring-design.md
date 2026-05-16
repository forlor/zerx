# Zerx Spring 体系设计文档

> **ZERX SPRING ECOSYSTEM DESIGN**
> 版本：v2.0.0 | 日期：2026-05-16 | 作者：Forlor
> GitHub: github.com/forlor/zerx
> JDK 21 | Maven | Spring Boot 3.3+ | Spring Data JDBC

---

## 目录

- [1 设计概述](#1-设计概述)
  - [1.1 设计目标](#11-设计目标)
  - [1.2 两大分类](#12-两大分类)
  - [1.3 整体架构](#13-整体架构)
  - [1.4 与 zerx-core 的关系](#14-与-zerx-core-的关系)
  - [1.5 技术选型决策：Spring Data JDBC](#15-技术选型决策spring-data-jdbc)
- [2 框架封装层 — zerx-spring](#2-框架封装层--zerx-spring)
  - [2.1 zerx-spring-web（Web 层增强）](#21-zerx-spring-webweb-层增强)
  - [2.2 zerx-spring-data（数据访问增强）](#22-zerx-spring-data数据访问增强)
  - [2.3 zerx-spring-cache（缓存抽象）](#23-zerx-spring-cache缓存抽象)
  - [2.4 zerx-spring-security（认证授权）](#24-zerx-spring-security认证授权)
  - [2.5 zerx-spring-mq（消息队列）](#25-zerx-spring-mq消息队列)
  - [2.6 zerx-spring-doc（API 文档）](#26-zerx-spring-docapi-文档)
  - [2.7 zerx-spring-logging（请求日志）](#27-zerx-spring-logging请求日志)
  - [2.8 zerx-spring-monitor（监控）](#28-zerx-spring-monitor监控)
  - [2.9 zerx-spring-bom（框架层 BOM）](#29-zerx-spring-bom框架层-bom)
- [3 业务组件层 — zerx-component](#3-业务组件层--zerx-component)
  - [3.1 zerx-component-log（操作日志）](#31-zerx-component-log操作日志)
  - [3.2 zerx-component-excel（Excel 操作）](#32-zerx-component-excelexcel-操作)
  - [3.3 zerx-component-oss（对象存储）](#33-zerx-component-oss对象存储)
  - [3.4 zerx-component-sms（短信服务）](#34-zerx-component-sms短信服务)
  - [3.5 zerx-component-mail（邮件服务）](#35-zerx-component-mail邮件服务)
  - [3.6 zerx-component-lock（分布式锁）](#36-zerx-component-lock分布式锁)
  - [3.7 zerx-component-idempotent（幂等/防重）](#37-zerx-component-idempotent幂等防重)
  - [3.8 zerx-component-ratelimit（接口限流）](#38-zerx-component-ratelimit接口限流)
  - [3.9 zerx-component-datascope（数据权限）](#39-zerx-component-datascope数据权限)
  - [3.10 zerx-component-tenant（多租户）](#310-zerx-component-tenant多租户)
  - [3.11 zerx-component-pay（支付集成）](#311-zerx-component-pay支付集成)
  - [3.12 zerx-component-bom（组件层 BOM）](#312-zerx-component-bom组件层-bom)
- [4 统一设计规范](#4-统一设计规范)
  - [4.1 自动配置规范](#41-自动配置规范)
  - [4.2 注解驱动规范](#42-注解驱动规范)
  - [4.3 策略模式规范（多厂商适配）](#43-策略模式规范多厂商适配)
  - [4.4 配置前缀规范](#44-配置前缀规范)
  - [4.5 异步化规范](#45-异步化规范)
- [5 模块依赖关系](#5-模块依赖关系)
- [6 实施路线图](#6-实施路线图)

---

## 1 设计概述

### 1.1 设计目标

Zerx Spring 体系在 zerx-core（零依赖基础脚手架）之上，提供两大类能力：

| 目标 | 说明 |
|------|------|
| **框架封装** | 对 Spring Boot 自身能力的实用性增强，消除样板代码，统一编码规范 |
| **业务组件** | 企业级通用业务能力的标准化落地，注解驱动，按需取用 |

核心设计原则：

- **注解驱动**：业务零侵入，一个注解启用一个能力（`@Log`、`@RateLimiter`、`@DistributedLock`）
- **策略模式**：多厂商适配（OSS/SMS/Pay）通过配置 `type` 字段动态切换，无需改代码
- **AOP 切面**：日志、权限、限流、防重等横切关注点统一通过 AOP 实现
- **自动配置**：每个模块都是独立的 Spring Boot Starter，引入即生效，配置即定制
- **按需取用**：每个组件独立 JAR，用哪个引哪个，不做强制捆绑
- **轻量 ORM**：使用 Spring Data JDBC 替代 MyBatis，拥抱领域驱动设计，避免 ORM 黑盒

### 1.2 两大分类

```
┌─────────────────────────────────────────────────────────────┐
│                      Zerx Spring 体系                        │
│                                                             │
│  ┌───────────────────────┐  ┌────────────────────────────┐  │
│  │  zerx-spring          │  │  zerx-component            │  │
│  │  框架封装层            │  │  业务组件层                 │  │
│  │                       │  │                            │  │
│  │  对 Spring Boot 自身   │  │  企业级通用业务能力          │  │
│  │  能力的实用性封装       │  │  的标准化落地               │  │
│  │                       │  │                            │  │
│  │  · Web 层增强          │  │  · 操作日志                │  │
│  │  · 数据访问增强        │  │  · Excel 导入导出          │  │
│  │  · 缓存抽象            │  │  · 对象存储                │  │
│  │  · 认证授权            │  │  · 短信服务                │  │
│  │  · 消息队列            │  │  · 邮件服务                │  │
│  │  · API 文档            │  │  · 分布式锁                │  │
│  │  · 请求日志            │  │  · 幂等/防重               │  │
│  │  · 监控                │  │  · 接口限流                │  │
│  │                       │  │  · 数据权限                │  │
│  │                       │  │  · 多租户                  │  │
│  │                       │  │  · 支付集成                │  │
│  └───────────┬───────────┘  └─────────────┬──────────────┘  │
│              │                          │                  │
│              └──────────┬───────────────┘                  │
│                         │                                  │
│                    ┌────▼────┐                              │
│                    │zerx-core│ ← 零依赖基础脚手架             │
│                    └─────────┘                              │
└─────────────────────────────────────────────────────────────┘
```

**两者的本质区别：**

| 维度 | 框架封装 (zerx-spring) | 业务组件 (zerx-component) |
|------|----------------------|------------------------|
| 性质 | 对 Spring 框架能力的增强 | 面向具体业务场景的方案 |
| 依赖 | 以 `spring-boot-starter` 为基础 | 可依赖 zerx-spring 中的框架模块 |
| 举例 | 全局异常处理、响应封装、Spring Data JDBC 集成 | 短信验证码、Excel 导入、对象存储 |
| 通用性 | 所有 Spring Boot 项目都可能用到 | 根据业务场景按需引入 |

### 1.3 整体架构

```
zerx/
├── pom.xml                              ← zerx-parent
├── zerx-core/                           ← 已完成：零依赖基础脚手架
│   ├── zerx-common/
│   ├── zerx-core-bom/
│   └── zerx-architecture-test/
│
├── zerx-spring/                         ← 框架封装层
│   ├── pom.xml                          ← 聚合 POM（packaging=pom）
│   ├── zerx-spring-web/                 ← Web 层增强
│   ├── zerx-spring-data/                ← 数据访问增强（Spring Data JDBC）
│   ├── zerx-spring-cache/               ← 缓存抽象
│   ├── zerx-spring-security/            ← 认证授权
│   ├── zerx-spring-mq/                  ← 消息队列
│   ├── zerx-spring-doc/                 ← API 文档
│   ├── zerx-spring-logging/             ← 请求日志
│   ├── zerx-spring-monitor/             ← 监控
│   └── zerx-spring-bom/                 ← 框架层 BOM
│
├── zerx-component/                      ← 业务组件层
│   ├── pom.xml                          ← 聚合 POM（packaging=pom）
│   ├── zerx-component-log/              ← 操作日志
│   ├── zerx-component-excel/            ← Excel 导入导出
│   ├── zerx-component-oss/              ← 对象存储
│   ├── zerx-component-sms/              ← 短信服务
│   ├── zerx-component-mail/             ← 邮件服务
│   ├── zerx-component-lock/             ← 分布式锁
│   ├── zerx-component-idempotent/       ← 幂等/防重提交
│   ├── zerx-component-ratelimit/        ← 接口限流
│   ├── zerx-component-datascope/        ← 数据权限
│   ├── zerx-component-tenant/           ← 多租户
│   ├── zerx-component-pay/              ← 支付集成
│   └── zerx-component-bom/              ← 组件层 BOM
│
└── docs/                                ← 设计文档
```

### 1.4 与 zerx-core 的关系

```
                    ┌──────────────────────────┐
                    │       zerx-core           │
                    │  ┌──────────┐             │
                    │  │zerx-common│ 60 个源文件  │
                    │  │ 零依赖    │ 2108 测试    │
                    │  └─────┬────┘             │
                    └────────┼──────────────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
     ┌────────▼─────┐      │      ┌───────▼──────────┐
     │ zerx-spring  │      │      │ zerx-component   │
     │ 框架封装层   │◄─────┘      │ 业务组件层        │
     │              │ 依赖关系    │                  │
     │ 对 Spring    │◄───────────│ 可选择性依赖       │
     │ 框架增强     │             │ zerx-spring 模块  │
     └──────────────┘             └──────────────────┘
```

- **zerx-spring** 依赖 zerx-common，不依赖任何 zerx-component 模块
- **zerx-component** 可选择性依赖 zerx-spring 模块（如 `component-log` 依赖 `spring-web` 的异常体系）
- 两个层级之间保持单向依赖，不存在循环依赖

### 1.5 技术选型决策：Spring Data JDBC

> **核心决策**：数据访问层使用 **Spring Data JDBC**，不使用 MyBatis / MyBatis-Plus / Spring Data JPA。

**选型理由：**

| 维度 | Spring Data JDBC | MyBatis / MyBatis-Plus | Spring Data JPA |
|------|-----------------|----------------------|-----------------|
| **ORM 层** | 轻量，无代理、无懒加载、无脏检查 | 无 ORM，纯 SQL 映射 | 重量级，Hibernate 全套 |
| **领域模型** | 原生支持聚合根，DDD 友好 | 贫血模型，SQL 驱动 | 实体关系图，隐式耦合 |
| **学习成本** | 低：继承 `CrudRepository` 即可 | 中：需学习 XML/注解 SQL | 高：需理解 JPA 生命周期 |
| **性能** | 高：无代理开销，无 N+1（开启 Single Query Loading） | 高：直接 SQL | 中：懒加载可能触发 N+1 |
| **SQL 控制** | 派生查询 + 自定义 `@Query` | 完全手动控制 | HQL / JPQL / Criteria |
| **缓存/状态** | 无状态，每次 save 全量插入/更新 | 无状态 | 一级缓存，脏检查 |
| **复杂查询** | DTO 投影 + JdbcTemplate 兜底 | 最强：动态 SQL | Criteria API 较复杂 |
| **多对多** | 通过中间实体显式管理 | 原生支持 | 自动管理关联 |
| **审计** | 原生 `@CreatedDate` `@LastModifiedDate` | 需自定义 Interceptor | 原生支持 |
| **事件回调** | `BeforeSaveCallback` `AfterSaveCallback` 等 7 个生命周期钩子 | Interceptor 插件 | EntityListener |
| **与 Spring Boot 集成** | `spring-boot-starter-data-jdbc` 一键自动配置 | 需额外 Starter | `spring-boot-starter-data-jpa` |
| **Lombok 兼容** | 推荐使用 `@Getter` `@Setter`（避免 `@Data`），Java 17+ Record 作为值对象 | 无特殊限制 | 需注意 `@Data` 与代理冲突 |

**设计哲学契合度分析：**

Spring Data JDBC 的设计哲学与 Zerx 项目高度契合：

1. **显式优于隐式**：没有 JPA 的自动脏检查和懒加载陷阱，所有持久化行为都是显式的。调用 `save()` 就是完整插入或更新，开发者对每一条 SQL 都有清晰认知。

2. **DDD 聚合根原生支持**：Spring Data JDBC 以聚合（Aggregate）为基本持久化单元，只有聚合根有 Repository。子实体通过聚合根级联保存，天然强制了领域边界。这与 Zerx 的分层架构理念一致——领域模型不是数据表的简单映射，而是有行为、有边界的业务对象。

3. **轻量无侵入**：不依赖字节码增强（CGLIB）、不产生代理对象、不需要 `@Transactional` 之外的隐式上下文。实体就是普通 Java 对象（POJO），可以使用 JDK 21 的 Record 作为值对象。

4. **与 zerx-common 协同**：zerx-common 的 `Result<T>`、`PageRequest`、`PageResult`、异常体系、`ErrorCode` 可以直接与 Spring Data JDBC 的 Repository 返回值整合，无需额外适配。

**关键约束与应对：**

| 约束 | 应对方案 |
|------|---------|
| 无懒加载 | 合理控制聚合大小，大数据量用 DTO 投影 |
| 无自动多对多 | 中间表作为一等子实体，或用 `List<Long>` ID 引用 |
| 保存时子集合先删后插 | 热路径用自定义 `@Query` 增量更新 |
| 无动态 SQL | 简单条件用派生查询，复杂条件用 `JdbcTemplate` 兜底 |
| N+1 查询 | 开启 Single Query Loading（`JdbcMappingContext.setSingleQueryLoading(true)`） |

---

## 2 框架封装层 — zerx-spring

框架封装层对 Spring Boot 自身能力做实用性增强，目标是消除样板代码、统一编码规范。每个模块遵循 Spring Boot Starter 规范，通过 `AutoConfiguration.imports` 实现自动装配。

### 2.1 zerx-spring-web（Web 层增强）

> **核心价值**：统一 API 响应格式、全局异常处理、参数校验增强，消除每个 Controller 的重复代码。

**依赖**：`spring-boot-starter-web`、`zerx-common`

**核心能力**：

| 能力 | 说明 | 关键类/注解 |
|------|------|------------|
| 统一响应封装 | 所有 Controller 返回值自动包装为 `R<T>` | `ResponseBodyAdvice` + `@ResponseResult` |
| 全局异常处理 | 业务异常→标准响应，系统异常→友好提示 | `@RestControllerAdvice` + `GlobalExceptionHandler` |
| 参数校验增强 | JSR-380 校验失败返回统一格式，支持分组校验 | `@Validated` + `MethodArgumentNotValidException` 处理 |
| CORS 跨域 | 自动配置，支持配置文件控制 | `CorsFilter` 自动装配 |
| 请求上下文 | 当前请求的用户ID、租户ID、TraceID 等信息 | `RequestContextHolder` |

**API 设计**：

```java
// 统一响应体（复用 zerx-common 的 Result<T>，增加 HTTP 状态码语义）
// Controller 只需返回业务数据，ResponseBodyAdvice 自动包装
@GetMapping("/user/{id}")
public UserVO getUser(@PathVariable Long id) {
    return userService.getById(id);
    // 实际响应：{"code":"200","message":"success","data":{...}}
}

// 手动返回错误（使用 zerx-common 的异常体系）
@GetMapping("/order/{id}")
public OrderVO getOrder(@PathVariable Long id) {
    Order order = orderService.getById(id);
    if (order == null) {
        throw new NotFoundException(ErrorCode.ORDER_NOT_FOUND);
    }
    return order;
}
```

**配置项**：

```yaml
zerx:
  web:
    response-wrap:
      enabled: true              # 是否开启响应体自动包装
      exclude-packages:          # 排除的包路径（如 Swagger、Actuator）
        - org.springdoc
        - org.springframework.boot.actuator
    cors:
      enabled: true
      allowed-origins: "*"
      allowed-methods: GET,POST,PUT,DELETE,OPTIONS
      max-age: 3600
```

**目录结构**：

```
zerx-spring-web/
└── src/main/java/com/zerx/spring/web/
    ├── autoconfigure/
    │   └── ZerxWebAutoConfiguration.java
    ├── advice/
    │   ├── GlobalExceptionHandler.java
    │   └── ResponseBodyAdvice.java
    ├── config/
    │   ├── CorsAutoConfiguration.java
    │   └── WebMvcCustomizer.java
    └── properties/
        └── ZerxWebProperties.java
```

---

### 2.2 zerx-spring-data（数据访问增强）

> **核心价值**：基于 Spring Data JDBC 提供统一的数据访问增强，包括聚合根基类、自动审计、逻辑删除、分页、多数据源等通用能力，让开发者专注于领域建模而非基础设施。

**依赖**：`spring-boot-starter-data-jdbc`、`zerx-common`

**核心能力**：

| 能力 | 说明 | 关键类/注解 |
|------|------|------------|
| 聚合根基类 | 统一 ID、审计字段、逻辑删除字段 | `BaseAggregate` |
| 审计支持 | 创建时间/人、更新时间/人自动填充 | `@EnableJdbcAuditing` + `AuditorAware` |
| 逻辑删除 | 全局逻辑删除配置，复用 `DeleteFlag` 枚举 | `@ReadOnlyProperty` + `SoftDeleteRepository` |
| 通用分页 | 统一分页请求/响应，与 `PageRequest`/`PageResult` 整合 | `Pageable` + 自定义 `Slice` 适配 |
| 多数据源 | 动态数据源切换（主从、读写分离） | `@DataSource("slave")` + `AbstractRoutingDataSource` |
| 慢 SQL 检测 | 超过阈值的 SQL 自动记录 WARN 日志 | `JdbcAggregateTemplate` 包装 + `StatementInterceptor` |
| Single Query Loading | 解决 N+1 查询问题 | `JdbcMappingContext.setSingleQueryLoading(true)` |
| 自定义 Repository 基类 | 提供批量保存、存在性检查等通用方法 | `ZerxRepository` / `ZerxRepositoryImpl` |
| EntityCallback | 7 个生命周期钩子（BeforeSave/AfterSave 等） | `BeforeSaveCallback` `AfterLoadCallback` 等 |

**设计原则 — 聚合根建模规范：**

Spring Data JDBC 以聚合（Aggregate）为基本持久化单元。在 Zerx 中，聚合根设计遵循以下规范：

1. **每个聚合根对应一张主表**，子实体对应从表，通过外键关联
2. **聚合之间的引用通过 ID**，不直接持有其他聚合根的实体引用
3. **聚合根保护业务不变量**，修改状态通过行为方法而非 setter
4. **子实体使用 `@Collection` 注解**，Spring Data JDBC 自动级联保存
5. **值对象使用 Java Record**，通过 `@MappedCollection` 嵌入
6. **避免 `@Data` 注解**，使用 `@Getter` `@Setter` + `@EqualsAndHashCode(of = "id")` + `@ToString(exclude = {"children"})`

**API 设计**：

```java
// ─── 聚合根基类 ───
public abstract class BaseAggregate {
    @Id
    private Long id;

    @CreatedDate
    private LocalDateTime createTime;

    @LastModifiedDate
    private LocalDateTime updateTime;

    @CreatedBy
    private Long createBy;

    @LastModifiedBy
    private Long updateBy;

    @ReadOnlyProperty  // 不映射到数据库，由逻辑删除机制控制
    private Boolean deleted = false;
}

// ─── 聚合根实体示例 ───
@Table("sys_user")
public class User extends BaseAggregate {
    private String username;
    private String password;
    private String email;
    private String phone;

    @Enumerated(EnumType.STRING)
    private CommonStatus status;

    // 子实体集合（一对多：用户 → 角色）
    @MappedCollection(idColumn = "user_id")
    private List<UserRole> roles = new ArrayList<>();

    // 行为方法：保护业务不变量
    public void activate() {
        if (this.status == CommonStatus.DISABLED) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        this.status = CommonStatus.ENABLED;
    }

    public void changePassword(String newPassword, String encodedOldPassword) {
        if (!passwordEncoder.matches(encodedOldPassword, this.password)) {
            throw new ValidationException(ErrorCode.PASSWORD_MISMATCH);
        }
        this.password = passwordEncoder.encode(newPassword);
    }
}

// ─── 子实体 ───
public class UserRole {
    private Long userId;
    private Long roleId;
    private String roleName;
}

// ─── Repository ───
public interface UserRepository extends CrudRepository<User, Long>,
    ZerxRepository<User, Long> {

    // 派生查询
    Optional<User> findByUsername(String username);
    Optional<User> findByPhone(String phone);
    boolean existsByUsername(String username);
    List<User> findByStatus(CommonStatus status);

    // 自定义查询（DTO 投影）
    @Query("SELECT id, username, email FROM sys_user WHERE status = :status")
    List<UserSummary> findSummariesByStatus(@Param("status") String status);
}

// ─── DTO 投影（接口方式） ───
public interface UserSummary {
    Long getId();
    String getUsername();
    String getEmail();
}

// ─── 自定义 Repository 实现（Fragment 模式） ───
public interface ZerxRepository<T, ID> {
    boolean existsByIdAndDeletedFalse(ID id);
    List<T> findAllByDeletedFalse();
    long countByDeletedFalse();
}

public class ZerxRepositoryImpl<T, ID> implements ZerxRepository<T, ID> {
    private final JdbcAggregateTemplate template;
    private final AggregateProjectionFactory projectionFactory;

    public ZerxRepositoryImpl(JdbcAggregateTemplate template,
                               AggregateProjectionFactory projectionFactory) {
        this.template = template;
        this.projectionFactory = projectionFactory;
    }

    @Override
    public boolean existsByIdAndDeletedFalse(ID id) {
        return template.existsById(id); // 逻辑删除由 SQL WHERE 子句控制
    }

    @Override
    public List<T> findAllByDeletedFalse() {
        // 通过 EntityCallback 注入 deleted=0 条件
        return template.findAll(Class<T>);
    }
}

// ─── 逻辑删除机制（BeforeDeleteCallback + BeforeSaveCallback） ───
@Component
public class SoftDeleteCallback implements BeforeDeleteCallback<Object>,
    BeforeConvertCallback<Object> {

    @Override
    public Object onBeforeDelete(Object entity, MutableAggregateChange<Object> aggregateChange) {
        // 拦截 delete 操作，改为 UPDATE SET deleted = 1
        if (entity instanceof BaseAggregate agg) {
            aggregateChange.setSql("""
                UPDATE %s SET deleted = 1, update_time = NOW()
                WHERE id = :id AND deleted = 0
                """.formatted(aggregateChange.getTableName()));
        }
        return entity;
    }

    @Override
    public Object onBeforeConvert(Object entity) {
        // 查询时自动过滤 deleted = 1 的记录
        if (entity instanceof BaseAggregate agg && agg.getId() != null) {
            // 通过自定义 SelectQueryProvider 注入 WHERE deleted = 0
        }
        return entity;
    }
}

// ─── 审计提供者 ───
@Component
public class ZerxAuditorAware implements AuditorAware<Long> {
    @Override
    public Optional<Long> getCurrentAuditor() {
        return Optional.ofNullable(SecurityContextHolder.getUserId());
    }
}
```

**配置项**：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/zerx?useUnicode=true&characterEncoding=utf-8
    username: root
    password: xxx
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

zerx:
  data:
    jdbc:
      # 逻辑删除配置
      logic-delete:
        enabled: true
        field: deleted
        deleted-value: 1
        not-deleted-value: 0
      # Single Query Loading（解决 N+1）
      single-query-loading: true
      # 命名策略
      naming-strategy: SNAKE_CASE   # SNAKE_CASE / CAMEL_CASE
    slow-sql:
      enabled: true
      threshold: 1000ms             # 慢 SQL 阈值
    multi-datasource:
      enabled: false
      primary: master
      strict: false                 # 严格模式：未匹配数据源时抛异常
      datasource:
        master:
          url: jdbc:mysql://localhost:3306/zerx
          username: root
          password: xxx
        slave:
          url: jdbc:mysql://localhost:3307/zerx_slave
          username: root
          password: xxx
```

**目录结构**：

```
zerx-spring-data/
└── src/main/java/com/zerx/spring/data/
    ├── autoconfigure/
    │   ├── ZerxDataAutoConfiguration.java
    │   └── ZerxAuditingAutoConfiguration.java
    ├── repository/
    │   ├── ZerxRepository.java          # Fragment 接口
    │   └── ZerxRepositoryImpl.java      # Fragment 实现
    ├── domain/
    │   ├── BaseAggregate.java            # 聚合根基类
    │   └── SoftDeleteCallback.java       # 逻辑删除回调
    ├── audit/
    │   └── ZerxAuditorAware.java         # 审计提供者
    ├── datasource/
    │   └── DynamicRoutingDataSource.java # 动态数据源
    ├── config/
    │   └── JdbcCustomizer.java           # Single Query Loading 等配置
    └── properties/
        └── ZerxDataProperties.java
```

**多数据源使用**：

```java
@Service
public class OrderService {

    @DataSource("slave")     // 读从库
    public Order getById(Long id) {
        return orderRepository.findById(id)
            .filter(o -> !o.getDeleted())
            .orElseThrow(() -> new NotFoundException(ErrorCode.ORDER_NOT_FOUND));
    }

    @DataSource("master")    // 写主库（默认）
    public void create(Order order) {
        orderRepository.save(order);
    }
}
```

**复杂查询兜底 — JdbcTemplate**：

对于 Spring Data JDBC 派生查询无法覆盖的复杂 SQL 场景，直接注入 `JdbcTemplate` 作为降级方案。Spring Data JDBC 与 JdbcTemplate 共享同一个 DataSource，可以无缝混用。

```java
@Service
public class ReportService {
    private final JdbcTemplate jdbcTemplate;

    // 多表关联报表查询
    public List<MonthlySalesReport> getMonthlySales(int year) {
        return jdbcTemplate.query("""
            SELECT m.month, COALESCE(SUM(o.amount), 0) as total
            FROM months m
            LEFT JOIN orders o ON m.month = MONTH(o.create_time)
                AND YEAR(o.create_time) = ? AND o.deleted = 0
            GROUP BY m.month
            ORDER BY m.month
            """, new ReportRowMapper(), year);
    }
}
```

---

### 2.3 zerx-spring-cache（缓存抽象）

> **核心价值**：统一缓存操作 API，支持本地缓存（Caffeine）和分布式缓存（Redis）的平滑切换。

**依赖**：`spring-boot-starter-cache`、`caffeine`（可选）、`spring-boot-starter-data-redis`（可选）

**核心能力**：

| 能力 | 说明 |
|------|------|
| 多级缓存 | L1 本地（Caffeine）+ L2 分布式（Redis），减少网络开销 |
| 统一注解 | 扩展 `@Cacheable` / `@CacheEvict` / `@CachePut`，支持 TTL |
| 缓存工具类 | `CacheOps` 提供链式 API：`cacheOps.set("key", value, 10, MINUTES)` |
| 防穿透/防击穿/防雪崩 | 布隆过滤器 + 互斥锁 + 随机过期时间 |
| 缓存键生成 | SpEL 支持，统一前缀避免冲突 |

**API 设计**：

```java
// 注解式缓存（扩展 Spring Cache）
@Cacheable(value = "user", key = "#id", ttl = 30, timeUnit = TimeUnit.MINUTES)
public UserVO getById(Long id) { ... }

@CacheEvict(value = "user", key = "#id")
public void update(Long id, UserDTO dto) { ... }

// 工具类式缓存
@Autowired
private CacheOps cacheOps;

public UserVO getUser(Long id) {
    return cacheOps.get("user:" + id, () -> userRepository.findById(id).orElse(null), 30, MINUTES);
}
```

**配置项**：

```yaml
zerx:
  cache:
    type: redis                  # caffeine / redis / multilevel
    key-prefix: "zerx:"
    multilevel:
      l1:
        max-size: 1000
        expire-after-write: 5m
      l2:
        expire-after-write: 30m
```

---

### 2.4 zerx-spring-security（认证授权）

> **核心价值**：提供统一的认证授权基础设施，支持 JWT Token、OAuth2 等多种认证方式的灵活切换。

**依赖**：`spring-boot-starter-security`、`jjwt`（可选）、`zerx-common`

**核心能力**：

| 能力 | 说明 | 关键类 |
|------|------|--------|
| JWT Token | 签发、验证、刷新，无状态认证 | `JwtTokenProvider` |
| Token 过滤器 | 从请求头/参数提取 Token，设置安全上下文 | `JwtAuthenticationFilter` |
| 权限控制 | `@PreAuthorize` / `@RequiresPermissions` 注解式鉴权 | `SecurityExpressionHandler` |
| 登录/登出 | 统一登录接口，支持账号密码、手机验证码、OAuth2 | `AuthService` |
| 用户上下文 | 当前登录用户的 ID、角色、权限等信息 | `SecurityContextHolder`（增强） |
| 密码加密 | BCrypt 加密、密码策略校验 | `PasswordEncoder` |

**API 设计**：

```java
// 登录认证
public record LoginRequest(String username, String password) {}
public record LoginResponse(String accessToken, String refreshToken, long expiresIn) {}

@PostMapping("/auth/login")
public R<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
    return R.ok(authService.login(request));
}

// 获取当前用户
@GetMapping("/auth/me")
public R<UserVO> currentUser() {
    Long userId = SecurityContextHolder.getUserId();
    return R.ok(userService.getById(userId));
}

// 接口鉴权
@PreAuthorize("hasPermission('sys:user:add')")
@PostMapping("/user")
public R<Void> createUser(@RequestBody @Valid UserDTO dto) { ... }
```

**配置项**：

```yaml
zerx:
  security:
    enabled: true
    jwt:
      secret: "your-256-bit-secret-key-here"
      access-token-expire: 7200     # 2 小时
      refresh-token-expire: 604800  # 7 天
      issuer: zerx
      header-name: Authorization
      header-prefix: "Bearer "
    permit-urls:                    # 免认证 URL
      - /auth/login
      - /auth/register
      - /doc.html
      - /actuator/**
```

---

### 2.5 zerx-spring-mq（消息队列）

> **核心价值**：统一消息队列抽象，支持 RabbitMQ、Kafka、Redis Stream 的平滑切换。

**依赖**：`spring-boot-starter-amqp`（RabbitMQ，可选）、`spring-kafka`（Kafka，可选）

**核心能力**：

| 能力 | 说明 |
|------|------|
| 统一发送接口 | `MessageTemplate.send(topic, message)` 屏蔽中间件差异 |
| 消息监听注解 | `@ZerxListener(topic = "order.created")` 简化消费定义 |
| 可靠性保障 | 消息确认、重试、死信队列 |
| 消息序列化 | 支持 JSON / Protobuf |

**配置项**：

```yaml
zerx:
  mq:
    type: rabbitmq                  # rabbitmq / kafka / redis-stream
    producer:
      retry:
        enabled: true
        max-attempts: 3
        backoff: 1000ms
    consumer:
      concurrency: 5
      prefetch: 10
```

---

### 2.6 zerx-spring-doc（API 文档）

> **核心价值**：零配置集成 SpringDoc OpenAPI，自动生成 API 文档和交互式调试页面。

**依赖**：`springdoc-openapi-starter-webmvc-ui`

**核心能力**：

| 能力 | 说明 |
|------|------|
| 自动文档 | 扫描 Controller 自动生成 OpenAPI 3.0 文档 |
| 分组配置 | 按模块分组（系统管理、业务模块等） |
| 认证集成 | 自动识别 JWT Token，支持在线调试 |
| 自定义响应 | 统一 `R<T>` 响应体的文档展示 |

**配置项**：

```yaml
zerx:
  doc:
    enabled: true
    title: Zerx API
    version: ${zerx.version}
    description: Zerx 企业级开发平台接口文档
    grouping:
      enabled: true
```

---

### 2.7 zerx-spring-logging（请求日志）

> **核心价值**：Web 请求全链路日志记录，支持 TraceID 传递、敏感参数脱敏、慢请求告警。

**依赖**：`zerx-spring-web`、`zerx-common`

**核心能力**：

| 能力 | 说明 |
|------|------|
| TraceID | 自动生成请求唯一标识，贯穿请求链路 |
| 请求/响应日志 | 记录 URL、方法、参数、响应码、耗时 |
| 敏感参数脱敏 | 密码、Token 等字段自动脱敏（复用 `SensitiveLogFilter`） |
| 慢请求告警 | 响应时间超过阈值自动 WARN |
| 日志格式 | 支持纯文本和 JSON 格式切换 |

**配置项**：

```yaml
zerx:
  logging:
    enabled: true
    format: text                    # text / json
    slow-threshold: 3000ms
    sensitive-params:               # 需要脱敏的参数名
      - password
      - token
      - secret
    exclude-urls:
      - /actuator/**
      - /doc.html
```

---

### 2.8 zerx-spring-monitor（监控）

> **核心价值**：基于 Spring Boot Actuator 的监控增强，集成 Prometheus + Grafana 标准方案。

**依赖**：`spring-boot-starter-actuator`、`micrometer-registry-prometheus`

**核心能力**：

| 能力 | 说明 |
|------|------|
| 自定义健康检查 | 数据库、Redis、消息队列连接状态 |
| JVM 指标 | 内存、GC、线程池 |
| 业务指标 | 接口 QPS、响应时间、错误率 |
| 端点暴露 | `/actuator/health`、`/actuator/prometheus`、`/actuator/info` |

**配置项**：

```yaml
zerx:
  monitor:
    enabled: true
    endpoints:
      web:
        exposure:
          include: health,info,prometheus,metrics
```

---

### 2.9 zerx-spring-bom（框架层 BOM）

> 统一管理 zerx-spring 所有模块及第三方框架版本。

```xml
<dependencyManagement>
    <dependencies>
        <!-- Spring Boot BOM -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- zerx-core BOM -->
        <dependency>
            <groupId>com.zerx</groupId>
            <artifactId>zerx-core-bom</artifactId>
            <version>${zerx.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- zerx-spring 内部模块 -->
        <dependency>
            <groupId>com.zerx</groupId>
            <artifactId>zerx-spring-web</artifactId>
            <version>${zerx.version}</version>
        </dependency>
        <dependency>
            <groupId>com.zerx</groupId>
            <artifactId>zerx-spring-data</artifactId>
            <version>${zerx.version}</version>
        </dependency>
        <!-- ... 其他模块 ... -->
    </dependencies>
</dependencyManagement>
```

**第三方版本管理**：

| 依赖 | 版本 | 用途 |
|------|------|------|
| `spring-boot-dependencies` | 3.3.5 | Spring Boot 全家桶 BOM |
| `springdoc-openapi` | 2.6.0 | OpenAPI 3.0 文档生成 |
| `jjwt` | 0.12.6 | JWT Token 签发/验证 |
| `redisson` | 3.35.0 | 分布式锁、限流 |
| `easyexcel` | 4.0.3 | Excel 导入导出 |
| `caffeine` | 3.1.8 | 本地缓存 |
| `bucket4j` | 8.7.0 | 令牌桶限流 |

---

## 3 业务组件层 — zerx-component

业务组件层面向具体的企业业务场景，每个组件解决一个高频通用问题。设计范式为 **「注解 + AOP + 策略模式」**，业务代码零侵入。

### 3.1 zerx-component-log（操作日志）

> **核心价值**：一个注解记录用户操作行为，用于审计追溯。

**依赖**：`zerx-spring-web`、`spring-boot-starter-aop`

**核心注解**：

```java
// 注解定义
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {
    /** 模块名称，如 "用户管理" */
    String title() default "";
    /** 操作类型 */
    BusinessType businessType() default BusinessType.OTHER;
    /** 是否保存请求参数 */
    boolean isSaveRequestData() default true;
    /** 是否保存响应参数 */
    boolean isSaveResponseData() default false;
    /** 排除的参数（敏感信息） */
    String[] excludeParams() default {};
}

public enum BusinessType {
    OTHER, INSERT, UPDATE, DELETE, EXPORT, IMPORT, GRANT, CLEAN
}
```

**使用示例**：

```java
@Log(title = "用户管理", businessType = BusinessType.INSERT)
@PostMapping("/user")
public R<Void> create(@RequestBody @Valid UserDTO dto) { ... }
```

**设计要点**：
- AOP 切面拦截 `@Log` 注解方法，异步写入日志（`@Async` / `ApplicationEvent`）
- 自动提取操作人、IP、请求参数、响应结果、耗时、异常信息
- 日志存储抽象为 `LogStorageService` 接口，默认数据库实现，可扩展为 ES/MQ

**配置项**：

```yaml
zerx:
  log:
    enabled: true
    async: true                    # 异步写入
    exclude-urls: /actuator/**
```

---

### 3.2 zerx-component-excel（Excel 操作）

> **核心价值**：基于 EasyExcel 的大数据量 Excel 导入导出，注解驱动，一行代码搞定。

**依赖**：`easyexcel`（4.x）、`zerx-common`

**核心 API**：

```java
// 导出
@GetMapping("/user/export")
public void export(HttpServletResponse response) {
    List<UserVO> list = userService.list();
    ExcelUtil.export(response, "用户列表", UserVO.class, list);
}

// 导入
@PostMapping("/user/import")
public R<ImportResult> importUsers(@RequestParam MultipartFile file) {
    ImportResult result = ExcelUtil.importExcel(file, UserImportVO.class, listener);
    return R.ok(result);
}

// 实体定义（使用 EasyExcel 注解）
public class UserVO {
    @ExcelProperty("用户名")
    private String username;

    @ExcelProperty("手机号")
    private String phone;

    @ExcelIgnore
    private String password;

    @ExcelProperty("状态")
    @DictFormat(dictType = "sys_status")  // 字典翻译
    private Integer status;
}
```

**设计要点**：
- 提供 `ExcelUtil.export()` / `ExcelUtil.importExcel()` 静态工具方法
- 大数据量导出采用分批写入，避免 OOM
- 导入支持校验监听器（`AnalysisEventListener`），实时反馈错误
- 支持密码加密列（`@ExcelEncrypt`）
- 支持字典翻译（`@DictFormat`）

**配置项**：

```yaml
zerx:
  excel:
    threshold: 5000               # 内存阈值，超过则分批写入
```

---

### 3.3 zerx-component-oss（对象存储）

> **核心价值**：统一文件上传/下载/管理接口，策略模式支持 MinIO、阿里云 OSS、腾讯 COS 等多厂商。

**依赖**：`zerx-common`，可选依赖各厂商 SDK

**核心接口**：

```java
// 统一存储接口
public interface OssStorageService {
    /** 上传文件 */
    OssResult upload(MultipartFile file);
    /** 上传流 */
    OssResult upload(String path, InputStream inputStream, String contentType);
    /** 下载文件 */
    void download(String filePath, OutputStream outputStream);
    /** 删除文件 */
    void delete(String filePath);
    /** 获取访问 URL */
    String getUrl(String filePath);
}

// 上传结果
public record OssResult(String url, String filePath, String originalFilename, long size) {}
```

**使用示例**：

```java
@Autowired
private OssStorageService ossService;

@PostMapping("/upload")
public R<OssResult> upload(@RequestParam MultipartFile file) {
    return R.ok(ossService.upload(file));
}
```

**策略模式实现**：

```
OssStorageService (接口)
├── MinioOssStorageService     ← type=minio
├── AliyunOssStorageService    ← type=aliyun
├── TencentCosStorageService   ← type=tencent
└── S3OssStorageService        ← type=s3 (通用 S3 协议)
```

**配置项**：

```yaml
zerx:
  oss:
    enabled: true
    type: minio                    # minio / aliyun / tencent / s3
    endpoint: http://localhost:9000
    access-key: your-access-key
    secret-key: your-secret-key
    bucket-name: zerx
    region: us-east-1
    domain: https://oss.example.com   # 自定义 CDN 域名
```

---

### 3.4 zerx-component-sms（短信服务）

> **核心价值**：统一短信发送接口，策略模式支持阿里云、腾讯云、华为云等多厂商，内置验证码场景。

**依赖**：`zerx-spring-cache`（验证码缓存）、`zerx-common`

**核心接口**：

```java
// 短信发送服务
public interface SmsService {
    /** 发送模板短信 */
    void sendTemplate(String phone, String templateId, Map<String, String> params);
}

// 验证码服务（内置场景）
public interface SmsCodeService {
    /** 发送验证码 */
    void sendCode(String phone, SmsScene scene);
    /** 校验验证码 */
    boolean verifyCode(String phone, String code, SmsScene scene);
}

// 验证码场景枚举
public enum SmsScene {
    LOGIN,          // 登录
    REGISTER,       // 注册
    RESET_PASSWORD, // 重置密码
    BIND_PHONE      // 绑定手机
}
```

**使用示例**：

```java
// 发送验证码
@PostMapping("/sms/code")
public R<Void> sendCode(@RequestParam String phone, @RequestParam SmsScene scene) {
    smsCodeService.sendCode(phone, scene);
    return R.ok();
}

// 校验验证码
@PostMapping("/sms/verify")
public R<Void> verifyCode(@RequestParam String phone, @RequestParam String code, @RequestParam SmsScene scene) {
    boolean success = smsCodeService.verifyCode(phone, code, scene);
    return success ? R.ok() : R.fail("验证码错误");
}
```

**策略模式实现**：

```
SmsService (接口)
├── AliyunSmsService           ← sms.type=aliyun
├── TencentSmsService          ← sms.type=tencent
├── HuaweiSmsService           ← sms.type=huawei
└── MockSmsService             ← sms.type=mock (开发环境)
```

**安全机制**：
- 验证码有效期 5 分钟（可配置）
- 同手机号 60 秒发送间隔（基于 Redis `SETNX`）
- IP 维度限流（同 IP 每小时最多 10 条）
- 单手机号每日发送上限（20 条）

**配置项**：

```yaml
zerx:
  sms:
    enabled: true
    type: aliyun                   # aliyun / tencent / huawei / mock
    aliyun:
      access-key-id: xxx
      access-key-secret: xxx
      sign-name: Zerx
      template-codes:
        login: SMS_123456
        register: SMS_123457
    code:
      expire: 300                  # 验证码有效期（秒）
      interval: 60                 # 发送间隔（秒）
      daily-limit: 20              # 单手机号日发送上限
      ip-hourly-limit: 10          # 单 IP 小时上限
```

---

### 3.5 zerx-component-mail（邮件服务）

> **核心价值**：基于 JavaMail + Thymeleaf 模板的邮件发送组件，支持 HTML 邮件、附件、异步发送。

**依赖**：`spring-boot-starter-mail`、`thymeleaf`（可选）

**核心接口**：

```java
// 邮件服务
public interface MailService {
    /** 发送纯文本邮件 */
    void sendSimple(String to, String subject, String content);
    /** 发送 HTML 邮件 */
    void sendHtml(String to, String subject, String htmlContent);
    /** 发送模板邮件（Thymeleaf） */
    void sendTemplate(String to, String subject, String templateName, Map<String, Object> variables);
    /** 发送带附件的邮件 */
    void sendWithAttachment(String to, String subject, String content, List<File> attachments);
}
```

**配置项**：

```yaml
spring:
  mail:
    host: smtp.qq.com
    port: 587
    username: xxx@qq.com
    password: xxx
    default-encoding: UTF-8

zerx:
  mail:
    enabled: true
    async: true                    # 异步发送
    from-name: Zerx Platform
```

---

### 3.6 zerx-component-lock（分布式锁）

> **核心价值**：注解式分布式锁，基于 Redisson 实现，支持可重入锁、读写锁、联锁。

**依赖**：`redisson-spring-boot-starter`、`zerx-spring-cache`

**核心注解**：

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    /** 锁的 Key，支持 SpEL */
    String key();
    /** 等待时间（秒），0 表示不等待 */
    long waitTime() default 0;
    /** 锁持有时间（秒），-1 表示不自动释放 */
    long leaseTime() default 30;
    /** 锁类型 */
    LockType lockType() default LockType.REENTRANT;
}

public enum LockType {
    REENTRANT,    // 可重入锁
    FAIR,         // 公平锁
    READ,         // 读锁
    WRITE,        // 写锁
}
```

**使用示例**：

```java
// 库存扣减：防止并发超卖
@DistributedLock(key = "'stock:' + #productId", waitTime = 3, leaseTime = 10)
public void deductStock(Long productId, int quantity) { ... }

// 订单创建：防重复提交
@DistributedLock(key = "'order:create:' + #userId", waitTime = 0, leaseTime = 5)
public Order createOrder(Long userId, OrderDTO dto) { ... }
```

**配置项**：

```yaml
zerx:
  lock:
    enabled: true
    type: redisson
```

---

### 3.7 zerx-component-idempotent（幂等/防重）

> **核心价值**：注解式接口幂等性保证，防止网络重试或用户连击导致的重复操作。

**依赖**：`zerx-spring-cache`（基于 Redis `SETNX`）

**核心注解**：

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    /** 幂等 Key，支持 SpEL */
    String key() default "";
    /** 幂等窗口期（秒） */
    long window() default 5;
    /** 提示消息 */
    String message() default "请勿重复操作";
    /** 幂等维度 */
    IdempotentType type() default IdempotentType.PARAM;
}

public enum IdempotentType {
    PARAM,     // 基于请求参数
    TOKEN,     // 基于前端幂等 Token
    IP,        // 基于 IP
}
```

**使用示例**：

```java
@Idempotent(key = "'order:create:' + #userId", window = 10)
@PostMapping("/order")
public R<Order> createOrder(@RequestBody OrderDTO dto) { ... }

@Idempotent(type = IdempotentType.TOKEN)
@PostMapping("/payment")
public R<PaymentResult> pay(@RequestBody PaymentDTO dto) { ... }
```

**配置项**：

```yaml
zerx:
  idempotent:
    enabled: true
    default-window: 5             # 默认窗口期（秒）
```

---

### 3.8 zerx-component-ratelimit（接口限流）

> **核心价值**：注解式接口限流，支持单机（Guava/Bucket4j）和分布式（Redis）两种模式。

**依赖**：`zerx-spring-cache`（Redis 模式）

**核心注解**：

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimiter {
    /** 限流阈值（次/时间窗口） */
    double value() default 100;
    /** 时间窗口 */
    long timeout() default 60;
    /** 时间单位 */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    /** 限流维度 */
    LimitType limitType() default LimitType.GLOBAL;
    /** 提示消息 */
    String message() default "操作过于频繁，请稍后再试";
}

public enum LimitType {
    GLOBAL,     // 全局限流
    IP,         // IP 限流
    USER,       // 用户限流
}
```

**使用示例**：

```java
// 发送短信：每分钟最多 1 次
@RateLimiter(value = 1, timeout = 60, limitType = LimitType.IP)
@PostMapping("/sms/code")
public R<Void> sendCode(...) { ... }

// 查询接口：每秒最多 100 次
@RateLimiter(value = 100, timeout = 1, limitType = LimitType.GLOBAL)
@GetMapping("/user/list")
public R<PageResult<UserVO>> list(...) { ... }
```

**配置项**：

```yaml
zerx:
  ratelimit:
    enabled: true
    type: redis                    # local (Guava) / redis
```

---

### 3.9 zerx-component-datascope（数据权限）

> **核心价值**：基于 Spring Data JDBC 自定义查询包装的行级数据权限控制，不同角色看到不同范围的数据。

**依赖**：`zerx-spring-data`、`zerx-spring-security`

**设计思路（Spring Data JDBC 方案）**：

与 MyBatis 拦截器不同，Spring Data JDBC 没有 SQL 拦截器机制。数据权限通过以下三种方式实现：

1. **Repository 方法级别**：自定义 Repository 方法名包含权限维度（如 `findByDeptIdIn`、`findByCreateBy`）
2. **AOP + 查询包装**：通过 `@DataScope` 注解 + AOP 拦截，在执行查询前动态注入权限条件
3. **自定义 AbstractRepositoryImpl**：在基类中统一处理数据权限条件拼接

**核心注解**：

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {
    /** 部门表别名 */
    String deptAlias() default "d";
    /** 用户表别名 */
    String userAlias() default "u";
}
```

**权限等级**（由角色配置决定）：

| 等级 | 说明 | 查询方式 |
|------|------|---------|
| `ALL` | 全部数据 | 无追加 |
| `CUSTOM` | 自定义部门 | `WHERE dept_id IN (...)` |
| `DEPT` | 本部门 | `WHERE dept_id = #{currentDeptId}` |
| `DEPT_AND_CHILD` | 本部门及下级 | `WHERE dept_id IN (递归子部门)` |
| `SELF` | 仅本人 | `WHERE create_by = #{currentUserId}` |

**实现方案 — AOP + DataScopeContext**：

```java
// 数据权限上下文（ThreadLocal）
public class DataScopeContext {
    private static final ThreadLocal<DataScopeInfo> HOLDER = new ThreadLocal<>();

    public static void set(DataScopeInfo info) { HOLDER.set(info); }
    public static DataScopeInfo get() { return HOLDER.get(); }
    public static void clear() { HOLDER.remove(); }

    public record DataScopeInfo(
        Long userId, Long deptId, Set<Long> deptIds, String scopeType
    ) {}
}

// AOP 切面：拦截 @DataScope 方法
@Aspect
@Component
public class DataScopeAspect {

    @Autowired
    private RoleService roleService;

    @Before("@annotation(dataScope)")
    public void beforeQuery(JoinPoint joinPoint, DataScope dataScope) {
        Long userId = SecurityContextHolder.getUserId();
        String scopeType = roleService.getDataScopeType(userId);
        Set<Long> deptIds = roleService.getDataScopeDeptIds(userId);
        Long deptId = SecurityContextHolder.getDeptId();

        DataScopeContext.set(new DataScopeInfo(userId, deptId, deptIds, scopeType));
    }

    @After("@annotation(dataScope)")
    public void afterQuery(JoinPoint joinPoint, DataScope dataScope) {
        DataScopeContext.clear();
    }
}

// 数据权限查询工具类
@Component
public class DataScopeHelper {

    public static String appendScopeCondition(String baseSql, String userAlias) {
        DataScopeInfo info = DataScopeContext.get();
        if (info == null || "ALL".equals(info.scopeType())) {
            return baseSql;
        }

        String condition = switch (info.scopeType()) {
            case "SELF" -> userAlias + ".create_by = " + info.userId();
            case "DEPT" -> "dept_id = " + info.deptId();
            case "DEPT_AND_CHILD" -> "dept_id IN (" +
                String.join(",", info.deptIds().stream().map(String::valueOf).toList()) + ")";
            case "CUSTOM" -> "dept_id IN (" +
                String.join(",", info.deptIds().stream().map(String::valueOf).toList()) + ")";
            default -> "1=1";
        };

        return baseSql + " AND " + condition;
    }
}
```

**使用示例**：

```java
// 方式一：注解 + JdbcTemplate 自定义查询
@DataScope(deptAlias = "d", userAlias = "u")
@GetMapping("/user/list")
public R<PageResult<UserVO>> list(UserQuery query) {
    String sql = "SELECT * FROM sys_user u WHERE u.deleted = 0";
    sql = DataScopeHelper.appendScopeCondition(sql, "u");
    // 使用 JdbcTemplate 执行
    return R.ok(jdbcTemplate.query(sql, new UserVORowMapper()));
}

// 方式二：Repository 派生查询（适用于简单场景）
public interface UserRepository extends CrudRepository<User, Long> {
    // 仅本人数据
    List<User> findByCreateByAndDeletedFalse(Long userId);

    // 本部门数据
    List<User> findByDeptIdAndDeletedFalse(Long deptId);

    // 本部门及子部门数据
    @Query("SELECT * FROM sys_user WHERE dept_id IN (:deptIds) AND deleted = 0")
    List<User> findByDeptIdInAndDeletedFalse(@Param("deptIds") Set<Long> deptIds);
}
```

---

### 3.10 zerx-component-tenant（多租户）

> **核心价值**：基于 Spring Data JDBC 的多租户 SaaS 支持，自动注入 `tenant_id` 条件。

**依赖**：`zerx-spring-data`

**设计思路（Spring Data JDBC 方案）**：

Spring Data JDBC 没有类似 MyBatis 的 SQL 拦截器机制。多租户隔离通过以下方式实现：

1. **AfterLoadCallback**：加载时自动校验 tenant_id 匹配
2. **BeforeSaveCallback**：保存时自动填充 tenant_id
3. **自定义 Repository 基类**：所有查询方法自动追加 `WHERE tenant_id = ?` 条件
4. **TenantContextHolder**（ThreadLocal）管理当前租户上下文

**租户上下文**：

```java
// 租户上下文（ThreadLocal）
public class TenantContextHolder {
    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    public static void setTenantId(Long tenantId) { HOLDER.set(tenantId); }
    public static Long getTenantId() { return HOLDER.get(); }
    public static void clear() { HOLDER.remove(); }
    public static boolean isSystemTenant() {
        return getTenantId() == null || getTenantId() == 0L;
    }
}

// 聚合根基类增加 tenantId
public abstract class BaseAggregate {
    @Id
    private Long id;

    private Long tenantId;  // 租户 ID

    // ... 其他审计字段
}
```

**自动填充租户 ID（BeforeConvertCallback）**：

```java
@Component
public class TenantCallback implements BeforeConvertCallback<BaseAggregate> {

    @Override
    public BaseAggregate onBeforeConvert(BaseAggregate aggregate) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            aggregate.setTenantId(tenantId);
        }
        return aggregate;
    }
}
```

**自定义 Repository 自动追加租户条件**：

```java
public interface TenantAwareRepository<T, ID> extends CrudRepository<T, ID> {
    // 所有查询方法在自定义实现中自动追加 tenant_id 条件
}

public class TenantAwareRepositoryImpl<T extends BaseAggregate, ID>
    implements TenantAwareRepository<T, ID> {

    private final JdbcAggregateTemplate template;
    private final JdbcMappingContext mappingContext;

    @Override
    public Optional<T> findById(ID id) {
        Long tenantId = TenantContextHolder.getTenantId();
        // 使用自定义查询确保 tenant_id 匹配
        return template.findById(id, entityClass)
            .filter(entity -> Objects.equals(entity.getTenantId(), tenantId));
    }

    @Override
    public Iterable<T> findAll() {
        Long tenantId = TenantContextHolder.getTenantId();
        // 使用 @Query 或 JdbcTemplate 自动追加 tenant_id
        return template.findAll(entityClass).stream()
            .filter(e -> Objects.equals(e.getTenantId(), tenantId))
            .toList();
    }
}
```

**忽略表配置**：

```yaml
zerx:
  tenant:
    enabled: false               # 默认关闭
    column: tenant_id
    ignore-tables:               # 不需要租户隔离的表
      - sys_user
      - sys_dict_type
      - sys_config
```

---

### 3.11 zerx-component-pay（支付集成）

> **核心价值**：统一支付接口，策略模式支持微信支付、支付宝，覆盖下单、回调、退款、查单全流程。

**依赖**：各支付渠道 SDK

**核心接口**：

```java
public interface PayService {
    /** 创建支付订单 */
    PayResult createOrder(PayOrder order);
    /** 处理支付回调 */
    boolean handleCallback(Map<String, String> params);
    /** 查询支付状态 */
    PayStatus queryOrder(String orderNo);
    /** 退款 */
    RefundResult refund(String orderNo, BigDecimal amount);
}
```

**策略模式实现**：

```
PayService (接口)
├── WechatPayService            ← pay.type=wechat
├── AlipayService               ← pay.type=alipay
└── MockPayService              ← pay.type=mock (开发环境)
```

**配置项**：

```yaml
zerx:
  pay:
    enabled: false               # 按需开启
    type: wechat
    wechat:
      app-id: xxx
      mch-id: xxx
      api-key: xxx
      cert-path: /path/to/cert
      notify-url: https://example.com/pay/notify/wechat
    alipay:
      app-id: xxx
      private-key: xxx
      alipay-public-key: xxx
      notify-url: https://example.com/pay/notify/alipay
```

---

### 3.12 zerx-component-bom（组件层 BOM）

> 统一管理 zerx-component 所有模块版本。

```xml
<dependencyManagement>
    <dependencies>
        <!-- zerx-spring BOM -->
        <dependency>
            <groupId>com.zerx</groupId>
            <artifactId>zerx-spring-bom</artifactId>
            <version>${zerx.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- zerx-component 内部模块 -->
        <dependency>
            <groupId>com.zerx</groupId>
            <artifactId>zerx-component-log</artifactId>
            <version>${zerx.version}</version>
        </dependency>
        <!-- ... 其他模块 ... -->
    </dependencies>
</dependencyManagement>
```

---

## 4 统一设计规范

### 4.1 自动配置规范

每个模块遵循 Spring Boot 3.x 自动配置规范：

1. **入口文件**：`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
2. **条件装配**：使用 `@ConditionalOnProperty`、`@ConditionalOnClass`、`@ConditionalOnWebApplication` 等
3. **属性绑定**：使用 `@ConfigurationProperties` + `zerx.` 前缀
4. **Auto-Configuration 排序**：使用 `@AutoConfigureBefore` / `@AutoConfigureAfter` 控制装配顺序

```java
// 标准自动配置模板
@AutoConfiguration
@ConditionalOnProperty(prefix = "zerx.xxx", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ZerxXxxProperties.class)
public class ZerxXxxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public XxxService xxxService(ZerxXxxProperties properties) {
        return new XxxServiceImpl(properties);
    }
}
```

### 4.2 注解驱动规范

业务组件统一使用注解驱动模式：

| 组件 | 注解 | 拦截方式 | 处理逻辑 |
|------|------|---------|---------|
| 操作日志 | `@Log` | AOP `@Around` | 异步记录操作日志 |
| 分布式锁 | `@DistributedLock` | AOP `@Around` | Redisson 加锁/解锁 |
| 幂等防重 | `@Idempotent` | AOP `@Around` | Redis SETNX 校验 |
| 接口限流 | `@RateLimiter` | AOP `@Around` | 滑动窗口 / 令牌桶 |
| 数据权限 | `@DataScope` | AOP `@Before`/`@After` | ThreadLocal 注入查询条件 |

### 4.3 策略模式规范（多厂商适配）

多厂商适配统一采用策略模式 + Spring 条件装配：

```
接口定义（api 包）
├── XxxService.java               # 统一接口
└── XxxConfig.java                # 配置属性

实现类（provider 包）
├── AliyunXxxService.java         # 阿里云实现
├── TencentXxxService.java        # 腾讯云实现
├── HuaweiXxxService.java         # 华为云实现
└── MockXxxService.java           # Mock 实现（测试用）

自动配置
└── XxxAutoConfiguration.java     # 根据 type 配置动态注册 Bean
```

### 4.4 配置前缀规范

所有 Zerx 组件配置统一使用 `zerx.` 前缀：

```yaml
zerx:
  web: { ... }
  data: { ... }
  cache: { ... }
  security: { ... }
  log: { ... }
  excel: { ... }
  oss: { ... }
  sms: { ... }
  mail: { ... }
  lock: { ... }
  idempotent: { ... }
  ratelimit: { ... }
  tenant: { ... }
  pay: { ... }
```

### 4.5 异步化规范

涉及 IO 操作的组件统一支持异步模式：

- 使用 `ThreadPoolTaskExecutor` 自定义线程池
- `@Async("zerxTaskExecutor")` 注解标记异步方法
- 事件驱动场景使用 `ApplicationEventPublisher` 替代直接 `@Async`
- 线程池参数统一在 `ZerxAsyncProperties` 中管理

---

## 5 模块依赖关系

### 5.1 zerx-spring 内部依赖

```
zerx-spring-web ─────────┐
zerx-spring-data ────────┤
zerx-spring-cache ───────┤──→ zerx-common
zerx-spring-security ────┤
zerx-spring-mq ──────────┤
zerx-spring-doc ─────────┘

zerx-spring-logging ──────→ zerx-spring-web
zerx-spring-monitor ──────→ zerx-spring-web
zerx-spring-bom ──────────→ (纯 BOM，无代码依赖)
```

### 5.2 zerx-component 内部依赖

```
zerx-component-log ─────────────→ zerx-spring-web
zerx-component-excel ───────────→ zerx-common
zerx-component-oss ─────────────→ zerx-common
zerx-component-sms ─────────────→ zerx-spring-cache, zerx-spring-web
zerx-component-mail ────────────→ zerx-common
zerx-component-lock ────────────→ zerx-spring-cache
zerx-component-idempotent ──────→ zerx-spring-cache, zerx-spring-web
zerx-component-ratelimit ───────→ zerx-spring-cache, zerx-spring-web
zerx-component-datascope ───────→ zerx-spring-data, zerx-spring-security
zerx-component-tenant ──────────→ zerx-spring-data
zerx-component-pay ─────────────→ zerx-common
zerx-component-bom ─────────────→ (纯 BOM，无代码依赖)
```

### 5.3 全局依赖图

```
                    zerx-common (零依赖)
                         │
        ┌────────────────┼────────────────┐
        │                │                │
   zerx-spring-web   zerx-spring-data  zerx-spring-cache
   zerx-spring-security zerx-spring-mq  zerx-spring-doc
        │                │                │
        └────────────────┼────────────────┘
                         │
              ┌──────────┼──────────┐
              │                     │
    zerx-component-log      zerx-component-excel
    zerx-component-sms      zerx-component-oss
    zerx-component-lock     zerx-component-mail
    zerx-component-idempotent  zerx-component-pay
    zerx-component-ratelimit   zerx-component-datascope
    zerx-component-tenant
```

---

## 6 实施路线图

### Phase 1：框架封装层基础（预计 2-3 周）

| 序号 | 模块 | 核心能力 | 优先级 |
|------|------|---------|--------|
| 1.1 | zerx-spring-bom | Spring Boot BOM + 版本管理 | P0 |
| 1.2 | zerx-spring-web | 全局异常处理 + 响应封装 + CORS | P0 |
| 1.3 | zerx-spring-data | Spring Data JDBC + 聚合根基类 + 审计 + 逻辑删除 | P0 |
| 1.4 | zerx-spring-security | JWT Token + Spring Security 6.x | P0 |
| 1.5 | zerx-spring-cache | Redis / Caffeine 缓存抽象 | P1 |
| 1.6 | zerx-spring-doc | SpringDoc OpenAPI 集成 | P1 |
| 1.7 | zerx-spring-logging | TraceID + 请求日志 + 脱敏 | P1 |
| 1.8 | zerx-spring-monitor | Actuator + Prometheus | P2 |
| 1.9 | zerx-spring-mq | RabbitMQ / Kafka 抽象 | P2 |

### Phase 2：核心业务组件（预计 2-3 周）

| 序号 | 模块 | 核心能力 | 优先级 |
|------|------|---------|--------|
| 2.1 | zerx-component-bom | 组件层 BOM | P0 |
| 2.2 | zerx-component-log | 操作日志 `@Log` | P0 |
| 2.3 | zerx-component-lock | 分布式锁 `@DistributedLock` | P0 |
| 2.4 | zerx-component-idempotent | 幂等防重 `@Idempotent` | P0 |
| 2.5 | zerx-component-ratelimit | 接口限流 `@RateLimiter` | P0 |
| 2.6 | zerx-component-excel | EasyExcel 导入导出 | P1 |
| 2.7 | zerx-component-oss | 对象存储 | P1 |
| 2.8 | zerx-component-sms | 短信服务 | P1 |
| 2.9 | zerx-component-mail | 邮件服务 | P2 |

### Phase 3：高级业务组件（预计 2 周）

| 序号 | 模块 | 核心能力 | 优先级 |
|------|------|---------|--------|
| 3.1 | zerx-component-datascope | 数据权限（Spring Data JDBC 方案） | P1 |
| 3.2 | zerx-component-tenant | 多租户（ThreadLocal + EntityCallback） | P1 |
| 3.3 | zerx-component-pay | 支付集成 | P2 |

### 测试策略

| 层级 | 策略 | 工具 |
|------|------|------|
| 单元测试 | 每个工具类、组件核心逻辑 100% 覆盖 | JUnit 5 + Mockito |
| 集成测试 | Repository 层 + 自动配置 | Spring Boot Test + H2 |
| 数据库测试 | 跨数据库兼容性验证 | TestContainers (MySQL / PostgreSQL) |
| 架构测试 | 模块依赖隔离、包结构约束 | ArchUnit |

---

> **文档变更记录**
>
> | 版本 | 日期 | 变更内容 |
> |------|------|---------|
> | v1.0.0 | 2026-05-16 | 初始版本（基于 MyBatis-Plus） |
> | v2.0.0 | 2026-05-16 | 数据访问层由 MyBatis-Plus 切换为 Spring Data JDBC；重写 zerx-spring-data 设计；重写数据权限和多租户实现方案；新增技术选型决策章节 |
