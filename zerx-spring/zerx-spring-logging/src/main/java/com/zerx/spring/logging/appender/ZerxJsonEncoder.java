package com.zerx.spring.logging.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.EncoderBase;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON 日志 Encoder — 将 Logback 日志事件序列化为 JSON 格式。
 * <p>
 * 输出字段：timestamp / level / logger / thread / message / mdc（含 traceId 等）。
 * 便于 ELK / Loki 等日志采集系统直接解析。
 * </p>
 *
 * <h3>Logback 配置示例：</h3>
 * <pre>{@code
 * <encoder class="com.zerx.spring.logging.appender.ZerxJsonEncoder">
 *     <includeMdc>true</includeMdc>
 *     <includeLoggerName>true</includeLoggerName>
 *     <includeThreadName>true</includeThreadName>
 *     <prettyPrint>false</prettyPrint>
 * </encoder>
 * }</pre>
 *
 * @author zerx
 */
public class ZerxJsonEncoder extends EncoderBase<ILoggingEvent> {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                    .withZone(ZoneId.systemDefault());

    private boolean includeMdc = true;
    private boolean includeLoggerName = true;
    private boolean includeThreadName = true;
    private boolean prettyPrint = false;

    @Override
    public byte[] headerBytes() {
        return null;
    }

    @Override
    public byte[] footerBytes() {
        return null;
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("timestamp", TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(event.getTimeStamp())));
        json.put("level", event.getLevel().toString());

        if (includeLoggerName) {
            json.put("logger", event.getLoggerName());
        }
        if (includeThreadName) {
            json.put("thread", event.getThreadName());
        }

        json.put("message", event.getFormattedMessage());

        if (includeMdc && event.getMDCPropertyMap() != null && !event.getMDCPropertyMap().isEmpty()) {
            json.put("mdc", event.getMDCPropertyMap());
        }

        if (event.getThrowableProxy() != null) {
            json.put("exception", event.getThrowableProxy().getMessage());
            json.put("exceptionClass", event.getThrowableProxy().getClassName());
        }

        byte[] bytes = toJsonBytes(json);
        // Logback 期望每条日志一行
        byte[] line = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, line, 0, bytes.length);
        line[bytes.length] = '\n';
        return line;
    }

    private byte[] toJsonBytes(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder(256);
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                sb.append(',');
                if (prettyPrint) sb.append(' ');
            }
            first = false;
            sb.append('"').append(escapeJson(entry.getKey())).append('"').append(':');
            appendValue(sb, entry.getValue());
        }
        sb.append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void appendValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value);
        } else if (value instanceof Map) {
            sb.append(toJsonString((Map<String, Object>) value));
        } else {
            sb.append('"').append(escapeJson(value.toString())).append('"');
        }
    }

    private String toJsonString(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder(128);
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(escapeJson(entry.getKey())).append('"').append(':');
            appendValue(sb, entry.getValue());
        }
        sb.append('}');
        return sb.toString();
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        StringBuilder sb = null;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            String replacement = switch (c) {
                case '"' -> "\\\"";
                case '\\' -> "\\\\";
                case '\n' -> "\\n";
                case '\r' -> "\\r";
                case '\t' -> "\\t";
                default -> null;
            };
            if (replacement != null) {
                if (sb == null) {
                    sb = new StringBuilder(value.length() + 16);
                    sb.append(value, 0, i);
                }
                sb.append(replacement);
            } else if (sb != null) {
                sb.append(c);
            }
        }
        return sb != null ? sb.toString() : value;
    }

    // ======================== Logback setter（通过 XML 配置注入） ========================

    public void setIncludeMdc(boolean includeMdc) {
        this.includeMdc = includeMdc;
    }

    public void setIncludeLoggerName(boolean includeLoggerName) {
        this.includeLoggerName = includeLoggerName;
    }

    public void setIncludeThreadName(boolean includeThreadName) {
        this.includeThreadName = includeThreadName;
    }

    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }
}
