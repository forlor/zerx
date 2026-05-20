# Zerx Spring Data Module Design

## 1. 模块概述

Zerx Spring Data 是基于 Spring Data JDBC 的数据访问层增强框架，为业务系统提供统一的聚合根基类、链式 SQL 构建器、AOP 数据权限、归档机制、慢 SQL 检测、高性能批量操作等通用能力。本模块采用**归档优先删除（Archive-Before-Delete）**模式替代传统逻辑删除，确保主表查询性能不受历史数据影响。

### 设计原则

| 原则 | 实现方式 |
|------|---------|
| 不使用逻辑删除 | 通过归档-before-删除模式替代，主表不保留已删除数据，避免大表查询性能劣化 |
| 归档-before-删除 | 删除前先将数据复制到 `_archive` 归档表，归档失败则阻止物理删除 |
| SQL 注入防护 | DynamicQuery 所有参数通过 PreparedStatement 绑定；数据权限使用参数化条件 |
| SPI 可扩展 | `Archiver`、`DataScopeUserProvider` 接口由业务实现，框架自动发现并集成 |
| 零配置默认 | 慢 SQL 检测、Single Query Loading 等默认开启，无需额外配置 |
| 无 Web 编译依赖 | 审计感知通过反射访问 `RequestContext`，data 模块不直接依赖 web 模块 |

---

## 2. 模块结构

```
zerx-spring-data/
├── pom.xml
└── src/main/java/com/zerx/spring/data/
    ├── autoconfigure/
    │   ├── ZerxDataAutoConfiguration    自动配置入口（Bean 注册）
    │   └── ArchivePurgeTask             归档过期数据定时清理
    ├── domain/
    │   └── BaseEntity                   聚合根基类（ID、审计字段、乐观锁）
    ├── query/
    │   └── DynamicQuery                 链式 SQL 构建器
    ├── datascope/
    │   ├── @DataScope                   数据权限注解
    │   ├── DataScopeInterceptor         AOP 拦截器
    │   ├── DataScopeHandler             SQL 条件生成器（递归 CTE）
    │   ├── DataScopeContext             ThreadLocal 上下文
    │   ├── DataScopeUser                用户上下文信息（record）
    │   └── DataScopeUserProvider        SPI：用户信息提供者接口
    ├── archive/
    │   ├── Archiver<T>                  SPI：归档策略接口
    │   ├── JdbcArchiveRepository<T>     默认实现：数据库归档表
    │   ├── ArchiveCallback              BeforeDeleteCallback 生命周期回调
    │   ├── ArchiveService               归档服务门面
    │   ├── ArchiveProperties            归档配置属性
    │   └── ArchiveException             归档异常
    ├── repository/
    │   ├── ZerxRepository<T, ID>        通用 Repository 接口
    │   ├── ZerxRepositoryHelper         Repository 增强组件
    │   └── BatchOperations              JDBC 批量操作工具
    ├── audit/
    │   └── ZerxAuditorAware             审计感知（反射访问 RequestContext）
    ├── config/
    │   ├── SlowSqlInterceptor           慢 SQL 检测拦截器
    │   └── CamelCaseNamingStrategy      驼峰命名策略
    ├── properties/
    │   └── ZerxDataProperties           数据层配置属性
    └── util/
        └── NamingUtils                  命名工具类
```

### 依赖关系

```
zerx-spring-data
  ├── spring-boot-starter-data-jdbc
  ├── spring-jdbc (JdbcTemplate)
  ├── zerx-common (PageRequest, PageResult)
  └── zerx-spring-web (可选 — RequestContext 用于审计填充)
```

---

## 3. 架构设计

### 3.1 数据查询流程

