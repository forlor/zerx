# Zerx 项目架构设计文档

> **ZERX FRAMEWORK DESIGN**  
> Java 多模块 Maven 脚手架项目  
> 版本：v1.0.0 | 日期：2026-05-12 | 作者：Forlor  
> GitHub: github.com/forlor/zerx  
> JDK 21 | Maven | Spring Boot 3

---

## 目录

- [1 项目概述](#1-项目概述)
  - [1.1 项目背景](#11-项目背景)
  - [1.2 设计目标](#12-设计目标)
  - [1.3 技术栈概览](#13-技术栈概览)
- [2 项目架构设计](#2-项目架构设计)
  - [2.1 整体架构](#21-整体架构)
  - [2.2 模块依赖关系](#22-模块依赖关系)
- [3 基础脚手架 (zerx-core)](#3-基础脚手架-zerx-core)
  - [3.1 zerx-common（通用工具类）](#31-zerx-common通用工具类)
  - [3.2 zerx-exception（统一异常处理）](#32-zerx-exception统一异常处理)
  - [3.3 zerx-logging（日志抽象）](#33-zerx-logging日志抽象)
  - [3.4 zerx-crypto（加解密工具）](#34-zerx-crypto加解密工具)
  - [3.5 zerx-http（轻量 HTTP 客户端）](#35-zerx-http轻量-http-客户端)
  - [3.6 zerx-core-bom（基础模块 BOM）](#36-zerx-core-bom基础模块-bom)
- [4 Spring Boot 脚手架组件 (zerx-spring)](#4-spring-boot-脚手架组件-zerx-spring)
  - [4.1 zerx-spring-boot-starter（自动配置基底）](#41-zerx-spring-boot-starter自动配置基底)
  - [4.2 zerx-spring-web（Web 层增强）](#42-zerx-spring-webweb-层增强)
  - [4.3 zerx-spring-data（数据访问增强）](#43-zerx-spring-data数据访问增强)
  - [4.4 zerx-spring-security（安全框架集成）](#44-zerx-spring-security安全框架集成)
  - [4.5 zerx-spring-cache（缓存支持）](#45-zerx-spring-cache缓存支持)
  - [4.6 zerx-spring-mq（消息队列支持）](#46-zerx-spring-mq消息队列支持)
  - [4.7 zerx-spring-doc（API 文档集成）](#47-zerx-spring-docapi-文档集成)
  - [4.8 zerx-spring-logging（请求日志）](#48-zerx-spring-logging请求日志)
  - [4.9 zerx-spring-monitor（监控与健康检查）](#49-zerx-spring-monitor监控与健康检查)
  - [4.10 zerx-spring-bom（Spring 模块 BOM）](#410-zerx-spring-bomspring-模块-bom)
- [5 Maven 配置规范](#5-maven-配置规范)
  - [5.1 父 POM 设计](#51-父-pom-设计)
  - [5.2 版本号规范](#52-版本号规范)
- [6 模块详细信息](#6-模块详细信息)

---

## 1 项目概述

### 1.1 项目背景

Zerx 是一个面向企业级 Java 应用开发的多模块 Maven 脚手架项目。其核心理念是"分层解耦、按需取用"，旨在为开发团队提供一套高效、灵活、可维护的基础设施。在当前快速迭代的开发环境中，团队往往需要在多个项目之间复用通用能力，同时又希望避免引入过多不必要的依赖。Zerx 的设计正是因此而生，它将功能拆分为两大部分：依赖极少的基础脚手架和基于 Spring Boot 3 的增强组件。开发者可以根据项目实际需求，灵活地组合所需模块，避免了"一刀切"式第三方框架带来的包袱和约束。

Zerx 项目采用 JDK 21 作为基础运行环境，充分利用 Java 最新特性，包括 Record 类型、Pattern Matching、Virtual Threads、Sealed Classes 等，确保框架在性能和开发体验上始终保持领先。项目以 Maven 作为构建工具，采用多模块结构，通过明确的模块边界和依赖关系，实现高度模块化的工程实践。

### 1.2 设计目标

Zerx 项目的设计遵循以下核心目标：

- **极简依赖**：基础脚手架部分仅依赖 JDK 标准库和少量经过严格筛选的第三方库，确保核心能力的轻量化和稳定性
- **分层架构**：将基础能力与框架集成能力分离，使得基础模块可以独立于 Spring Boot 使用
- **高度可扩展**：每个模块都提供清晰的扩展点，支持开发者按需定制
- **开箱即用**：提供合理的默认配置，降低初始学习成本

### 1.3 技术栈概览

| 技术组件 | 版本 | 用途说明 |
|----------|------|----------|
| JDK | 21 (LTS) | 运行环境，支持 Record、Virtual Threads、Pattern Matching 等新特性 |
| Maven | 3.9+ | 项目构建与依赖管理 |
| Spring Boot | 3.3+ | 脚手架组件基础框架 |
| JUnit 5 | 5.10+ | 单元测试框架 |
| SLF4J | 2.0+ | 日志门面抽象 |
| Mockito | 5.x | 单元测试 Mock 框架 |

---

## 2 项目架构设计

### 2.1 整体架构

Zerx 项目采用多模块 Maven 结构，整体架构分为两大部分：**zerx-core**（基础脚手架）和 **zerx-spring**（Spring Boot 3 脚手架组件）。根项目 `zerx-parent` 作为最顶层的父 POM，统一管理所有子模块的依赖版本、插件配置和构建参数。这种分层设计使得基础脚手架完全独立于 Spring 生态，可以在任何 Java 项目中单独使用，而 Spring Boot 组件则在其基础上提供框架集成能力。

项目的目录结构设计如下：

```
zerx/
├── pom.xml                          ← 父 POM (zerx-parent)
├── zerx-core/                       ← 基础脚手架 (极少依赖)
│   ├── zerx-common/                ← 通用工具类
│   ├── zerx-exception/             ← 统一异常处理
│   ├── zerx-logging/               ← 日志抽象
│   ├── zerx-crypto/                ← 加解密工具
│   ├── zerx-http/                  ← 轻量 HTTP 客户端
│   └── zerx-core-bom/              ← 基础模块 BOM
├── zerx-spring/                      ← Spring Boot 3 脚手架
│   ├── zerx-spring-boot-starter/   ← 自动配置基底
│   ├── zerx-spring-web/            ← Web 层增强
│   ├── zerx-spring-data/           ← 数据访问增强
│   ├── zerx-spring-security/       ← 安全框架集成
│   ├── zerx-spring-cache/          ← 缓存支持
│   ├── zerx-spring-mq/             ← 消息队列支持
│   ├── zerx-spring-doc/            ← API 文档集成
│   ├── zerx-spring-logging/        ← 请求日志
│   ├── zerx-spring-monitor/        ← 监控与健康检查
│   └── zerx-spring-bom/            ← Spring 模块 BOM
└── docs/                            ← 设计文档
```

### 2.2 模块依赖关系

各模块之间的依赖关系遵循单向依赖原则。`zerx-common` 作为最底层的基础模块，不依赖任何其他 Zerx 模块，仅依赖 JDK 标准库。其他基础模块（如 `zerx-exception`、`zerx-logging` 等）均可选择性依赖 `zerx-common`。Spring Boot 组件层的模块依赖 `zerx-core` 中的基础能力，并在此基础上提供 Spring Boot 自动配置和集成能力。每个 Spring Boot Starter 都是独立的 JAR 包，开发者只需引入所需的 Starter 依赖，即可获得对应功能，无需引入整个框架。

```
┌─────────────────────────────────────────────────┐
│                  zerx-parent                     │
│  ┌───────────────┐    ┌──────────────────────┐  │
│  │  zerx-core    │    │    zerx-spring        │  │
│  │  ┌─────────┐  │    │  ┌────────────────┐  │  │
│  │  │ common  │◄─┼────┼──│spring-boot-    │  │  │
│  │  └────┬────┘  │    │  │   starter       │  │  │
│  │       │       │    │  └───────┬────────┘  │  │
│  │  ┌────┴────┐  │    │  ┌───────┴────────┐  │  │
│  │  │exception│  │    │  │spring-web       │  │  │
│  │  ├─────────┤  │    │  ├────────────────┤  │  │
│  │  │logging  │  │    │  │spring-data      │  │  │
│  │  ├─────────┤  │    │  ├────────────────┤  │  │
│  │  │crypto   │  │    │  │spring-security  │  │  │
│  │  ├─────────┤  │    │  ├────────────────┤  │  │
│  │  │http     │  │    │  │spring-cache     │  │  │
│  │  └─────────┘  │    │  ├────────────────┤  │  │
│  │  ┌─────────┐  │    │  │spring-mq        │  │  │
│  │  │core-bom │  │    │  ├────────────────┤  │  │
│  │  └─────────┘  │    │  │spring-doc       │  │  │
│  └───────────────┘    │  ├────────────────┤  │  │
│                       │  │spring-logging   │  │  │
│                       │  ├────────────────┤  │  │
│                       │  │spring-monitor   │  │  │
│                       │  ├────────────────┤  │  │
│                       │  │spring-bom       │  │  │
│                       │  └────────────────┘  │  │
│                       └──────────────────────┘  │
└─────────────────────────────────────────────────┘
```

---

## 3 基础脚手架 (zerx-core)

基础脚手架是 Zerx 的核心底座，设计原则是"零框架依赖"，仅依赖 JDK 标准库和极少的第三方库，确保在没有框架的环境下也能运行。这部分模块提供的能力是所有 Java 项目都可能需要的通用能力，包括工具类、异常处理、日志、加解密、HTTP 客户端等。

### 3.1 zerx-common（通用工具类）

`zerx-common` 是所有基础模块的底层依赖，提供广泛使用的工具方法和基础类型定义。该模块不依赖任何第三方库，仅使用 JDK 标准库。主要包含以下功能组件：

#### 3.1.1 字符串工具 (StringUtil)

提供字符串常用操作的工具类，包括判空、裁剪、驼峰转换、序列化/反序列化、正则匹配、编码转换等方法。所有方法均为静态方法，线程安全，方便在任何场景下直接调用。

#### 3.1.2 集合工具 (CollectionUtil)

提供集合操作的工具类，包括集合判空、分组、去重、过滤、折叠、排序等方法。充分利用 JDK 21 Stream API 的特性，提供更为简洁的链式调用风格。

#### 3.1.3 日期时间工具 (DateUtil)

基于 `java.time` 包的日期时间工具类，提供日期格式化、解析、计算差值、时区转换等功能。对常用日期格式定义标准常量，统一项目中的日期处理风格。

#### 3.1.4 反射工具 (ReflectUtil)

提供反射操作的工具类，包括字段访问、方法调用、注解解析等功能。利用 JDK 21 的反射增强特性，提供类型安全的访问方式，减少反射调用的样板代码。

#### 3.1.5 基础类型定义

提供统一的基础类型定义，包括通用响应结构体 `Result<T>`、分页请求参数 `PageRequest`、分页响应结果 `PageResult<T>`、接口响应码枚举 `ResponseCode` 等。这些类型均使用 JDK 21 的 Record 特性定义，确保不可变性和线程安全。

```java
// 示例：统一响应结构体
public record Result<T>(boolean success, String code, String message, T data) {
    public static <T> Result<T> ok(T data) {
        return new Result<>(true, "200", "success", data);
    }
    public static <T> Result<T> fail(String code, String message) {
        return new Result<>(false, code, message, null);
    }
}

// 示例：分页请求参数
public record PageRequest(int page, int size, List<OrderItem> orders) {
    public PageRequest {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 200) size = 200;
    }
}
```

### 3.2 zerx-exception（统一异常处理）

`zerx-exception` 提供一套完整的异常处理体系，仅依赖 `zerx-common`。该模块定义了统一的异常层级结构、业务异常基类以及异常工具方法。设计理念是通过异常码体系将不同类型的错误统一分类，方便全局异常处理和错误追踪。主要包含以下内容：

#### 3.2.1 异常基类体系

定义顶层抽象异常基类 `ZerxException`，以及业务异常 `BusinessException`、参数校验异常 `ValidationException`、权限异常 `AuthorizationException`、资源未找到异常 `NotFoundException` 等子类。每个异常均携带错误码和详细信息，支持链式异常原因设置。

```java
// 异常基类
public abstract class ZerxException extends RuntimeException {
    private final ErrorCode errorCode;
    
    protected ZerxException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }
    
    protected ZerxException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.message(), cause);
        this.errorCode = errorCode;
    }
    
    public ErrorCode getErrorCode() { return errorCode; }
    public String getCode() { return errorCode.code(); }
    public int getHttpStatus() { return errorCode.httpStatus(); }
}

// 业务异常
public class BusinessException extends ZerxException {
    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
```

#### 3.2.2 异常码枚举

定义统一的异常码枚举 `ErrorCode`，采用分段编码规则：

| 段位 | 范围 | 说明 |
|------|------|------|
| 1xxxx | 10000-19999 | 系统级异常（网络、IO、运行时等） |
| 2xxxx | 20000-29999 | 业务逻辑异常（业务规则校验失败等） |
| 3xxxx | 30000-39999 | 参数校验异常（格式错误、必填缺失等） |
| 4xxxx | 40000-49999 | 权限相关异常（未认证、无权限等） |
| 5xxxx | 50000-59999 | 外部服务异常（第三方接口调用失败等） |

每个异常码包含数值编码、描述信息和 HTTP 状态码映射。

```java
public record ErrorCode(String code, String message, int httpStatus) {
    // 系统级异常
    public static final ErrorCode SYSTEM_ERROR = 
        new ErrorCode("10001", "系统内部错误", 500);
    public static final ErrorCode NETWORK_ERROR = 
        new ErrorCode("10002", "网络连接异常", 503);
    
    // 业务异常
    public static final ErrorCode BALANCE_NOT_ENOUGH = 
        new ErrorCode("20001", "余额不足", 400);
    public static final ErrorCode ORDER_NOT_FOUND = 
        new ErrorCode("20002", "订单不存在", 404);
    
    // 参数校验异常
    public static final ErrorCode PARAM_REQUIRED = 
        new ErrorCode("30001", "必填参数不能为空", 400);
    public static final ErrorCode PARAM_FORMAT_ERROR = 
        new ErrorCode("30002", "参数格式错误", 400);
    
    // 权限异常
    public static final ErrorCode UNAUTHORIZED = 
        new ErrorCode("40001", "未登录或认证已过期", 401);
    public static final ErrorCode FORBIDDEN = 
        new ErrorCode("40002", "无访问权限", 403);
}
```

### 3.3 zerx-logging（日志抽象）

`zerx-logging` 提供统一的日志抽象层，基于 SLF4J 接口设计，仅依赖 `zerx-common` 和 SLF4J API。该模块的设计目的是提供一套标准化的日志工具，使得项目中的日志输出风格统一、可控。主要包含日志工具类 `LoggerUtil`，提供结构化日志记录、日志上下文传递、日志格式化等功能，支持在多线程环境下自动注入请求 ID、用户信息等上下文。

### 3.4 zerx-crypto（加解密工具）

`zerx-crypto` 提供常用的加解密工具，仅依赖 `zerx-common`。该模块封装了常见的加密算法，包括对称加密（AES）、哈希算法（MD5、SHA-256、SHA-512）、Base64 编解码、HMAC 签名验证等。所有实现均使用 JDK 内置的 JCA（Java Cryptography Architecture），无需额外依赖。提供统一的 `CryptoUtil` 工具类接口，支持链式调用，便于在不同场景下灵活使用。

```java
public final class CryptoUtil {
    // AES 加解密
    public static String encryptAES(String plainText, String key);
    public static String decryptAES(String cipherText, String key);
    
    // 哈希算法
    public static String md5(String input);
    public static String sha256(String input);
    public static String sha512(String input);
    
    // Base64
    public static String encodeBase64(String input);
    public static String decodeBase64(String encoded);
    
    // HMAC
    public static String hmacSha256(String data, String secret);
}
```

### 3.5 zerx-http（轻量 HTTP 客户端）

`zerx-http` 提供一个轻量级的 HTTP 客户端封装，基于 JDK 21 内置的 `HttpClient` 实现，仅依赖 `zerx-common`。该模块提供统一的 HTTP 请求接口，支持 GET、POST、PUT、DELETE 等常见方法，支持 JSON 请求体序列化和响应体反序列化，支持请求拦截器、超时设置、自定义请求头等功能。利用 JDK 21 `HttpClient` 的异步非阻塞特性，提供高性能的 HTTP 请求能力。

```java
public final class HttpClientUtil {
    public static HttpResponse get(String url);
    public static HttpResponse get(String url, Map<String, String> headers);
    public static <T> T get(String url, Class<T> responseType);
    
    public static HttpResponse post(String url, Object body);
    public static <T> T post(String url, Object body, Class<T> responseType);
    
    public static HttpResponse put(String url, Object body);
    public static HttpResponse delete(String url);
}
```

### 3.6 zerx-core-bom（基础模块 BOM）

`zerx-core-bom` 是基础脚手架模块的 Bill of Materials，采用 Maven BOM 机制统一管理所有基础模块的版本号。使用方只需在 `pom.xml` 中引入该 BOM，即可免去每个模块单独指定版本号的麻烦，确保模块间版本一致性。

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.zerx</groupId>
            <artifactId>zerx-core-bom</artifactId>
            <version>${zerx.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## 4 Spring Boot 脚手架组件 (zerx-spring)

Spring Boot 脚手架组件是 Zerx 的增强层，基于 Spring Boot 3.3+ 和 Spring Framework 6.x 开发。这部分模块依赖 `zerx-core` 中的基础能力，并在此基础上提供自动配置、框架集成和约定于编程能力。每个 Starter 都遵循 Spring Boot 的自动配置规范，通过 `spring.factories` 或 `AutoConfiguration.imports` 文件实现自动装配。

### 4.1 zerx-spring-boot-starter（自动配置基底）

`zerx-spring-boot-starter` 是所有 Spring Boot 组件的基底模块，提供通用的自动配置基础设施。包括通用配置属性绑定机制、条件装配注解处理器、环境感知配置等基础能力。该模块定义了统一的配置前缀 **"zerx."**，所有组件的配置项均在该前缀下进行管理，避免与其他组件的配置冲突。同时提供核心配置属性类 `ZerxProperties`，支持开发者通过 `application.yml` 进行全局配置。

```yaml
# application.yml 示例
zerx:
  web:
    api-prefix: /api
    cors:
      allowed-origins: "*"
  logging:
    enabled: true
    slow-threshold: 3000ms
  doc:
    enabled: true
    title: Zerx API
```

### 4.2 zerx-spring-web（Web 层增强）

`zerx-spring-web` 提供对 Spring MVC 的增强能力。主要包含：

- **全局异常处理器**：自动将业务异常转换为统一的响应格式
- **参数校验增强**：基于 JSR-380 注解提供更友好的校验错误提示
- **统一的响应体封装** `ResponseResult<T>`：确保所有 API 返回格式一致
- **请求日志拦截器**：自动记录请求和响应的关键信息
- **CORS 跨域配置自动装配**

```java
// 全局异常处理器示例
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseResult<?> handleBusinessException(BusinessException ex) {
        return ResponseResult.fail(ex.getCode(), ex.getMessage());
    }
}
```

### 4.3 zerx-spring-data（数据访问增强）

`zerx-spring-data` 提供数据访问层的增强能力，支持 MyBatis、MyBatis-Plus 和 Spring Data JPA 多种 ORM 框架的集成。主要提供：

- **通用分页查询封装**：统一不同 ORM 框架的分页接口
- **多数据源配置支持**：自动装配动态数据源切换
- **数据源监控组件**：提供连接池状态监控和慢 SQL 检测
- **自动填充字段、逻辑删除**等通用功能的抽象封装

### 4.4 zerx-spring-security（安全框架集成）

`zerx-spring-security` 提供基于 Spring Security 6.x 的安全框架集成。主要提供：

- **统一的认证接口** `AuthenticationProvider` 抽象：支持多种认证方式（用户名密码、JWT Token、OAuth2 等）的灵活切换
- **统一的授权模型**：支持基于角色（RBAC）和基于权限（ABAC）的访问控制
- **安全上下文传播**：自动将用户信息、角色、权限等信息绑定到当前线程
- **常见安全攻击的防护**：包括 CSRF、XSS、SQL 注入等

### 4.5 zerx-spring-cache（缓存支持）

`zerx-spring-cache` 提供统一的缓存抽象层，基于 Spring Cache 规范实现。支持多种缓存实现，包括本地缓存（Caffeine）和分布式缓存（Redis）。提供缓存注解的增强定义，支持自定义缓存过期策略、缓存键生成策略、缓存冲击空缺等功能。通过自动配置，开发者只需配置缓存类型和连接参数，即可快速集成缓存能力。

### 4.6 zerx-spring-mq（消息队列支持）

`zerx-spring-mq` 提供消息队列的抽象层和集成能力。支持多种消息中间件，包括 RabbitMQ、Kafka 和 Redis Stream。提供统一的消息发送接口，封装复杂的消息发送逻辑；消息监听器注解，简化消息消费的定义方式；消息可靠性保障，包括消息确认机制、重试策略、死信队列等功能。该模块采用策略模式设计，支持在不同的消息中间件之间平滑切换。

### 4.7 zerx-spring-doc（API 文档集成）

`zerx-spring-doc` 提供基于 SpringDoc OpenAPI 的 API 文档集成。自动生成 API 文档和交互式接口调试页面，支持 OpenAPI 3.0 规范。提供统一的 API 分组配置，自动读取控制器层级的注解信息生成文档；支持自定义响应示例、全局认证配置等功能。开发者无需额外配置，引入 Starter 后即可访问 Swagger UI 页面。

### 4.8 zerx-spring-logging（请求日志）

`zerx-spring-logging` 提供 Web 请求的全链路日志记录能力。通过 Filter 和 Interceptor 实现请求入口和出口的日志记录，包括请求 URL、请求方法、请求头、响应状态码、响应时间等关键信息。支持敏感参数脱敏，防止密码等敏感信息被记录到日志中；支持自定义日志格式输出（JSON、文本等）；支持慢请求警告，自动对响应时间超过阈值的请求进行标记。

### 4.9 zerx-spring-monitor（监控与健康检查）

`zerx-spring-monitor` 提供基于 Spring Boot Actuator 的监控增强能力。自定义健康检查指标，支持添加数据库连接、Redis 连接、消息队列等外部依赖的健康状态检查；应用指标采集，自动采集 JVM 内存、GC、线程池等关键指标；支持多种监控端点的集成，包括 Prometheus、Grafana、ELK 等主流监控方案。

### 4.10 zerx-spring-bom（Spring 模块 BOM）

`zerx-spring-bom` 是 Spring Boot 组件模块的 Bill of Materials，统一管理所有 Spring Boot 组件的版本号。使用方只需在 `pom.xml` 中引入该 BOM，即可获得经过充分测试的版本组合，避免版本冲突问题。该 BOM 同时会引入 `zerx-core-bom`，确保基础模块版本的一致性。

---

## 5 Maven 配置规范

### 5.1 父 POM 设计

根项目 `zerx-parent` 作为最顶层的父 POM，采用 `pom` 打包方式，统一管理以下内容：

- **JDK 版本设置**：通过 `maven.compiler.source` 和 `maven.compiler.target` 属性配置为 21
- **依赖版本统一管理**：通过 `dependencyManagement` 指定所有第三方依赖的版本号
- **插件统一配置**：包括 `maven-compiler-plugin`、`maven-source-plugin`、`maven-javadoc-plugin`、`maven-surefire-plugin` 等常用插件
- **代码质量配置**：集成 Checkstyle、SpotBugs 等静态代码检查工具

```xml
<!-- 父 POM 核心配置示例 -->
<groupId>com.zerx</groupId>
<artifactId>zerx-parent</artifactId>
<version>${zerx.version}</version>
<packaging>pom</packaging>

<properties>
    <zerx.version>1.0.0</zerx.version>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>

<modules>
    <module>zerx-core</module>
    <module>zerx-spring</module>
</modules>
```

### 5.2 版本号规范

Zerx 项目采用语义化版本号规范，主版本号采用 `v` 前缀，如 `v1.0.0`、`v1.1.0`、`v2.0.0` 等。版本号管理遵循语义化版本规范：

| 版本段 | 含义 | 示例 | 说明 |
|--------|------|------|------|
| 主版本号 (MAJOR) | 不兼容的 API 变更 | v1.0.0 → v2.0.0 | 接口或行为有破坏性变更 |
| 次版本号 (MINOR) | 向后兼容的功能新增 | v1.0.0 → v1.1.0 | 新增功能，不影响已有功能 |
| 修订号 (PATCH) | 向后兼容的 Bug 修复 | v1.0.0 → v1.0.1 | 修复 Bug，无功能变更 |

同时在父 POM 中定义 `${zerx.version}` 属性，统一管理所有子模块的版本号。

---

## 6 模块详细信息

下表完整列出了 Zerx 项目所有模块的详细信息，包括模块名称、所属分组、主要依赖和核心能力描述。

| 模块名称 | 分组 | 主要依赖 | 核心能力 |
|----------|------|----------|----------|
| zerx-common | core | 无（仅 JDK） | 字符串、集合、日期、反射等工具类 |
| zerx-exception | core | zerx-common | 统一异常体系、错误码枚举 |
| zerx-logging | core | zerx-common, SLF4J | 日志抽象、结构化日志 |
| zerx-crypto | core | zerx-common | AES、MD5、SHA、Base64、HMAC |
| zerx-http | core | zerx-common | 轻量 HTTP 客户端封装 |
| zerx-core-bom | core | 无 | 基础模块版本统一管理 |
| zerx-spring-boot-starter | spring | zerx-core 模块 | 自动配置基底、配置属性绑定 |
| zerx-spring-web | spring | zerx-spring-boot-starter | 全局异常处理、参数校验、响应封装 |
| zerx-spring-data | spring | zerx-spring-boot-starter | 分页、多数据源、自动填充 |
| zerx-spring-security | spring | zerx-spring-boot-starter | 认证授权、RBAC、JWT |
| zerx-spring-cache | spring | zerx-spring-boot-starter | Caffeine、Redis 缓存抽象 |
| zerx-spring-mq | spring | zerx-spring-boot-starter | RabbitMQ、Kafka、Redis Stream |
| zerx-spring-doc | spring | zerx-spring-boot-starter | OpenAPI 3.0 文档自动生成 |
| zerx-spring-logging | spring | zerx-spring-boot-starter | 请求响应日志、慢请求警告 |
| zerx-spring-monitor | spring | zerx-spring-boot-starter | 健康检查、指标采集、监控集成 |
| zerx-spring-bom | spring | 无 | Spring 模块版本统一管理 |
