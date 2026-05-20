package com.zerx.common.logging;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.zerx.common.util.ExceptionUtil;

/**
 * 日志异常格式化工具
 * <p>
 * 将异常信息格式化为结构化的日志文本，确保异常日志包含完整的 cause chain、
 * 错误码和业务上下文。与 SLF4J 等日志框架完全解耦，仅负责"格式化"，
 * 输出的字符串可直接传入任意日志框架的日志方法。
 * </p>
 *
 * <h3>核心能力：</h3>
 * <ul>
 *   <li>完整异常链格式化：逐层输出 cause chain，避免只打印表层异常</li>
 *   <li>ErrorCode 集成：自动提取 ZerxException 中的错误码，输出为前缀</li>
 *   <li>业务上下文附加：支持附加自定义 KV 上下文信息</li>
 *   <li>简洁模式：仅输出异常类名和消息，适用于轻量场景</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 基本格式化
 * String log = LogHelper.formatException(exception);
 *
 * // 带业务上下文
 * String log = LogHelper.formatException(exception,
 *     "orderId", "202605140001",
 *     "userId", "10086"
 * );
 *
 * // 简洁模式（仅类名 + 消息）
 * String brief = LogHelper.formatBrief(exception);
 * }</pre>
 *
 * @author zerx
 */
public final class LogHelper {

    /** 异常链缩进前缀 */
    private static final String CAUSE_PREFIX = "  \u2514\u2500 ";

    /** 上下文标签 */
    private static final String CONTEXT_LABEL = "Context";

    /** 分隔线 */
    private static final String SEPARATOR = " | ";

    private LogHelper() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ======================== 完整格式化 ========================

    /**
     * 格式化异常为结构化日志文本
     * <p>
     * 输出格式：
     * <pre>
     * [错误码:消息] 异常顶层描述
     *   └─ Caused by: [异常类名] 异常消息
     *      └─ Caused by: [异常类名] 异常消息
     * </pre>
     * </p>
     *
     * @param throwable 异常对象
     * @return 格式化后的日志文本，null 返回 "null"
     */
    public static String formatException(Throwable throwable) {
        return formatException(throwable, Map.of());
    }

    /**
     * 格式化异常为结构化日志文本（带业务上下文）
     * <p>
     * 上下文参数以 key-value 交替形式传入，必须为偶数个参数。
     * </p>
     *
     * @param throwable      异常对象
     * @param contextKeyValues 上下文键值对（key1, value1, key2, value2, ...）
     * @return 格式化后的日志文本
     * @throws IllegalArgumentException 如果上下文参数个数为奇数
     */
    public static String formatException(Throwable throwable, Object... contextKeyValues) {
        return formatException(throwable, toMap(contextKeyValues));
    }