```
Controller
    │
    ▼
┌──────────────────────────────────────────┐
│  Service 方法                              │
│  ┌──────────────────────────────────────┐ │
│  │  @DataScope(column="dept_id",        │ │
│  │             type=DEPT_AND_CHILD)     │ │
│  │  public List<Order> listOrders(...)  │ │
│  └──────────────────────────────────────┘ │
│           │                               │
│           ▼                               │
│  ┌──────────────────────────────────────┐ │
│  │  DataScopeInterceptor (AOP)          │ │
│  │  1. 读取 @DataScope 注解              │ │
│  │  2. DataScopeUserProvider            │ │
│  │     .getCurrentUser() → DataScopeUser│ │
│  │  3. DataScopeHandler                 │ │
│  │     .generateCondition() → SQL片段   │ │
│  │  4. DataScopeContext.set(sqlCondition)│ │
│  └──────────────────────────────────────┘ │
│           │                               │
│           ▼                               │
│  ┌──────────────────────────────────────┐ │
│  │  DynamicQuery.from(jdbc, "sys_order") │ │
│  │    .select(...)                      │ │
│  │    .eq("status", "ACTIVE")           │ │
│  │    .applyDataScope()  ← 从 ThreadLocal│ │
│  │                       读取权限条件    │ │
│  │    .list(rowMapper)                  │ │
│  └──────────────────────────────────────┘ │
│           │                               │
│           ▼                               │
│  finally: DataScopeContext.clear()        │
└──────────────────────────────────────────┘
```

### 3.2 归档-before-删除流程

```
业务代码: repository.delete(entity)
    │
    ▼
┌──────────────────────────────────────────┐
│  Spring Data JDBC 事件机制                │
│                                          │
│  BeforeDeleteCallback                     │
│  ┌──────────────────────────────────────┐ │
│  │  ArchiveCallback.onBeforeDelete()     │ │
│  │  1. 检查该实体是否配置了归档           │ │
│  │  2. 查找匹配的 Archiver               │ │
│  │  3. archiver.archive(entity)          │ │
│  │     ├─ INSERT INTO xxx_archive       │ │
│  │     │   SELECT * FROM xxx WHERE id=? │ │
│  │     └─ 成功 → 继续                   │ │
│  │        失败 → 抛 ArchiveException     │ │
│  │               阻止物理删除            │ │
│  └──────────────────────────────────────┘ │
└──────────────┬───────────────────────────┘
               │ 归档成功
               ▼
┌──────────────────────────────────────────┐
│  Spring Data JDBC                         │
│  执行物理 DELETE FROM xxx WHERE id = ?    │
└──────────────────────────────────────────┘
```

### 3.3 归档表生命周期

```
创建归档表（Flyway 迁移）
    │
    ▼
┌─────────────┐     删除实体时     ┌──────────────────┐
│   主 表      │ ─────────────────→ │    归档表          │
│ sys_user    │   BeforeDelete     │ sys_user_archive  │
│ (活跃数据)   │   自动复制         │ +archived_at      │
│             │                    │ +archived_by      │
└─────────────┘                    └────────┬─────────┘
                                            │
                                   ArchivePurgeTask
                                   (定时清理，默认每天凌晨3点)
                                            │
                                            ▼
                                   ┌──────────────────┐
                                   │  保留天数内的归档  │
                                   │  (默认 90 天)     │
                                   └──────────────────┘
```

### 3.4 慢 SQL 检测流程

```
JdbcTemplate.query() / update() / execute()
    │
    ▼
┌──────────────────────────────────────────┐
│  SlowSqlInterceptor (BeanPostProcessor)   │
│                                          │
│  ProxyFactory 代理 JdbcTemplate           │
│  ┌──────────────────────────────────────┐ │
│  │  1. 记录 startTime                   │ │
│  │  2. invocation.proceed()             │ │
│  │  3. 计算 elapsed = now - start       │ │
│  │  4. elapsed >= threshold ?           │ │
│  │     ├─ WARN:  "Slow SQL detected!"   │ │
│  │     │   (SQL + 脱敏参数)             │ │
│  │     └─ DEBUG: "SQL executed in Xms" │ │
│  └──────────────────────────────────────┘ │
│                                          │
│  敏感参数自动脱敏：                        │
│  password=***MASKED***                    │
│  token=***MASKED***                       │
└──────────────────────────────────────────┘
```

---

## 4. 核心组件

### 4.1 BaseEntity — 聚合根基类

所有 Spring Data JDBC 实体必须继承的基类，提供统一的 ID 策略、审计字段和乐观锁支持。

