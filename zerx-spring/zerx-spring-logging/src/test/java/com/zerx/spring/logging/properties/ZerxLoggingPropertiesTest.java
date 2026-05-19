package com.zerx.spring.logging.properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ZerxLoggingProperties} 单元测试
 *
 * @author zerx
 */
class ZerxLoggingPropertiesTest {

    @Test
    void default_values() {
        ZerxLoggingProperties props = new ZerxLoggingProperties();

        // Top-level
        assertTrue(props.isEnabled());

        // OpLog
        assertTrue(props.getOpLog().isEnabled());
        assertTrue(props.getOpLog().isLogToLogger());
        assertFalse(props.getOpLog().isRecordExceptionStackTrace());
        assertEquals(1024, props.getOpLog().getMaxParamLength());
        assertEquals(2048, props.getOpLog().getMaxResultLength());

        // SensitiveLog
        assertFalse(props.getSensitiveLog().isEnabled());
        assertTrue(props.getSensitiveLog().isFilterMobile());
        assertTrue(props.getSensitiveLog().isFilterEmail());
        assertTrue(props.getSensitiveLog().isFilterIdCard());
        assertTrue(props.getSensitiveLog().isFilterBankCard());
        assertTrue(props.getSensitiveLog().isFilterPassword());
        assertFalse(props.getSensitiveLog().isFilterIpv4());

        // RateLimit
        assertFalse(props.getRateLimit().isEnabled());
        assertEquals(100, props.getRateLimit().getMaxPerSecond());

        // JsonFormat
        assertFalse(props.getJsonFormat().isEnabled());
        assertTrue(props.getJsonFormat().isIncludeMdc());
        assertTrue(props.getJsonFormat().isIncludeLoggerName());
        assertFalse(props.getJsonFormat().isIncludeThreadName());
        assertFalse(props.getJsonFormat().isPrettyPrint());
    }

    @Test
    void set_enabled() {
        ZerxLoggingProperties props = new ZerxLoggingProperties();
        props.setEnabled(false);
        assertFalse(props.isEnabled());
    }

    @Test
    void set_opLog_properties() {
        ZerxLoggingProperties props = new ZerxLoggingProperties();
        ZerxLoggingProperties.OpLog opLog = props.getOpLog();

        opLog.setEnabled(false);
        assertFalse(opLog.isEnabled());

        opLog.setLogToLogger(false);
        assertFalse(opLog.isLogToLogger());

        opLog.setRecordExceptionStackTrace(true);
        assertTrue(opLog.isRecordExceptionStackTrace());

        opLog.setMaxParamLength(512);
        assertEquals(512, opLog.getMaxParamLength());

        opLog.setMaxResultLength(4096);
        assertEquals(4096, opLog.getMaxResultLength());
    }

    @Test
    void set_sensitiveLog_properties() {
        ZerxLoggingProperties props = new ZerxLoggingProperties();
        ZerxLoggingProperties.SensitiveLog sensitiveLog = props.getSensitiveLog();

        sensitiveLog.setEnabled(true);
        assertTrue(sensitiveLog.isEnabled());

        sensitiveLog.setFilterMobile(false);
        assertFalse(sensitiveLog.isFilterMobile());

        sensitiveLog.setFilterEmail(false);
        assertFalse(sensitiveLog.isFilterEmail());

        sensitiveLog.setFilterIdCard(false);
        assertFalse(sensitiveLog.isFilterIdCard());

        sensitiveLog.setFilterBankCard(false);
        assertFalse(sensitiveLog.isFilterBankCard());

        sensitiveLog.setFilterPassword(false);
        assertFalse(sensitiveLog.isFilterPassword());

        sensitiveLog.setFilterIpv4(true);
        assertTrue(sensitiveLog.isFilterIpv4());
    }

    @Test
    void set_rateLimit_properties() {
        ZerxLoggingProperties props = new ZerxLoggingProperties();
        ZerxLoggingProperties.RateLimit rateLimit = props.getRateLimit();

        rateLimit.setEnabled(true);
        assertTrue(rateLimit.isEnabled());

        rateLimit.setMaxPerSecond(50);
        assertEquals(50, rateLimit.getMaxPerSecond());
    }

    @Test
    void set_jsonFormat_properties() {
        ZerxLoggingProperties props = new ZerxLoggingProperties();
        ZerxLoggingProperties.JsonFormat jsonFormat = props.getJsonFormat();

        jsonFormat.setEnabled(true);
        assertTrue(jsonFormat.isEnabled());

        jsonFormat.setIncludeMdc(false);
        assertFalse(jsonFormat.isIncludeMdc());

        jsonFormat.setIncludeLoggerName(false);
        assertFalse(jsonFormat.isIncludeLoggerName());

        jsonFormat.setIncludeThreadName(true);
        assertTrue(jsonFormat.isIncludeThreadName());

        jsonFormat.setPrettyPrint(true);
        assertTrue(jsonFormat.isPrettyPrint());
    }

    @Test
    void custom_nested_instances() {
        ZerxLoggingProperties props = new ZerxLoggingProperties();

        ZerxLoggingProperties.OpLog customOpLog = new ZerxLoggingProperties.OpLog();
        customOpLog.setEnabled(false);
        props.setOpLog(customOpLog);
        assertFalse(props.getOpLog().isEnabled());

        ZerxLoggingProperties.SensitiveLog customSensitive = new ZerxLoggingProperties.SensitiveLog();
        customSensitive.setEnabled(true);
        props.setSensitiveLog(customSensitive);
        assertTrue(props.getSensitiveLog().isEnabled());

        ZerxLoggingProperties.RateLimit customRate = new ZerxLoggingProperties.RateLimit();
        customRate.setEnabled(true);
        props.setRateLimit(customRate);
        assertTrue(props.getRateLimit().isEnabled());

        ZerxLoggingProperties.JsonFormat customJson = new ZerxLoggingProperties.JsonFormat();
        customJson.setEnabled(true);
        props.setJsonFormat(customJson);
        assertTrue(props.getJsonFormat().isEnabled());
    }
}
