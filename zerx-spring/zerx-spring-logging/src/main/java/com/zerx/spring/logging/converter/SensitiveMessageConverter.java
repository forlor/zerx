package com.zerx.spring.logging.converter;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.zerx.common.logging.SensitiveLogFilter;

/**
 * Logback Pattern 转换器 — 脱敏日志消息
 * <p>
 * 在 Logback 的 {@code <pattern>} 中使用 {@code %zerxMsg} 代替 {@code %msg}，
 * 自动对日志消息中的敏感数据进行脱敏处理。
 * </p>
 *
 * <h3>Logback 配置示例：</h3>
 * <pre>{@code
 * <configuration>
 *     <conversionRule conversionWord="zerxMsg"
 *                     converterClass="com.zerx.spring.logging.converter.SensitiveMessageConverter"/>
 *
 *     <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
 *         <encoder>
 *             <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %zerxMsg%n</pattern>
 *         </encoder>
 *     </appender>
 * </configuration>
 * }</pre>
 *
 * @author zerx
 */
public class SensitiveMessageConverter extends MessageConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String originalMessage = super.convert(event);
        if (originalMessage == null || originalMessage.isEmpty()) {
            return originalMessage;
        }
        return SensitiveLogFilter.filter(originalMessage);
    }
}
