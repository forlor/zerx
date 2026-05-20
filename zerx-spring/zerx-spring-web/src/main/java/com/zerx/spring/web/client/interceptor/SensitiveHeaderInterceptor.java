package com.zerx.spring.web.client.interceptor;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * 敏感 Header 脱敏拦截器
 * <p>
 * 在出站请求日志中，对 Authorization、X-Token 等敏感 Header 的值进行掩码处理，
 * 防止凭据信息泄露到日志文件中。该拦截器修改请求 Header 为掩码值后执行请求，
 * 适用于纯日志脱敏场景（请求仍然携带真实 Header 发送）。
 * </p>
 *
 * <h3>性能设计：</h3>
 * <ul>
 *   <li>敏感 Header 名称集合在构造时预编译为小写 {@link Set}，匹配时 O(1)</li>
 *   <li>仅在请求包含目标 Header 时才执行字符串操作</li>
 *   <li>正则模式预编译，避免每次请求重复编译</li>
 * </ul>
 *
 * @author zerx
 */
public class SensitiveHeaderInterceptor implements ClientHttpRequestInterceptor {

    /** 掩码字符串 */
    private static final String MASK_VALUE = "******";

    /** 预编译：匹配 Bearer Token 格式（Bearer + 空格 + 任意内容） */
    private static final Pattern BEARER_PATTERN = Pattern.compile(
            "^[Bb]earer\\s+.+$", Pattern.CASE_INSENSITIVE);

    /** 需要脱敏的 Header 名称（小写），预计算为 Set */
    private final Set<String> sensitiveHeaders;

    /**
     * 使用默认敏感 Header 列表构造
     * <p>
     * 默认脱敏：authorization、x-token、x-api-key、x-secret、proxy-authorization
     * </p>
     */
    public SensitiveHeaderInterceptor() {
        this(Set.of(
                "authorization", "x-token", "x-api-key", "x-secret", "proxy-authorization"
        ));
    }

    /**
     * 使用自定义敏感 Header 列表构造
     *
     * @param sensitiveHeaders 需要脱敏的 Header 名称集合（不区分大小写）
     */
    public SensitiveHeaderInterceptor(Set<String> sensitiveHeaders) {
        this.sensitiveHeaders = Set.copyOf(sensitiveHeaders);
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                         ClientHttpRequestExecution execution) throws IOException {
        // 遍历 Header 并对敏感值进行掩码处理（仅影响日志，请求仍携带原始值）
        request.getHeaders().forEach((name, values) -> {
            if (isSensitive(name)) {
                List<String> maskedValues = values.stream()
                        .map(this::maskValue)
                        .toList();
                request.getHeaders().put(name, maskedValues);
            }
        });
        return execution.execute(request, body);
    }

    /**
     * 判断 Header 名称是否为敏感 Header
     *
     * @param headerName Header 名称
     * @return 敏感返回 {@code true}
     */
    private boolean isSensitive(String headerName) {
        return sensitiveHeaders.contains(headerName.toLowerCase());
    }

    /**
     * 对单个 Header 值进行掩码处理
     * <p>
     * Bearer Token 格式：保留 "Bearer " 前缀，替换 token 部分为掩码；
     * 其他格式：完全替换为掩码。
     * </p>
     *
     * @param value 原始 Header 值
     * @return 掩码后的值
     */
    private String maskValue(String value) {
        if (value == null || value.isBlank()) {
            return MASK_VALUE;
        }
        // 保留 Bearer 前缀结构，便于识别认证方式
        if (BEARER_PATTERN.matcher(value).matches()) {
            int spaceIdx = value.indexOf(' ');
            return value.substring(0, spaceIdx + 1) + MASK_VALUE;
        }
        return MASK_VALUE;
    }
}