| 字段 | 注解 | 说明 |
|------|------|------|
| `id` | `@Id` | 主键 ID（Long） |
| `createTime` | `@CreatedDate` | 创建时间（自动填充） |
| `updateTime` | `@LastModifiedDate` | 更新时间（自动填充） |
| `createBy` | `@CreatedBy` | 创建人 ID（自动填充） |
| `updateBy` | `@LastModifiedBy` | 更新人 ID（自动填充） |
| `version` | `@Version` | 乐观锁版本号（自动递增） |

**不包含逻辑删除字段**（如 `deleted`、`delFlag`）。删除操作通过归档机制管理，主表不保留已删除数据。

```java
@Table("sys_user")
public class User extends BaseEntity {
    private String username;
    private String email;
    // ...
}
```

### 4.2 DynamicQuery — 链式 SQL 构建器

解决 Spring Data JDBC 在复杂查询场景下的能力不足。底层委托 `JdbcTemplate` 执行，与 Spring Data JDBC 共享同一个 DataSource，保证事务一致性。

#### 支持的 WHERE 方法（18+）

| 方法 | SQL | 说明 |
|------|-----|------|
| `eq(column, value)` | `column = ?` | 等于（null 安全） |
| `ne(column, value)` | `column != ?` | 不等于 |
| `like(column, value)` | `column LIKE '%value%'` | 模糊匹配 |
| `notLike(column, value)` | `column NOT LIKE '%value%'` | 模糊排除 |
| `in(column, values)` | `column IN (?, ?, ?)` | 集合包含 |
| `notIn(column, values)` | `column NOT IN (?, ?, ?)` | 集合排除 |
| `between(column, min, max)` | `column BETWEEN ? AND ?` | 范围查询 |
| `ge(column, value)` | `column >= ?` | 大于等于 |
| `gt(column, value)` | `column > ?` | 大于 |
| `le(column, value)` | `column <= ?` | 小于等于 |
| `lt(column, value)` | `column < ?` | 小于 |
| `isNull(column)` | `column IS NULL` | 为空判断 |
| `isNotNull(column)` | `column IS NOT NULL` | 非空判断 |
| `raw(clause, values)` | 自由拼接 | 原始条件 |

#### 子查询方法

| 方法 | SQL |
|------|-----|
| `inSubQuery(column, subQuery)` | `column IN (SELECT ...)` |
| `notInSubQuery(column, subQuery)` | `column NOT IN (SELECT ...)` |
| `exists(subQuery)` | `EXISTS (SELECT ...)` |
| `notExists(subQuery)` | `NOT EXISTS (SELECT ...)` |

#### 其他特性

| 特性 | 方法 |
|------|------|
| OR 条件组 | `or()` / `orCondition()` / `endOr()` |
| JOIN | `leftJoin()` / `innerJoin()` / `rightJoin()` / `crossJoin()` |
| GROUP BY / HAVING | `groupBy()` / `having()` |
| ORDER BY（多列） | `orderBy(column, asc)` — 可多次调用 |
| DISTINCT | `distinct()` |
| 分页 | `page(PageRequest, RowMapper)` — 自动 COUNT + LIMIT/OFFSET |
| UNION | `DynamicQuery.unionAll(...)` / `DynamicQuery.union(...)` |
| 数据权限注入 | `applyDataScope()` — 从 ThreadLocal 自动追加条件 |

#### count() 智能优化

- 无 GROUP BY：替换 SELECT 为 `COUNT(*)`，剥离 ORDER BY/LIMIT/OFFSET
- 有 GROUP BY：使用子查询包装 `SELECT COUNT(*) FROM (...) _cnt`

### 4.3 @DataScope — AOP 数据权限

通过注解 + AOP 在 Service 方法执行前自动注入数据权限过滤条件，限制用户只能查询到有权限的数据。

#### 权限策略

| 策略 | SQL 条件 | 说明 |
|------|---------|------|
| `SELF` | `column = ?` | 仅本人数据 |
| `DEPT` | `column IN (?,?,?)` | 本部门数据 |
| `DEPT_AND_CHILD` | `column IN (?,?,?,...)` | 本部门及子部门数据 |
| `ALL` | 不追加条件 | 全部数据 |

