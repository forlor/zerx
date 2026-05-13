<p align="center">
  <h1 align="center">Zerx</h1>
  <p align="center">
    <strong>面向企业级 Java 应用开发的多模块 Maven 脚手架</strong>
  </p>
  <p align="center">
    <a href="#特性">特性</a> · <a href="#项目结构">项目结构</a> · <a href="#快速开始">快速开始</a> · <a href="#模块说明">模块说明</a> · <a href="#开发规范">开发规范</a>
  </p>
  <p align="center">
    <img src="https://img.shields.io/badge/JDK-21+-green" alt="JDK 21+">
    <img src="https://img.shields.io/badge/Spring_Boot-3.3+-brightgreen" alt="Spring Boot 3.3+">
    <img src="https://img.shields.io/badge/Maven-3.9+-orange" alt="Maven 3.9+">
    <img src="https://img.shields.io/badge/License-Apache_2.0-blue" alt="License">
  </p>
</p>

---

## 特性

**Zerx** 是一个轻量级、多模块的 Java 基础开发脚手架，采用分层架构设计，帮助团队快速构建企业级应用。

### 核心理念

- **Core / Spring 分层** — 核心层（zerx-core）保持零/轻量外部依赖，可脱离 Spring 独立使用；Spring 层（zerx-spring）通过自动配置提供胶水集成
- **零污染核心** — zerx-common 纯 JDK 实现，不引入任何第三方依赖，可在任何 Java 环境中使用
- **架构守护** — 基于 ArchUnit 的模块边界测试，在 CI 中自动拦截违规依赖
- **开箱即用的规范** — 集成 Checkstyle、SpotBugs、Maven Enforcer，从源头保证代码质量和风格一致

### 技术栈

| 类别 | 技术选型 |
|------|---------|
| 语言 | Java 21（LTS，支持 Virtual Threads、Record Pattern、Sequenced Collections） |
| 构建 | Maven 3.9+，多模块聚合 |
| 框架 | Spring Boot 3.3+（zerx-spring 层） |
| 日志 | SLF4J 2.x |
| 测试 | JUnit 5 + ArchUnit |
| 质量 | Checkstyle + SpotBugs + Maven Enforcer |

## 项目结构

```
zerx/
├── zerx-core/                          # 核心层（零/轻量依赖）
│   ├── zerx-common/                    #   通用工具 + 异常体系 + 基础类型
│   │   └── src/main/java/com/zerx/common/
│   │       ├── constants/              #     全局常量
│   │       ├── enums/                  #     基础枚举（BaseEnum）
│   │       ├── exception/              #     统一异常体系（ZerxException + 5 子类 + ErrorCode 枚举）
│   │       ├── functional/             #     Throwable 函数式接口（4 个）
│   │       ├── model/                  #     通用模型（Result、PageRequest、Pair、Triple 等）
│   │       └── util/                   #     工具类（StringUtil、CollectionUtil、DateUtil 等 12 个）
│   ├── zerx-architecture-test/         #   ArchUnit 架构规则测试（12 条规则，打包为 test-jar）
│   ├── zerx-logging/                   #   日志封装（规划中）
│   └── zerx-core-bom/                  #   核心层 BOM（规划中）
├── zerx-spring/                        # Spring 层（规划中）
├── docs/                               # 项目文档
├── .editorconfig                       # 跨编辑器格式统一
├── .gitattributes                      # Git 行为统一
├── checkstyle.xml                      # Checkstyle 规则（30+ 条）
├── checkstyle-suppressions.xml         # Checkstyle 豁免规则
├── spotbugs-exclude.xml                # SpotBugs 排除规则
├── CONTRIBUTING.md                     # 开发规范文档
└── pom.xml                             # 父 POM（版本管理 + 插件配置）
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

在你的项目中引入 zerx-common：

```xml
<dependency>
    <groupId>com.zerx</groupId>
    <artifactId>zerx-common</artifactId>
    <version>1.0.0</version>
</dependency>
```

> 注意：zerx-common 零第三方依赖，引入后不会给你的项目带来任何额外的 jar 包冲突。

## 模块说明

### zerx-common

核心工具模块，纯 JDK 实现，提供企业级 Java 项目中最常用的基础能力。

**工具类（util）**

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
