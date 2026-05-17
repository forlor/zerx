package com.zerx.spring.web.client.interceptor;

import com.zerx.common.exception.ErrorCode;
import com.zerx.common.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 外部服务异常转换拦截器
 * <p>
 * 将出站 HTTP 调用中的非 2xx 响应和 I/O 异常统一转换为
 * {@link ExternalServiceException}，使业务代码无需处理底层 HTTP 异常。
 * </p>
 *
 * <h3>异常映射规则：</h3>
 * <table>
 *   <tr><th>原始异常</th><th>ErrorCode</th><th>说明</th></tr>
 *   <tr><td>429 Too Many Requests</td><td>EXTERNAL_SERVICE_RATE_LIMIT</td><td>外部服务限流</td></tr>
 *   <tr><td>4xx (其他)</td><td>EXTERNAL_SERVICE_DATA_ERROR</td><td>请求参数/数据异常</td></tr>
 *   <tr><td>5xx</td><td>EXTERNAL_SERVICE_ERROR</td><td>外部服务内部错误</td></tr>
 *   <tr><td>ConnectTimeout / ReadTimeout</td><td>EXTERNAL_SERVICE_TIMEOUT</td><td>连接/读取超时</td></tr>
 *   <tr><td>其他 IOException</td><td>NETWORK_ERROR</td><td>网络连接异常</td></tr>
 * </table>
 *
 * @author zerx
 */
public class ErrorResponseInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ErrorResponseInterceptor.class);

    /** 响应体最大读取长度（用于错误日志），防止 OOM */
    private static final int MAX_ERROR_BODY_LENGTH = 2048;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                         ClientHttpRequestExecution execution) throws IOException {
        try {
            ClientHttpResponse response = execution.execute(request, body);
            HttpStatusCode status = response.getStatusCode();

            // 2xx 直接放行
            if (status.is2xxSuccessful()) {
                return response;
            }

            // 非 2xx → 读取响应体并转换为 ExternalServiceException
            String responseBody = readErrorBody(response);
            String uri = request.getURI().toString();
            String method = request.getMethod().name();
            int statusCode = status.value();

            ErrorCode errorCode = mapErrorCode(statusCode);
            String message = String.format("外部服务 [%s] %s 返回 %d: %s",
                    extractHost(uri), method, statusCode, truncate(responseBody, 200));

            log.warn("External service error: {} {} -> {}: {}", method, uri, statusCode, responseBody);

            throw new ExternalServiceException(errorCode, message, null, extractHost(uri));
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Spring 已包装的 HTTP 异常，直接映射
            String uri = request.getURI().toString();
            int statusCode = e.getStatusCode().value();
            ErrorCode errorCode = mapErrorCode(statusCode);
            String message = String.format("外部服务 [%s] %s 返回 %d: %s",
                    extractHost(uri), request.getMethod().name(), statusCode,
                    truncate(e.getResponseBodyAsString(), 200));

            throw new ExternalServiceException(errorCode, message, e.getCause(), extractHost(uri));
        } catch (ResourceAccessException e) {
            // 连接超时 / 读取超时 / 网络 I/O 异常
            String uri = request.getURI().toString();
            String message = String.format("外部服务 [%s] 调用失败: %s",
                    extractHost(uri), e.getMessage());

            if (isTimeout(e)) {
                throw new ExternalServiceException(ErrorCode.EXTERNAL_SERVICE_TIMEOUT, message, e, extractHost(uri));
            }
            throw new ExternalServiceException(ErrorCode.NETWORK_ERROR, message, e, extractHost(uri));
        }
    }

    /**
     * 根据 HTTP 状态码映射到对应的 ErrorCode
     */
    private ErrorCode mapErrorCode(int statusCode) {
        return switch (statusCode) {
            case 429 -> ErrorCode.EXTERNAL_SERVICE_RATE_LIMIT;
            case 404 -> ErrorCode.EXTERNAL_SERVICE_ERROR;  // 404 通常代表接口地址错误
            default -> {
                if (statusCode >= 500) {
                    yield ErrorCode.EXTERNAL_SERVICE_ERROR;
                }
                // 4xx (非 429/404) → 数据异常（参数格式、鉴权失败等）
                yield ErrorCode.EXTERNAL_SERVICE_DATA_ERROR;
            }
        };
    }

    /**
     * 判断异常是否为超时类型
     */
    private boolean isTimeout(ResourceAccessException e) {
        String msg = e.getMessage();
        if (msg == null) {
            return false;
        }
        String lower = msg.toLowerCase();
        return lower.contains("timeout") || lower.contains("timed out")
                || lower.contains("connect timed out") || lower.contains("read timed out");
    }

    /**
     * 从 URI 中提取 Host 用于日志记录
     */
    private String extractHost(String uri) {
        try {
            java.net.URI parsed = java.net.URI.create(uri);
            return parsed.getHost() != null ? parsed.getHost() : uri;
        } catch (Exception e) {
            return uri;
        }
    }

    /**
     * 读取错误响应体
     */
    private String readErrorBody(ClientHttpResponse response) {
        try (InputStream is = response.getBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            is.transferTo(baos);
            return baos.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "[failed to read response body: " + e.getMessage() + "]";
        }
    }

    /**
     * 截断字符串到指定长度
     */
    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