#### DEPT_AND_CHILD 递归 CTE

```
WITH RECURSIVE dept_tree AS (
    SELECT id FROM sys_dept WHERE id IN (?,?,?)
    UNION ALL
    SELECT d.id FROM sys_dept d
    INNER JOIN dept_tree dt ON d.parent_id = dt.id
)
SELECT id FROM dept_tree
```

#### 组件协作

| 组件 | 职责 |
|------|------|
| `@DataScope` | 注解声明，标注在 Service 方法上 |
| `DataScopeInterceptor` | AOP 环绕通知，读取注解和用户上下文 |
| `DataScopeHandler` | 根据 Type 生成参数化 SQL 条件 |
| `DataScopeContext` | ThreadLocal 存储 SQL 条件片段 |
| `DataScopeUserProvider` | SPI：业务实现，提供当前用户信息 |
| `DataScopeUser` | record：userId + deptIds + roles |

#### SPI 接口

```java
public interface DataScopeUserProvider {
    Optional<DataScopeUser> getCurrentUser();
}
```

业务系统实现此接口并注册为 Spring Bean，框架自动激活 `DataScopeInterceptor`。

### 4.4 归档机制

替代逻辑删除的核心设计。删除前先将数据完整复制到归档表，归档失败则阻止物理删除。

#### Archiver SPI

```java
public interface Archiver<T> {
    void archive(T entity);                        // 归档
    Optional<T> restore(Object id);                 // 恢复
    boolean supports(Class<?> entityClass);         // 类型匹配
}
```

#### JdbcArchiveRepository — 默认实现

| 操作 | SQL | 说明 |
|------|-----|------|
| `archive()` | `INSERT INTO xxx_archive SELECT *, CURRENT_TIMESTAMP AS archived_at FROM xxx WHERE id = ?` | 从主表复制到归档表 |
| `restore()` | `SELECT * FROM xxx_archive WHERE id = ?` + `DELETE FROM xxx_archive WHERE id = ?` | 从归档表查询并删除 |
| `findArchived()` | `SELECT * FROM xxx_archive WHERE id = ?` | 查询归档数据 |
| `listArchived()` | `SELECT * FROM xxx_archive ORDER BY archived_at DESC LIMIT ? OFFSET ?` | 分页查询归档 |
| `purgeExpired()` | `DELETE FROM xxx_archive WHERE archived_at < DATE_SUB(NOW(), INTERVAL ? DAY)` | 清理过期数据 |

**幂等性保证**：归档前检查归档表是否已存在该记录，避免重复归档。

#### ArchiveCallback — Spring Data 事件回调

实现 `BeforeDeleteCallback<BaseEntity>`，在 Repository delete 操作时自动触发归档。如果归档失败抛出 `ArchiveException`，Spring Data JDBC 将中止物理删除。

#### ArchivePurgeTask — 定时清理

- 默认 cron：`0 0 3 * * ?`（每天凌晨 3 点）
- 默认保留天数：90 天
- 需要启动类添加 `@EnableScheduling`

### 4.5 SlowSqlInterceptor — 慢 SQL 检测

通过 `BeanPostProcessor` 代理所有 `JdbcTemplate` 实例，拦截 `query`/`update`/`execute`/`batch` 方法。

| 日志级别 | 触发条件 | 内容 |
|---------|---------|------|
| WARN | 执行时间 >= 阈值（默认 1000ms） | SQL + 脱敏参数 |
| DEBUG | 执行时间 < 阈值 | SQL + 耗时 |
| ERROR | 执行异常 | SQL + 耗时 + 错误信息 |

**敏感参数自动脱敏**：匹配 `password`、`secret`、`token`、`api_key` 等关键词，替换为 `***MASKED***`。

### 4.6 BatchOperations — 批量操作工具

使用 `JdbcTemplate.batchUpdate()` 绕过 Spring Data JDBC 的逐条 INSERT，在大数据量写入场景下性能提升约 **60 倍**。

