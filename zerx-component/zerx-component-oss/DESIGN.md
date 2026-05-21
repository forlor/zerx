# Zerx Component OSS Module Design

## 1. 模块概述

Zerx Component OSS 是面向企业级应用的对象存储组件，提供统一的存储抽象接口，支持通过配置在 MinIO、阿里云 OSS、腾讯云 COS、通用 S3 协议之间平滑切换。核心特性包括：预上传暂存机制（客户端直传 + 业务确认）、公开/私有桶访问控制、基于策略模式的厂商适配、桶级别和目录级别暂存配置。基于 Spring Boot AutoConfiguration 实现零侵入集成，业务代码仅依赖 `OssStorageService` 接口。

### 设计原则

| 原则 | 实现方式 |
|------|----------|
| 统一抽象 | `OssStorageService` 接口遵循 Put/Get/Head/Delete 语义，对齐 S3 标准 |
| 策略模式 | 通过 `zerx.oss.type` 配置动态切换存储后端，无需改代码 |
| 预上传暂存 | 前端直传 OSS → 业务确认迁移，减轻服务端带宽 |
| 无数据库依赖 | 暂存元数据存入对象自定义 Header（`x-amz-meta-zerx-*`），stageToken 使用 UUID |
| 按需取用 | 所有厂商 SDK 均 `optional`，按需引入 |
| SDK 隔离 | 每个实现类仅依赖对应厂商 SDK，不互相引用 |

---

## 2. 模块结构

```
zerx-component-oss/
├── pom.xml
└── src/
    ├── main/java/com/zerx/component/oss/
    │   ├── OssStorageService.java             统一存储接口（15 个方法）
    │   ├── OssResult.java                    上传/复制结果（record）
    │   ├── OssObjectMeta.java                对象元数据（HEAD 结果）
    │   ├── OssObject.java                    对象读取结果（Closeable）
    │   ├── PresignedUrl.java                 预签名 URL（record）
    │   ├── OssStageResult.java               预上传暂存结果（record）
    │   ├── OssConfirmResult.java             确认预上传结果（record）
    │   ├── OssUploadRequest.java             上传请求（record）
    │   ├── OssStageRequest.java              预上传暂存请求（record）
    │   ├── OssPresignPutRequest.java         预签名直接上传请求（record）
    │   ├── OssException.java                 OSS 异常（extends BusinessException）
    │   ├── annotation/
    │   │   ├── OssUploadLimit.java           上传限制注解
    │   │   └── package-info.java
    │   ├── aspect/
    │   │   ├── OssUploadLimitAspect.java     上传限制切面
    │   │   └── package-info.java
    │   ├── autoconfigure/
    │   │   ├── ZerxOssAutoConfiguration.java 自动配置（策略选择）
    │   │   └── package-info.java
    │   ├── impl/
    │   │   ├── AbstractOssStorageService.java 公共基类（路径生成/暂存逻辑）
    │   │   ├── MinioOssStorageService.java   MinIO 实现
    │   │   ├── AliyunOssStorageService.java  阿里云 OSS 实现
    │   │   ├── TencentCosStorageService.java 腾讯 COS 实现
    │   │   ├── S3OssStorageService.java      通用 S3 实现
    │   │   └── package-info.java
    │   └── properties/
    │       ├── ZerxOssProperties.java        配置属性（zerx.oss.*）
    │       └── package-info.java
    ├── main/resources/META-INF/spring/
    │   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
    └── test/java/com/zerx/component/oss/
        ├── OssResultTest.java
        ├── OssObjectMetaTest.java
        ├── OssObjectTest.java
        ├── PresignedUrlTest.java
        ├── OssStageResultTest.java
        ├── OssConfirmResultTest.java
        ├── OssUploadRequestTest.java
        ├── OssStageRequestTest.java
        ├── OssPresignPutRequestTest.java
        ├── OssExceptionTest.java
        ├── ZerxOssAutoConfigurationTest.java
        ├── annotation/OssUploadLimitTest.java
        ├── aspect/OssUploadLimitAspectTest.java
        ├── impl/MinioOssStorageServiceTest.java
        ├── impl/AliyunOssStorageServiceTest.java
        ├── impl/TencentCosStorageServiceTest.java
        ├── impl/S3OssStorageServiceTest.java
        └── properties/ZerxOssPropertiesTest.java
```

