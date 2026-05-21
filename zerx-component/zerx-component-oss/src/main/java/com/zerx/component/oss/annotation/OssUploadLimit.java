package com.zerx.component.oss.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * OSS 上传限制注解 — 控制文件上传的扩展名、大小和路径。
 * <p>
 * 标注在 Controller 方法上，在方法执行前对上传参数进行校验。
 * 如果违反限制条件，将抛出 {@link com.zerx.component.oss.OssException}。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 限制只能上传图片，最大 5MB
 * &#64;OssUploadLimit(allowedExtensions = {"png", "jpg", "jpeg", "gif"}, maxSize = "5MB")
 * &#64;PostMapping("/upload/image")
 * public Result<OssResult> uploadImage(OssUploadRequest request) { ... }
 *
 * // 限制必须上传到指定路径，最大 100MB
 * &#64;OssUploadLimit(requiredPrefix = "documents/", maxSize = "100MB")
 * &#64;PostMapping("/upload/document")
 * public Result<OssResult> uploadDocument(OssUploadRequest request) { ... }
 * }</pre>
 *
 * @author zerx
 * @see com.zerx.component.oss.aspect.OssUploadLimitAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OssUploadLimit {

    /**
     * 允许的文件扩展名列表（不含点号，如 {@code {"png", "jpg", "pdf"}}）。
     * <p>
     * 空数组表示不限制扩展名。扩展名比较不区分大小写。
     * </p>
     *
     * @return 允许的文件扩展名数组
     */
    String[] allowedExtensions() default {};

    /**
     * 最大文件大小（如 {@code "10MB"}、{@code "500KB"}、{@code "1GB"}）。
     * <p>
     * 支持的单位：KB、MB、GB（不区分大小写）。默认 10MB。
     * 当上传请求的文件大小超过此限制时，将抛出异常。
     * </p>
     *
     * @return 最大文件大小字符串
     */
    String maxSize() default "10MB";

    /**
     * 必需的存储路径前缀。
     * <p>
     * 如果非空，上传请求中的 basePath 必须以此前缀开头，否则抛出异常。
     * 空字符串表示不限制路径前缀。
     * </p>
     *
     * @return 必需的路径前缀
     */
    String requiredPrefix() default "";
}