| 方法 | 说明 |
|------|------|
| `batchInsert(table, columns, rows)` | 批量插入（表名指定） |
| `batchInsert(entityClass, columns, rows)` | 批量插入（实体类自动解析表名） |
| `batchUpdate(table, setColumns, whereColumns, rows)` | 批量更新 |
| `batchDelete(table, whereColumns, rows)` | 批量删除 |
| `batchInsertAndReturnKeys(entityClass, columns, rows)` | 批量插入并返回生成的主键 |

默认批处理大小：**200 条/批**，可通过构造函数自定义。

### 4.7 ZerxAuditorAware — 审计感知

实现 Spring Data 的 `AuditorAware<Long>` 接口，自动从 `RequestContext.getUserId()` 获取当前用户 ID，填充 `@CreatedBy` 和 `@LastModifiedBy` 字段。

**反射桥接**：使用 `Class.forName()` + `Method.invoke()` 访问 `RequestContext`，避免 `zerx-spring-data` 对 `zerx-spring-web` 的编译时依赖。方法引用使用双重检查锁缓存，确保线程安全。

**降级策略**：当 Web 模块不可用时，自动注册无操作的默认 `AuditorAware`（返回 `Optional.empty()`）。

### 4.8 ZerxRepositoryHelper — Repository 增强

独立 Spring Bean，提供分页查询、批量存在性检查、总数统计等通用能力。

| 方法 | 说明 |
|------|------|
| `findPage(entityClass, pageRequest)` | 分页查询（按 ID 倒序），返回 `Map` 列表 |
| `existsByIds(entityClass, ids)` | 批量检查 ID 是否都存在 |
| `countAll(entityClass)` | 统计总记录数 |

### 4.9 命名策略

支持两种命名策略，通过 `zerx.data.naming-strategy` 配置切换：

| 策略 | 表名 | 列名 | 实现类 |
|------|------|------|--------|
| `SNAKE_CASE`（默认） | `sys_user` | `user_name` | `DefaultNamingStrategy` |
| `CAMEL_CASE` | `SysUser` | `userName` | `CamelCaseNamingStrategy` |

---

## 5. 自动配置

### 5.1 激活条件

| 条件 | 注解 |
|------|------|
| JdbcTemplate + JdbcAggregateTemplate 在 classpath | `@ConditionalOnClass` |
| `zerx.data.slow-sql.enabled` 未配置或为 `true` | `@ConditionalOnProperty(matchIfMissing = true)` |

### 5.2 注册的 Bean

| Bean | 条件 | 说明 |
|------|------|------|
| `JdbcMappingContext` | `@ConditionalOnMissingBean` | 自定义命名策略 + Single Query Loading |
| `SlowSqlInterceptor` | `slow-sql.enabled=true` | 慢 SQL 检测（BeanPostProcessor） |
| `ZerxSlowSqlLogger` | `slow-sql.enabled=true` | 慢 SQL 阈值判断辅助 |
| `AuditorAware<Long>` | `@ConditionalOnMissingBean` | 默认：返回 empty |
| `AuditorAware<Long>` (Zerx) | `RequestContext` 在 classpath | 反射访问 RequestContext |
| `ArchiveCallback` | `archive.enabled=true` + Archiver 存在 | BeforeDeleteCallback |
| `ArchiveService` | `archive.enabled=true` + Archiver 存在 | 归档服务门面 |
| `ArchivePurgeTask` | `archive.enabled=true` + DataSource 存在 | 定时清理 |
| `ZerxRepositoryHelper` | DataSource 存在 | Repository 增强 |
| `BatchOperations` | DataSource 存在 | 批量操作工具 |
| `DataScopeHandler` | DataSource 存在 | 数据权限 SQL 生成 |
| `DataScopeInterceptor` | `DataScopeUserProvider` 存在 | AOP 数据权限拦截 |

### 5.3 AutoConfiguration.imports

```
com.zerx.spring.data.autoconfigure.ZerxDataAutoConfiguration
```

---

## 6. 配置属性