### 依赖关系

```
zerx-component-oss
  ├── zerx-common                       (OssException extends BusinessException)
  ├── spring-boot-starter-web (optional) (MultipartFile, @RestControllerAdvice)
  ├── spring-boot-starter-aop (optional) (上传限制切面)
  ├── minio (optional)                  (MinIO SDK)
  ├── aliyun-sdk-oss (optional)         (阿里云 OSS SDK)
  ├── cos_api (optional)                (腾讯 COS SDK)
  └── aws-java-sdk-s3 (optional)        (AWS S3 SDK)
```

---

## 3. 架构设计

### 3.1 接口语义（对齐 S3 标准）

```
┌─────────────────────────────────────────────────────────────┐
│  OssStorageService                                           │
│                                                             │
│  ┌─────────────────┐  ┌──────────────────────────────────┐  │
│  │  直接上传        │  │  预上传（暂存）                    │  │
│  │  put()          │  │  presignStage() → stageToken      │  │
│  │  presignPut()   │  │  confirm(token) → 最终位置         │  │
│  │                 │  │  cancel(token) → 删除暂存         │  │
│  │                 │  │  purgeExpiredStages() → 清理       │  │
│  └────────┬────────┘  └──────────────┬───────────────────┘  │
│           │                          │                       │
│  ┌────────┴────────┐  ┌──────────────┴───────────────────┐  │
│  │  读取            │  │  管理                              │  │
│  │  head()         │  │  exists()                         │  │
│  │  get()          │  │  delete()                         │  │
│  │  url()          │  │  deleteBatch()                    │  │
│  │  presignGet()   │  │  copy()                           │  │
│  │  download()     │  │                                  │  │
│  └─────────────────┘  └──────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 预上传暂存流程

```
1. 前端请求上传
   POST /api/oss/stage  { filename: "合同.pdf" }
   │
   ▼
2. 后端 presignStage()
   ├─ stageToken = UUID
   ├─ stagingKey = _staging/{stageToken}
   ├─ presignedUrl (PUT, 带 metadata header)
   │   x-amz-meta-zerx-filename = 合同.pdf
   │   x-amz-meta-zerx-basepath = uploads/
   └─ 返回 OssStageResult(stageToken, presignedUrl)
   │
   ▼
3. 前端直接上传文件到 OSS（不经过后端）
   PUT {presignedUrl}  body = 文件二进制
   Headers: x-amz-meta-zerx-filename, x-amz-meta-zerx-basepath
   │
   ▼
4. 用户填写业务表单，点击"提交"
   POST /api/order/create  { stageToken: "uuid", ...业务字段 }
   │
   ▼
5. 后端 confirm(stageToken) + 保存业务数据（同一事务）
   ├─ HEAD _staging/{stageToken} → 读回 filename, basePath
   ├─ generateObjectKey(basePath, ext) → uploads/2026/05/21/uuid.pdf
   ├─ copy(_staging/uuid, uploads/2026/05/21/uuid.pdf)
   └─ delete(_staging/uuid)
   │
   ▼
6. 定时清理（每日）
   purgeExpiredStages(Duration.ofDays(1))
   → 扫描 _staging/ 下超过 1 天的文件，批量删除
