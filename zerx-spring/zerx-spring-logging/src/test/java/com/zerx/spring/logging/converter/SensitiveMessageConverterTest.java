package com.zerx.spring.logging.converter;

import com.zerx.common.logging.SensitiveLogFilter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link SensitiveMessageConverter} 单元测试
 *
 * @author zerx
 */
class SensitiveMessageConverterTest {

    @Test
    void converter_delegates_to_sensitive_log_filter() {
        // Verify the converter uses SensitiveLogFilter
        String input = "用户手机13812345678登录";
        String expected = SensitiveLogFilter.filter(input);

        SensitiveMessageConverter converter = new SensitiveMessageConverter();
        ch.qos.logback.classic.spi.LoggingEvent event = createMockEvent(input);

        String result = converter.convert(event);
        assertEquals(expected, result);
    }

    @Test
    void filter_mobile_phone() {
        SensitiveMessageConverter converter = new SensitiveMessageConverter();
        ch.qos.logback.classic.spi.LoggingEvent event = createMockEvent("用户手机13812345678登录");

        String result = converter.convert(event);
        assertEquals("用户手机138****5678登录", result);
    }

    @Test
    void filter_email() {
        // user (4 chars) → u**r, so masked: u**r@example.com
        String input = "邮箱user@example.com";
        String result = SensitiveLogFilter.filter(input);
        assertTrue(result.contains("**"));
        assertNotEquals(input, result);
    }

    @Test
    void filter_password() {
        SensitiveMessageConverter converter = new SensitiveMessageConverter();
        ch.qos.logback.classic.spi.LoggingEvent event = createMockEvent("password=pwd123");

        String result = converter.convert(event);
        assertEquals("password=******", result);
    }

    @Test
    void no_change_for_clean_message() {
        SensitiveMessageConverter converter = new SensitiveMessageConverter();
        ch.qos.logback.classic.spi.LoggingEvent event = createMockEvent("这是一条普通日志消息");

        String result = converter.convert(event);
        assertEquals("这是一条普通日志消息", result);
    }

    @Test
    void handle_null_message() {
        SensitiveMessageConverter converter = new SensitiveMessageConverter();
        ch.qos.logback.classic.spi.LoggingEvent event = createMockEvent(null);

        String result = converter.convert(event);
        assertNull(result);
    }

    @Test
    void handle_empty_message() {
        SensitiveMessageConverter converter = new SensitiveMessageConverter();
        ch.qos.logback.classic.spi.LoggingEvent event = createMockEvent("");

        String result = converter.convert(event);
        assertEquals("", result);
    }

    @Test
    void filter_multiple_sensitive_types_via_filter() {
        // Use English password keyword (password=) since SensitiveLogFilter regex only matches English
        String input = "用户手机13812345678登录，邮箱user@example.com，password=mysecret";
        String result = SensitiveLogFilter.filter(input);

        assertFalse(result.contains("13812345678"));
        assertTrue(result.contains("138****5678"));
        assertFalse(result.contains("user@example.com"));
        assertFalse(result.contains("mysecret"));
        assertTrue(result.contains("******"));
    }

    private ch.qos.logback.classic.spi.LoggingEvent createMockEvent(String message) {
        ch.qos.logback.classic.spi.LoggingEvent event =
                new ch.qos.logback.classic.spi.LoggingEvent();
        if (message != null) {
            event.setMessage(message);
        }
        return event;
    }
}