    /**
     * 格式化异常为结构化日志文本（带业务上下文）
     *
     * @param throwable 异常对象
     * @param context   业务上下文（key-value 映射）
     * @return 格式化后的日志文本
     */
    public static String formatException(Throwable throwable, Map<String, Object> context) {
        if (throwable == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder(512);

        // 1. 错误码前缀（自动识别 ZerxException 或其他异常）
        appendErrorCodePrefix(sb, throwable);

        // 2. 异常顶层描述
        sb.append(ExceptionUtil.getSimpleMessage(throwable));

        // 3. 业务上下文
        if (context != null && !context.isEmpty()) {
            appendContext(sb, context);
        }

        // 4. 异常链详情（仅当有多层 cause 时输出）
        appendCauseChain(sb, throwable);

        return sb.toString();
    }

    // ======================== 简洁格式化 ========================

    /**
     * 简洁格式化：仅输出异常链的类名和消息（不含堆栈和缩进）
     * <p>
     * 输出格式：顶层异常: message | caused by: 中间层: message | root: 根因: message
     * 适用于对日志体积敏感的场景，如高频日志、告警消息等。
     * </p>
     *
     * @param throwable 异常对象
     * @return 简洁格式的异常描述
     */
    public static String formatBrief(Throwable throwable) {
        if (throwable == null) {
            return "null";
        }
        List<Throwable> chain = ExceptionUtil.getExceptionChain(throwable);
        if (chain.size() == 1) {
            return ExceptionUtil.getSimpleMessage(chain.getFirst());
        }
        return chain.stream()
                .map(ExceptionUtil::getSimpleMessage)
                .collect(java.util.stream.Collectors.joining(SEPARATOR));
    }

    /**
     * 简洁格式化（带错误码前缀）
     *
     * @param throwable 异常对象
     * @return 带错误码前缀的简洁格式
     */
    public static String formatBriefWithCode(Throwable throwable) {
        if (throwable == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder(256);
        appendErrorCodePrefix(sb, throwable);
        sb.append(formatBrief(throwable));
        return sb.toString();
    }

    // ======================== 根因快速提取 ========================

    /**
     * 快速获取异常根因的描述
     * <p>
     * 遍历 cause chain，返回最深层异常的 "类名: 消息"。
     * </p>
     *
     * @param throwable 异常对象
     * @return 根因描述，null 返回 "null"
     */
    public static String rootCauseMessage(Throwable throwable) {
        if (throwable == null) {
            return "null";
        }
        Throwable root = ExceptionUtil.getRootCause(throwable);
        return ExceptionUtil.getSimpleMessage(root);
    }

    /**
     * 获取异常链的深度
     *
     * @param throwable 异常对象
     * @return 异常链深度，null 返回 0
     */
    public static int chainDepth(Throwable throwable) {
        return ExceptionUtil.getExceptionChainDepth(throwable);
    }

    // ======================== 内部方法 ========================

    /**
     * 追加错误码前缀：[错误码:消息] 形式
     */
    private static void appendErrorCodePrefix(StringBuilder sb, Throwable throwable) {
        String code = ExceptionUtil.extractCode(throwable);
        String message = ExceptionUtil.extractMessage(throwable);
        sb.append('[').append(code).append(':').append(message).append("] ");
    }

    /**
     * 追加业务上下文：换行输出 {key=value, key=value} 格式
     */
    private static void appendContext(StringBuilder sb, Map<String, Object> context) {
        sb.append('\n').append(CONTEXT_LABEL).append(": {");
        int i = 0;
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append('=');
            Object value = entry.getValue();
            if (value == null) {
                sb.append("null");
            } else if (value instanceof Throwable t) {
                // 如果值本身是异常，输出其简洁描述
                sb.append(ExceptionUtil.getSimpleMessage(t));
            } else {
                sb.append(value);
            }
            i++;
        }
        sb.append('}');
    }

    /**
     * 追加异常链详情：从第二层 cause 开始逐层缩进输出
     */
    private static void appendCauseChain(StringBuilder sb, Throwable throwable) {
        List<Throwable> chain = ExceptionUtil.getExceptionChain(throwable);
        if (chain.size() <= 1) {
            // 单层异常，不需要输出 cause chain
            return;
        }
        // 从第二层开始逐层输出
        for (int i = 1; i < chain.size(); i++) {
            Throwable cause = chain.get(i);
            sb.append('\n').append(CAUSE_PREFIX);
            if (i == 1) {
                sb.append("Caused by: ");
            }
            sb.append(ExceptionUtil.getSimpleMessage(cause));
        }
    }

    /**
     * 将交替的 key-value 参数转换为 LinkedHashMap（保持插入顺序）
     */
    private static Map<String, Object> toMap(Object... keyValues) {
        if (keyValues == null || keyValues.length == 0) {
            return Map.of();
        }
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "上下文参数必须为偶数个（key-value 交替），当前: " + keyValues.length);
        }
        Map<String, Object> map = new LinkedHashMap<>(keyValues.length / 2);
        for (int i = 0; i < keyValues.length; i += 2) {
            Object key = keyValues[i];
            if (key == null) {
                throw new IllegalArgumentException("上下文 key 不能为 null，位置: " + i);
            }
            map.put(key.toString(), keyValues[i + 1]);
        }
        return map;
    }
}