### 6.1 ZerxDataProperties (`zerx.data`)

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `naming-strategy` | `NamingStrategy` | `SNAKE_CASE` | 列名命名策略（`SNAKE_CASE` / `CAMEL_CASE`） |
| `single-query-loading` | boolean | `true` | 是否开启 Single Query Loading（解决 N+1） |
| `slow-sql.enabled` | boolean | `true` | 是否启用慢 SQL 检测 |
| `slow-sql.threshold` | Duration | `1000ms` | 慢 SQL 阈值 |
| `slow-sql.log-params` | boolean | `true` | 是否记录 SQL 参数值 |

### 6.2 ArchiveProperties (`zerx.data.archive`)

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | boolean | `false` | 是否启用归档 |
| `table-suffix` | String | `_archive` | 归档表后缀 |
| `entities` | `Set<String>` | `[]` | 需要归档的实体全限定类名 |
| `retain-days` | long | `90` | 归档数据保留天数 |
| `timeout` | Duration | `10s` | 归档操作超时时间 |
| `purge-cron` | String | `0 0 3 * * ?` | 过期归档清理 cron 表达式 |

### 6.3 完整配置示例

```yaml
zerx:
  data:
    naming-strategy: SNAKE_CASE
    single-query-loading: true
    slow-sql:
      enabled: true
      threshold: 1000ms
      log-params: true
    archive:
      enabled: true
      table-suffix: "_archive"
      entities:
        - com.zerx.business.user.entity.User
        - com.zerx.business.order.entity.Order
      retain-days: 90
      purge-cron: "0 0 3 * * ?"
```

---

## 7. 使用示例

### 7.1 定义实体（继承 BaseEntity）

```java
@Table("sys_user")
public class User extends BaseEntity {
    private String username;
    private String email;
    private Integer status;

    // 推荐 @Getter + @Setter，避免 Lombok @Data
    @Getter @Setter
    // ...
}
```

### 7.2 定义 Repository

```java
public interface UserRepository extends ZerxRepository<User, Long> {
    List<User> findByStatus(String status);
}
```

### 7.3 DynamicQuery 链式查询

```java
@Service
public class OrderService {
    @Autowired private JdbcTemplate jdbcTemplate;

    public List<OrderVO> listOrders(String keyword, LocalDateTime start, LocalDateTime end) {
        return DynamicQuery.from(jdbcTemplate, "sys_order o")
            .select("o.id", "o.order_no", "u.username")
            .leftJoin("sys_user u", "u.id = o.user_id")
            .eq("o.status", "PAID")
            .like("o.order_no", keyword)
            .between("o.create_time", start, end)
            .orderBy("o.create_time", false)
            .limit(20)
            .offset(0)
            .list(orderRowMapper);
    }
}
```

### 7.4 DynamicQuery 分页查询

```java
public PageResult<OrderVO> pageOrders(PageRequest pageReq, String status) {
    return DynamicQuery.from(jdbcTemplate, "sys_order")
        .eq("status", status)
        .page(pageReq, orderRowMapper);
}
```

### 7.5 数据权限（@DataScope）

```java
// 1. 实现 DataScopeUserProvider
@Component
public class ZerxDataScopeUserProvider implements DataScopeUserProvider {
    @Override
    public Optional<DataScopeUser> getCurrentUser() {
        Long userId = RequestContext.get().getUserId();
        Long deptId = RequestContext.get().getDeptId();
        if (userId == null) return Optional.empty();
        return Optional.of(new DataScopeUser(userId, List.of(deptId),
                RequestContext.get().getRoles()));
    }
}

// 2. 在 Service 方法上标注 @DataScope
@DataScope(column = "dept_id", type = DataScope.Type.DEPT_AND_CHILD)
public List<Order> listDeptOrders(String keyword) {
    return DynamicQuery.from(jdbcTemplate, "sys_order")
        .eq("status", "ACTIVE")
        .like("order_no", keyword)
        .applyDataScope()   // 自动追加: AND (dept_id IN (?,?,?))
        .list(orderRowMapper);
}
```

### 7.6 归档配置