```

### 3.3 暂存级别配置

```
┌─────────────────────────────────────────────────────────────┐
│  staging.bucket 未配置 → 目录级别暂存（同桶）                 │
│                                                             │
│  zerx 桶:                                                  │
│  ├── uploads/2026/05/21/uuid.pdf     ← 最终文件             │
│  ├── uploads/2026/05/21/uuid2.pdf    ← 最终文件             │
│  └── _staging/a1b2c3d4/              ← 暂存文件             │
│      └── _staging/e5f6g7h8/          ← 暂存文件             │
│                                                             │
│  staging.bucket = "zerx-staging" → 桶级别暂存（跨桶）         │
│                                                             │
│  zerx 桶:                     zerx-staging 桶:              │
│  ├── uploads/2026/05/21/uuid.pdf  ├── a1b2c3d4/  ← 暂存    │
│  └── uploads/2026/05/21/uuid2.pdf └── e5f6g7h8/  ← 暂存    │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. 核心组件

### 4.1 OssStorageService

统一存储接口，遵循 Put/Get/Head/Delete 语义。业务层仅依赖此接口。

| 方法 | 类别 | 说明 |
|------|------|------|
| `put(OssUploadRequest)` | 直接上传 | 上传 InputStream，自动生成 objectKey |
| `presignPut(OssPresignPutRequest)` | 直接上传 | 生成预签名 PUT URL（前端直传到最终位置） |
| `presignStage(OssStageRequest)` | 预上传 | 生成暂存区预签名 URL + stageToken |
| `confirm(String stageToken)` | 预上传 | 确认暂存文件，迁移到最终位置 |
| `cancel(String stageToken)` | 预上传 | 取消预上传，删除暂存文件 |
| `purgeExpiredStages(Duration)` | 预上传 | 清理过期的暂存文件 |
| `head(String objectKey)` | 读取 | 获取对象元数据（HEAD，不下载内容） |
| `get(String objectKey)` | 读取 | 获取对象（调用方关闭 InputStream） |
| `url(String objectKey)` | 读取 | 获取公开访问 URL |
| `presignGet(String objectKey, Duration)` | 读取 | 生成预签名下载 URL |
| `download(String objectKey, OutputStream)` | 读取 | 下载到输出流 |
| `exists(String objectKey)` | 管理 | 判断文件是否存在 |
| `delete(String objectKey)` | 管理 | 删除文件 |
| `deleteBatch(List<String>)` | 管理 | 批量删除，返回实际删除数 |
| `copy(String, String)` | 管理 | 复制文件 |

### 4.2 AbstractOssStorageService

公共基类，提供所有实现类共享的逻辑：

| 方法 | 说明 |
|------|------|
| `generateObjectKey(extension)` | `{basePath}/{yyyy/MM/dd}/{uuid}.{ext}` |
| `resolveBucket()` | 暂存桶（如配置）或主桶 |
| `buildUrl(objectKey)` | customDomain 或 endpoint+bucket 拼接 |
| `presignStage/confirm/cancel` | 预上传暂存的完整生命周期管理 |
| 13 个抽象 `do*` 方法 | 每个实现类按厂商 SDK 实现 |

### 4.3 stageToken 设计

stageToken 使用纯 UUID，简洁无编码：

```
stageToken = UUID.randomUUID()           → "a1b2c3d4-..."
stagingKey = "_staging/" + stageToken    → "_staging/a1b2c3d4-..."
```

暂存元数据（原始文件名、目标 basePath）存入对象自定义 Header：
- `x-amz-meta-zerx-filename` → 原始文件名
- `x-amz-meta-zerx-basepath` → 确认后的目标路径前缀

confirm 时 HEAD 请求读回元数据，生成最终 objectKey，copy 后 delete 暂存。

---

## 5. 自动配置

### 5.1 激活条件

