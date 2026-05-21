/**
 * 对象存储组件
 * <p>
 * 提供统一的对象存储操作抽象，支持 MinIO、S3、阿里云 OSS 等多种存储后端。
 * 包含直接上传、预签名上传、暂存（预上传）、对象读取和管理等核心功能。
 * </p>
 *
 * <h3>核心类：</h3>
 * <ul>
 *   <li>{@link com.zerx.component.oss.OssStorageService} — 对象存储服务接口，定义所有存储操作</li>
 *   <li>{@link com.zerx.component.oss.OssException} — OSS 操作异常</li>
 *   <li>{@link com.zerx.component.oss.OssResult} — 上传/复制操作结果</li>
 *   <li>{@link com.zerx.component.oss.OssObject} — 对象内容与元数据封装</li>
 *   <li>{@link com.zerx.component.oss.OssObjectMeta} — 对象元数据</li>
 *   <li>{@link com.zerx.component.oss.PresignedUrl} — 预签名 URL</li>
 *   <li>{@link com.zerx.component.oss.OssStageResult} — 暂存预上传结果</li>
 *   <li>{@link com.zerx.component.oss.OssConfirmResult} — 暂存确认结果</li>
 * </ul>
 *
 * <h3>子包：</h3>
 * <ul>
 *   <li>{@code annotation} — 自定义注解（如限流、权限控制）</li>
 *   <li>{@code aspect} — AOP 切面（注解驱动逻辑）</li>
 *   <li>{@code autoconfigure} — Spring Boot 自动配置</li>
 *   <li>{@code impl} — 接口实现类</li>
 *   <li>{@code properties} — 配置属性类</li>
 * </ul>
 */
package com.zerx.component.oss;
