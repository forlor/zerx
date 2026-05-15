# Zerx 开发规范

## 环境要求

| 工具 | 最低版本 | 推荐版本 |
|------|---------|---------|
| JDK | 21 | 21+ |
| Maven | 3.9.0 | 3.9.6+ |

## 代码风格

项目通过以下工具保证代码风格一致性：

- **EditorConfig** — 统一缩进（4 空格）、字符集（UTF-8）、换行符（LF）
- **Checkstyle** — 编译期强制检查命名规范、注释、代码结构
- **SpotBugs** — 静态分析检测潜在 bug 和安全漏洞
- **Maven Enforcer** — 强制 JDK/Maven 版本、依赖约束

### 命名规范

| 元素 | 规范 | 示例 |
|------|------|------|
| 包名 | 全小写 | `com.zerx.common.util` |
| 类名 | PascalCase | `StringUtil`, `BaseEnum` |
| 方法名 | camelCase | `isEmpty()`, `findByCode()` |
| 常量 | UPPER_SNAKE_CASE | `MAX_PAGE_SIZE` |
| 变量 | camelCase | `userName`, `resultCode` |
| 泛型参数 | 单个大写字母 | `T`, `R`, `K, V` |

### 格式规范

- 缩进：4 个空格（禁止 Tab）
- 行宽：最大 140 字符
- 左花括号 `{` 不换行
- 数组声明：Java 风格 `String[] args`（禁止 C 风格 `String args[]`）
- 每个文件末尾保留一个空行

### 注释规范

- 所有 `public` 类/接口/枚举必须有 Javadoc
- 所有 `public` 方法（超过 3 行）必须有 Javadoc
- 注释使用中文
- Javadoc 中 `@param`、`@return`、`@throws` 缺失不强制（简化维护成本）

### 禁止事项

- 禁止 `System.out.println` / `System.err.println`（使用 SLF4J）
- 禁止 `e.printStackTrace()`（使用 `logger.error("msg", e)`）
- 禁止 `import xxx.*`（逐个导入）
- 禁止导入 `sun.*` 包
- 禁止字符串用 `==` 比较
- 禁止 switch 没有 default

## 项目结构

```
zerx/
├── zerx-core/                  # 核心层（零/轻量依赖）
│   ├── zerx-common/            # 基础工具 + 异常体系
│   ├── zerx-logging/           # 日志封装
│   └── zerx-core-bom/          # 核心层依赖管理
├── zerx-spring/                # Spring 层（依赖 Spring Boot）
└── docs/                       # 项目文档
```

## Git 规范

### 分支命名

| 类型 | 格式 | 示例 |
|------|------|------|
| 主分支 | `main` | `main` |
| 功能分支 | `feat/<模块>-<描述>` | `feat/common-uuid-util` |
| 修复分支 | `fix/<模块>-<描述>` | `fix/common-null-check` |
| 重构分支 | `refactor/<模块>-<描述>` | `refactor/core-exception` |

### Commit Message 格式

遵循 [Conventional Commits](https://www.conventionalcommits.org/)：

```
<type>(<scope>): <description>

[可选正文]

[可选 footer]
```

**type 类型：**

| 类型 | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `refactor` | 重构（不改变行为） |
| `docs` | 文档变更 |
| `style` | 代码格式调整（不影响逻辑） |
| `test` | 测试相关 |
| `chore` | 构建/工具变更 |
| `ci` | CI/CD 配置变更 |
| `perf` | 性能优化 |

**示例：**

```
feat(common): 新增 UuidUtil 支持 UUIDv7 生成
fix(core): 修复 ReflectUtil 对 Record 类型的字段解析
refactor(logging): 重构日志上下文传递机制
```

## 构建

```bash
# 编译
mvn clean compile

# 运行测试
mvn clean test

# 完整构建（包含代码检查）
mvn clean verify

# 跳过代码检查（仅紧急情况使用）
mvn clean verify -Dcheckstyle.skip=true -Dspotbugs.skip=true
```

## IDE 配置

### IntelliJ IDEA

1. 导入项目：`File → Open → 选择 pom.xml → Open as Project`
2. 安装插件：
   - **Checkstyle-IDEA**：指向项目根目录的 `checkstyle.xml`
   - **SpotBugs IDEA**：自动集成
3. 代码格式：`Settings → Editor → Code Style → Java`，项目使用 4 空格缩进
4. EditorConfig 插件通常 IDEA 已内置，确保开启

### VS Code

1. 安装扩展：**Extension Pack for Java**、**Checkstyle for Java**
2. 工作区设置中配置 checkstyle 路径指向 `checkstyle.xml`