```java
// 1. 配置需要归档的实体
// application.yml: zerx.data.archive.entities[0]=com.zerx.business.user.entity.User

// 2. 注册 Archiver Bean
@Bean
Archiver<User> userArchiver(JdbcTemplate jdbc, ArchiveProperties props) {
    return new JdbcArchiveRepository<>(jdbc, props, User.class);
}

// 3. 创建归档表（Flyway 迁移）
// CREATE TABLE sys_user_archive LIKE sys_user;
// ALTER TABLE sys_user_archive ADD COLUMN archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

// 4. 正常删除 — 归档自动触发
userRepo.delete(user);  // BeforeDeleteCallback 自动归档

// 5. 查询归档数据
archiveService.findArchived(userId, User.class);

// 6. 恢复归档数据
archiveService.restore(userId, User.class)
    .ifPresent(data -> userRepo.save(convert(data)));
```

### 7.7 批量操作

```java
@Autowired private BatchOperations batchOps;

public void importUsers(List<UserImportDTO> list) {
    List<Object[]> rows = list.stream()
        .map(dto -> new Object[]{dto.getUsername(), dto.getEmail(), 1})
        .toList();

    // 批量插入，性能比 repository.saveAll() 高约 60 倍
    batchOps.batchInsert(User.class,
        List.of("username", "email", "status"), rows);
}
```

---

## 8. 设计决策

| 决策 | 理由 |
|------|------|
| 不使用逻辑删除 | 逻辑删除导致主表数据膨胀、索引膨胀、查询需要额外 `WHERE deleted=0`，性能随数据量增长持续劣化。归档-before-删除模式将历史数据分离到归档表，主表保持精简 |
| 归档-before-删除模式 | 先归档再物理删除，归档失败时抛 `ArchiveException` 阻止删除，保证数据不丢失。归档表与主表物理隔离，互不影响查询性能 |
| 归档表额外字段 | 归档表自动追加 `archived_at`（归档时间）和 `archived_by`（归档人），便于审计和定时清理 |
| DynamicQuery 作为补充 | Spring Data JDBC 的派生查询在复杂场景（多表 JOIN、动态条件、分页）下能力不足，DynamicQuery 提供 JdbcTemplate 级别的灵活性，同时保持参数化查询的安全性 |
| AOP + ThreadLocal 实现数据权限 | AOP 拦截 Service 方法设置 ThreadLocal，DynamicQuery 在同一线程中读取条件，无需修改 DynamicQuery 构造函数签名，保持 API 简洁 |
| 递归 CTE 解析部门树 | `DEPT_AND_CHILD` 策略使用 `WITH RECURSIVE` 一次性查询所有子部门 ID，避免 N 次递归查询或应用层树遍历。如数据库不支持 CTE，降级为直接使用父部门 ID |
| BeanPostProcessor 代理 JdbcTemplate | 无需业务代码修改任何调用方式，透明地拦截所有 SQL 执行。比 AOP 更底层，能拦截到直接通过 JdbcTemplate 实例调用的场景 |
| 反射桥接 RequestContext | `zerx-spring-data` 作为基础模块不应强依赖 `zerx-spring-web`。反射 + `@ConditionalOnClass` 实现可选集成，web 模块存在时自动启用审计填充，不存在时安全降级 |
| BatchOperations 独立于 Spring Data | 绕过 Spring Data JDBC 的逐条 INSERT 机制，直接使用 `JdbcTemplate.batchUpdate()`，避免 Entity 代理和事件回调的开销，适合大批量数据导入场景 |
| Single Query Loading 默认开启 | Spring Data JDBC 默认对 `@MappedCollection` 使用懒加载 + N+1 查询，Single Query Loading 通过 JOIN + 子查询一次性加载所有关联数据 |
| 命名策略可配置 | `SNAKE_CASE` 是数据库标准（默认），`CAMEL_CASE` 适配已有驼峰字段命名的数据库，通过单一配置项切换，无需修改实体类 |
| `@ConditionalOnBean(DataScopeUserProvider.class)` | 数据权限 AOP 拦截器仅在业务提供了用户信息提供者时才激活，非 Web 场景（定时任务、消息消费）自动跳过，避免空指针异常 |
