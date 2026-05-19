package com.zerx.spring.logging.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.EncoderBase;
import com.zerx.common.logging.SensitiveLogFilter;

import java.nio.charset.StandardCharsets;

/**
 * Logback Encoder 包装器 — 对编码后的日志字节进行脱敏过滤
 * <p>
 * 包裹在已有的 Encoder 之外，对最终输出的字节进行
 * {@link SensitiveLogFilter#filter(String)} 脱敏处理。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <p>
 * 当你不想修改 pattern 中的 {@code %msg} 为自定义转换器时，
 * 可以使用此 encoder 直接包裹原有 encoder，实现无侵入的日志脱敏。
 * </p>
 *
 * <h3>Logback 配置示例：</h3>
 * <pre>{@code
 * <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
 *     <encoder class="com.zerx.spring.logging.appender.ZerxSensitiveLogEncoder">
 *         <delegate class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
 *             <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
 *         </delegate>
 *     </encoder>
 * </appender>
 * }</pre>
 *
 * @author zerx
 */
public class ZerxSensitiveLogEncoder extends EncoderBase<ILoggingEvent> {

    private ch.qos.logback.core.encoder.Encoder<ILoggingEvent> delegate;

    /**
     * 设置被包裹的 Encoder
     *
     * @param delegate 目标 Encoder
     */
    public void setDelegate(ch.qos.logback.core.encoder.Encoder<ILoggingEvent> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void start() {
        if (delegate == null) {
            addError("No delegate encoder configured for " + getClass().getSimpleName());
            return;
        }
        if (!delegate.isStarted()) {
            delegate.start();
        }
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        if (delegate != null) {
            delegate.stop();
        }
    }

    @Override
    public byte[] headerBytes() {
        return delegate != null ? delegate.headerBytes() : null;
    }

    @Override
    public byte[] footerBytes() {
        return delegate != null ? delegate.footerBytes() : null;
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        if (delegate == null) {
            return null;
        }
        byte[] bytes = delegate.encode(event);
        if (bytes == null || bytes.length == 0) {
            return bytes;
        }
        String str = new String(bytes, StandardCharsets.UTF_8);
        String filtered = SensitiveLogFilter.filter(str);
        if (filtered.equals(str)) {
            return bytes;
        }
        return filtered.getBytes(StandardCharsets.UTF_8);
    }

}
