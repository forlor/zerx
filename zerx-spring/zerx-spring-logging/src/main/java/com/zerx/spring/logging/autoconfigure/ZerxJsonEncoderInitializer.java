package com.zerx.spring.logging.autoconfigure;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.Encoder;
import com.zerx.spring.logging.appender.ZerxJsonEncoder;
import com.zerx.spring.logging.properties.ZerxLoggingProperties;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

/**
 * JSON 日志 Encoder 初始化器。
 * <p>
 * 在 Spring 容器启动后，自动查找 Logback 的 Console Appender
 * 并替换其 Encoder 为 {@link ZerxJsonEncoder}。
 * </p>
 *
 * @author zerx
 */
class ZerxJsonEncoderInitializer implements SmartLifecycle {

    private final ZerxLoggingProperties properties;
    private volatile boolean running = false;

    ZerxJsonEncoderInitializer(ZerxLoggingProperties properties) {
        this.properties = properties;
    }

    @Override
    public void start() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ZerxLoggingProperties.JsonFormat config = properties.getJsonFormat();

        ZerxJsonEncoder jsonEncoder = new ZerxJsonEncoder();
        jsonEncoder.setIncludeMdc(config.isIncludeMdc());
        jsonEncoder.setIncludeLoggerName(config.isIncludeLoggerName());
        jsonEncoder.setIncludeThreadName(config.isIncludeThreadName());
        jsonEncoder.setPrettyPrint(config.isPrettyPrint());
        jsonEncoder.setContext(loggerContext);
        jsonEncoder.start();

        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        var iterator = rootLogger.iteratorForAppenders();
        while (iterator.hasNext()) {
            Appender<ILoggingEvent> appender = iterator.next();
            if (appender instanceof ConsoleAppender<ILoggingEvent> consoleAppender) {
                Encoder<ILoggingEvent> oldEncoder = consoleAppender.getEncoder();
                if (oldEncoder != null) {
                    oldEncoder.stop();
                }
                consoleAppender.setEncoder(jsonEncoder);
            }
        }
        running = true;
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE; // 最后执行
    }
}
