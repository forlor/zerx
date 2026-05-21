package com.zerx.component.oss.aspect;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zerx.component.oss.OssException;
import com.zerx.component.oss.OssUploadRequest;
import com.zerx.component.oss.annotation.OssUploadLimit;

/**
 * OSS 上传限制切面。
 * <p>
 * 拦截标注了 {@link OssUploadLimit} 注解的方法，在上传操作执行前校验：
 * <ul>
 *   <li>文件扩展名是否在允许列表中</li>
 *   <li>上传请求的 basePath 是否以必需的前缀开头</li>
 * </ul>
 * </p>
 * <p>
 * 注意：文件大小限制建议在框架层（如 Spring 的 {@code max-file-size}）或网关层控制，
 * 因为本切面处理的是 {@link OssUploadRequest}（已接收的 InputStream），
 * 无法在不消费流的情况下获取准确的大小。
 * </p>
 *
 * @author zerx
 * @see OssUploadLimit
 */
@Aspect
public class OssUploadLimitAspect {

    private static final Logger LOG = LoggerFactory.getLogger(OssUploadLimitAspect.class);

    /**
     * 校验上传请求的限制条件。
     * <p>
     * 从方法参数中查找 {@link OssUploadRequest} 类型的参数，
     * 然后根据 {@link OssUploadLimit} 注解的配置进行校验。
     * </p>
     *
     * @param joinPoint 连接点
     * @throws OssException 如果校验未通过
     */
    @Before("@annotation(limit)")
    public void checkUploadLimit(JoinPoint joinPoint, OssUploadLimit limit) {
        OssUploadRequest request = findUploadRequest(joinPoint.getArgs());
        if (request == null) {
            LOG.debug("未找到 OssUploadRequest 参数，跳过上传限制校验");
            return;
        }

        // 校验扩展名
        String[] allowedExtensions = limit.allowedExtensions();
        if (allowedExtensions.length > 0) {
            String filename = request.filename();
            String extension = extractExtension(filename);
            Set<String> allowedSet = new HashSet<>();
            for (String ext : allowedExtensions) {
                allowedSet.add(ext.toLowerCase());
            }
            if (!allowedSet.contains(extension.toLowerCase())) {
                throw OssException.ossError("不支持的文件类型: " + extension
                        + "，允许的类型: " + Arrays.toString(allowedExtensions));
            }
        }

        // 校验路径前缀
        String requiredPrefix = limit.requiredPrefix();
        if (requiredPrefix != null && !requiredPrefix.isEmpty()) {
            String basePath = request.basePath();
            if (basePath == null || !basePath.startsWith(requiredPrefix)) {
                throw OssException.ossError("上传路径必须以 \"" + requiredPrefix + "\" 开头");
            }
        }
    }

    /**
     * 从方法参数中查找第一个 {@link OssUploadRequest} 类型的参数。
     *
     * @param args 方法参数数组
     * @return 找到的 OssUploadRequest，未找到返回 {@code null}
     */
    private OssUploadRequest findUploadRequest(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof OssUploadRequest) {
                return (OssUploadRequest) arg;
            }
        }
        return null;
    }

    /**
     * 从文件名中提取扩展名（不含点号，小写）。
     *
     * @param filename 文件名
     * @return 小写扩展名，无扩展名时返回空字符串
     */
    private String extractExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }
}