| 条件 | 说明 |
|------|------|
| `zerx.oss.enabled=true` | 默认 true，可全局关闭 |
| `zerx.oss.type=MINIO` + `io.minio.MinioClient` 在 classpath | MinIO 实现 |
| `zerx.oss.type=ALIYUN` + `com.aliyun.oss.OSS` 在 classpath | 阿里云实现 |
| `zerx.oss.type=TENCENT` + `com.qcloud.cos.COSClient` 在 classpath | 腾讯 COS 实现 |
| `zerx.oss.type=S3` + `software.amazon.awssdk.services.s3.S3Client` 在 classpath | S3 实现 |

### 5.2 Bean 注册

`@ConditionalOnMissingBean` 确保业务可提供自定义实现。

---

## 6. 使用示例

### 6.1 配置

```yaml
# MinIO
zerx:
  oss:
    enabled: true
    type: minio
    endpoint: http://localhost:9000
    access-key: your-access-key
    secret-key: your-secret-key
    bucket: zerx
    base-path: uploads
    custom-domain: https://oss.example.com
    auto-create-bucket: true
    staging:
      bucket: zerx-staging          # 桶级别暂存（可选，不配则目录级别）
      prefix: _staging/
      default-ttl: 24h

# 阿里云 OSS
zerx:
  oss:
    type: aliyun
    endpoint: oss-cn-hangzhou.aliyuncs.com
    access-key: LTAI...
    secret-key: ...
    bucket: my-bucket
    region: oss-cn-hangzhou
```

### 6.2 直接上传

```java
@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private OssStorageService ossService;

    @PostMapping("/upload")
    public OssResult upload(@RequestParam MultipartFile file) throws IOException {
        return ossService.put(OssUploadRequest.of(
            file.getInputStream(), file.getOriginalFilename()
        ));
    }
}
```

### 6.3 预上传暂存

```java
// 1. 获取预签名 URL
OssStageResult stage = ossService.presignStage(new OssStageRequest(
    "合同.pdf", "application/pdf", "contracts/", Duration.ofHours(24)
));
// 返回 stageToken + presignedUrl 给前端

// 2. 前端直接上传到 OSS（PUT presignedUrl，携带 metadata header）

// 3. 业务提交时确认
OssConfirmResult confirmed = ossService.confirm(stageToken());
// → 文件迁移到 contracts/2026/05/21/uuid.pdf，暂存文件被删除

// 4. 取消
ossService.cancel(stageToken());
// → 暂存文件被删除
```

### 6.4 读取与下载

```java
// 获取元数据
OssObjectMeta meta = ossService.head("uploads/2026/05/21/uuid.pdf");

// 生成临时下载链接（私有桶）
PresignedUrl url = ossService.presignGet("uploads/2026/05/21/uuid.pdf", Duration.ofMinutes(30));

// 下载到流
try (OssObject obj = ossService.get("uploads/2026/05/21/uuid.pdf")) {
    Files.copy(obj.getInputStream(), Path.of("/tmp/download.pdf"));
}
```

---

## 7. 设计决策

| 决策 | 理由 |
|------|------|
| Put/Get/Head/Delete 命名 | 对齐 S3/MinIO 标准语义，降低认知成本 |
| 上传仅保留 InputStream | 统一接口签名，size/contentType 上传后从存储获取 |
| objectKey 自动生成 | 业务不关心存储路径，由组件按 `{basePath}/{yyyy/MM/dd}/{uuid}.{ext}` 规范管理 |
| 原始文件名存入元数据 | objectKey 不可变，原始文件名通过 `x-amz-meta-zerx-filename` 保留 |
| stageToken = UUID | 简洁无编码，暂存路径可直接推导（`_staging/{token}`） |
| 暂存元数据存对象 Header | 无需数据库，HEAD 请求即可读回，天然跨桶兼容 |
| 桶级别 + 目录级别暂存 | 桶级别更干净（隔离+清理安全），目录级别更轻量（同桶 copy） |
| 所有厂商 SDK optional | 按需引入，不需要 OSS 的项目零额外依赖 |
| 不依赖 zerx-spring-data | 组件层保持独立性，不引入数据库依赖 |
