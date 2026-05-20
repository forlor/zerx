package com.zerx.spring.web.client.interceptor;

import java.io.IOException;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.zerx.common.exception.ExternalServiceException;
import com.zerx.spring.web.properties.ZerxWebProperties;

/**
 * HTTP 请求重试拦截器
 * <p>
 * 对可重试的异常类型（5xx 服务端错误、429 限流、网络超时/连接异常）自动进行指数退避重试，
 * 集成 {@link com.zerx.common.retry.Retryer Retryer} 的退避策略。
 * </p>
 *
 * <h3>可重试条件：</h3>
 * <ul>
 *   <li>HTTP 5xx 服务端错误（服务端暂时性故障）</li>
 *   <li>HTTP 429 Too Many Requests（触发限流）</li>
 *   <li>网络连接超时 / 读取超时（{@link ResourceAccessException} 含 timeout 关键字）</li>
 *   <li>非超时的 I/O 异常（{@link IOException}）</li>
 * </ul>
 *
 * <h3>不可重试条件：</h3>
 * <ul>
 *   <li>HTTP 4xx 客户端错误（参数错误、鉴权失败等，重试无意义）</li>
 *   <li>{@link ExternalServiceException}（已被其他拦截器转换，不重复重试）</li>
 *   <li>{@link InterruptedException}（恢复中断标志后立即退出）</li>
 * </ul>
 *
 * <h3>性能设计：</h3>
 * <ul>
 *   <li>退避计算使用位运算 {@code 1L << attempt}，无 Math.pow 开销</li>
 *   <li>最大重试次数为 0 时直接跳过，零分支判断</li>
 *   <li>Thread.sleep 精度依赖 OS 调度，对性能影响可忽略</li>
 * </ul>
 *
 * @author zerx
 */
public class RetryInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(RetryInterceptor.class);

    /** 最大重试次数（0 = 不重试） */
    private final int maxRetries;

    /** 退避策略初始延迟 */
    private final Duration initialDelay;

    /** 退避策略最大延迟 */
    private final Duration maxDelay;

    /** 是否启用抖动 */
    private final boolean jitterEnabled;

    /**
     * 根据配置属性构造
     *
     * @param config HTTP 客户端配置
     */
    public RetryInterceptor(ZerxWebProperties.HttpClient config) {
        this.maxRetries = config.getMaxRetries();
        this.initialDelay = Duration.ofMillis(config.getRetryInitialDelayMs());
        this.maxDelay = Duration.ofMillis(config.getRetryMaxDelayMs());
        this.jitterEnabled = config.isRetryJitterEnabled();
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                         ClientHttpRequestExecution execution) throws IOException {
        if (maxRetries <= 0) {
            return execution.execute(request, body);
        }

        IOException lastException = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                ClientHttpResponse response = execution.execute(request, body);
                int status = response.getStatusCode().value();

                // 5xx 或 429 → 可重试
                if (status >= 500 || status == 429) {
                    if (attempt < maxRetries) {
                        LOG.warn("OUTBOUND {} {} -> {} (attempt {}/{}, will retry)",
                                request.getMethod(), request.getURI(), status,
                                attempt + 1, maxRetries + 1);
                        // 关闭当前响应体以释放连接
                        response.close();
                        sleep(calculateDelay(attempt));
                        continue;
                    }
                }
                return response;
            } catch (ExternalServiceException e) {
                // 已被 ErrorResponseInterceptor 转换 → 不重试，直接抛出
                throw new IOException("External service error (not retryable): " + e.getMessage(), e);
            } catch (HttpServerErrorException e) {
                if (attempt < maxRetries && isRetryableStatus(e.getStatusCode().value())) {
                    LOG.warn("OUTBOUND {} {} -> {} (attempt {}/{}, will retry)",
                            request.getMethod(), request.getURI(), e.getStatusCode().value(),
                            attempt + 1, maxRetries + 1);
                    sleep(calculateDelay(attempt));
                    continue;
                }
                throw e;
            } catch (ResourceAccessException e) {
                if (attempt < maxRetries && isRetryableNetworkError(e)) {
                    LOG.warn("OUTBOUND {} {} -> NETWORK_ERROR (attempt {}/{}, will retry): {}",
                            request.getMethod(), request.getURI(),
                            attempt + 1, maxRetries + 1, e.getMessage());
                    sleep(calculateDelay(attempt));
                    continue;
                }
                throw e;
            } catch (IOException e) {
                lastException = e;
                if (attempt < maxRetries) {
                    LOG.warn("OUTBOUND {} {} -> IO_ERROR (attempt {}/{}, will retry): {}",
                            request.getMethod(), request.getURI(),
                            attempt + 1, maxRetries + 1, e.getMessage());
                    sleep(calculateDelay(attempt));
                    continue;
                }
                throw e;
            }
        }
        throw lastException != null ? lastException : new IOException("Max retries exceeded");
    }

    /**
     * 计算指数退避延迟
     */
    private long calculateDelay(int attempt) {
        long delay = initialDelay.toMillis() * (1L << Math.min(attempt, 30));
        delay = Math.min(delay, maxDelay.toMillis());
        if (jitterEnabled) {
            long bound = Math.max(1, delay / 4);
            delay += ThreadLocalRandomProvider.nextLong(-bound, bound + 1);
        }
        return Math.max(0, delay);
    }

    /**
     * 判断 HTTP 状态码是否可重试
     */
    private boolean isRetryableStatus(int status) {
        return status >= 500 || status == 429;
    }

    /**
     * 判断网络异常是否可重试（超时或连接异常均可重试）
     */
    private boolean isRetryableNetworkError(ResourceAccessException e) {
        return true; // ResourceAccessException 本身就是网络/连接问题，通常可重试
    }

    /**
     * 延迟等待
     */
    private void sleep(long millis) throws IOException {
        if (millis > 0) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Retry sleep interrupted", e);
            }
        }
    }

    /**
     * ThreadLocalRandom 提供器，隔离随机数生成
     */
    static final class ThreadLocalRandomProvider {
        static long nextLong(long origin, long bound) {
            return java.util.concurrent.ThreadLocalRandom.current().nextLong(origin, bound);
        }
    }
}
