package com.zerx.spring.web.client.interceptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * 出站请求访问日志拦截器
 * <p>
 * 记录所有通过 RestClient 发出的出站 HTTP 请求和响应的关键信息，
 * 包括：HTTP 方法、URL、状态码、耗时、请求体（截断）、响应体（截断）。
 * </p>
 *
 * <h3>高性能设计：</h3>
 * <ul>
 *   <li>响应体缓冲后重新包装为可重复读取的 {@link ClientHttpResponse}，不丢失数据</li>
 *   <li>响应体截断为可配置的最大长度，防止大响应拖慢日志</li>
 *   <li>日志格式化在请求完成后执行，不影响 I/O 性能</li>
 *   <li>耗时基于 {@code System.nanoTime()}，纳秒级精度</li>
 * </ul>
 *
 * @author zerx
 */
public class OutboundAccessLogInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(OutboundAccessLogInterceptor.class);

    /** 响应体最大记录长度（字节），0 表示不记录响应体 */
    private final int maxResponseBodyLength;

    /** 响应体最大缓冲大小（字节），超过此大小的响应体不缓冲 */
    private final int maxBodyBufferSize;

    /**
     * 使用默认配置构造（日志截断 1024 字节，缓冲上限 1MB）
     */
    public OutboundAccessLogInterceptor() {
        this(1024, 1024 * 1024);
    }

    /**
     * 使用自定义最大响应体长度构造
     *
     * @param maxResponseBodyLength 响应体最大记录长度（字节），0 = 不记录
     */
    public OutboundAccessLogInterceptor(int maxResponseBodyLength) {
        this(maxResponseBodyLength, 1024 * 1024);
    }

    /**
     * @param maxResponseBodyLength 响应体最大记录长度（字节），0 = 不记录
     * @param maxBodyBufferSize     响应体最大缓冲大小（字节），超限不缓冲
     */
    public OutboundAccessLogInterceptor(int maxResponseBodyLength, int maxBodyBufferSize) {
        this.maxResponseBodyLength = maxResponseBodyLength;
        this.maxBodyBufferSize = maxBodyBufferSize;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                         ClientHttpRequestExecution execution) throws IOException {
        long startNanos = System.nanoTime();
        String requestBody = maskBody(body != null ? new String(body, StandardCharsets.UTF_8) : "");

        ClientHttpResponse response = execution.execute(request, body);

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

        // 检查 Content-Length 避免大响应体 OOM
        long contentLength = response.getHeaders().getContentLength();
        BufferedClientHttpResponse bufferedResponse;
        String responseBody;
        if (contentLength > maxBodyBufferSize) {
            bufferedResponse = null;
            responseBody = "[body skipped: Content-Length " + contentLength + " exceeds buffer limit]";
        } else {
            bufferedResponse = new BufferedClientHttpResponse(response, maxBodyBufferSize);
            responseBody = truncateBody(bufferedResponse.getBodyBytes());
        }

        int status = response.getStatusCode().value();
        String statusColor = status >= 500 ? "[SERVER_ERROR]" : status >= 400 ? "[CLIENT_ERROR]" : "";

        if (LOG.isInfoEnabled()) {
            LOG.info("{} OUTBOUND {} {} -> {} ({}ms) request={} response={}",
                    statusColor,
                    request.getMethod(),
                    request.getURI(),
                    status,
                    elapsedMs,
                    requestBody,
                    responseBody);
        }

        return bufferedResponse != null ? bufferedResponse : response;
    }

    /**
     * 截断响应体到最大长度
     */
    private String truncateBody(byte[] bytes) {
        if (maxResponseBodyLength <= 0) {
            return "[skipped]";
        }
        if (bytes.length > maxResponseBodyLength) {
            // 按字节截断后解码，避免截断多字节字符产生乱码
            int safeLen = maxResponseBodyLength;
            while (safeLen > 0 && isUtf8ContinuationByte(bytes[safeLen])) {
                safeLen--;
            }
            return new String(bytes, 0, safeLen, StandardCharsets.UTF_8)
                    + "...[" + bytes.length + " bytes total]";
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static boolean isUtf8ContinuationByte(byte b) {
        return (b & 0xC0) == 0x80;
    }

    /**
     * 对请求体进行简单脱敏（截断过长内容）
     */
    private String maskBody(String body) {
        if (body == null || body.isEmpty()) {
            return "";
        }
        if (body.length() > 512) {
            return body.substring(0, 512) + "...[truncated]";
        }
        return body;
    }

    /**
     * 缓冲响应体的 {@link ClientHttpResponse} 包装。
     * <p>
     * 读取原始响应体到内存后，提供可重复读取的 InputStream，
     * 确保调用方在拦截器之后仍能正常读取响应体。
     * </p>
     */
    private static class BufferedClientHttpResponse implements ClientHttpResponse {

        private final ClientHttpResponse delegate;
        private final byte[] bodyBytes;
        private final HttpStatusCode statusCode;
        private final HttpHeaders headers;
        private final String statusText;

        BufferedClientHttpResponse(ClientHttpResponse delegate, int maxBufferSize) throws IOException {
            this.statusCode = delegate.getStatusCode();
            this.headers = delegate.getHeaders();
            try (InputStream bodyStream = delegate.getBody()) {
                this.bodyBytes = readLimited(bodyStream, maxBufferSize);
            }
            this.delegate = delegate;
            String raw = delegate.getStatusText();
            this.statusText = raw != null ? raw : "";
        }

        private static byte[] readLimited(InputStream in, int max) throws IOException {
            byte[] buf = new byte[8192];
            var out = new java.io.ByteArrayOutputStream();
            int total = 0;
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                total += n;
                if (total > max) {
                    return out.toByteArray();
                }
            }
            return out.toByteArray();
        }

        byte[] getBodyBytes() {
            return bodyBytes;
        }

        @Override
        public InputStream getBody() {
            return bodyBytes.length > 0 ? new ByteArrayInputStream(bodyBytes) : InputStream.nullInputStream();
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return statusCode;
        }

        @Override
        public String getStatusText() throws IOException {
            HttpStatus httpStatus = HttpStatus.resolve(statusCode.value());
            return httpStatus != null ? httpStatus.getReasonPhrase() : statusText;
        }

        @Override
        public void close() {
            delegate.close();
        }
    }
}
