package com.zerx.common.util;

import com.zerx.common.exception.BusinessException;
import com.zerx.common.exception.ErrorCode;
import com.zerx.common.exception.ZerxException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 异常工具类
 * <p>
 * 提供异常信息提取、异常链遍历、堆栈跟踪等便捷方法，
 * 便于在日志记录、错误响应等场景中获取结构化的异常信息。
 * </p>
 *
 * @author zerx
 */
public final class ExceptionUtil {

    /** 默认堆栈跟踪深度 */
    private static final int DEFAULT_STACK_DEPTH = 32;

    /** 私有构造器，防止实例化 */
    private ExceptionUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ======================== 异常信息提取 ========================

    /**
     * 获取异常的完整堆栈信息字符串
     *
     * @param throwable 异常对象
     * @return 堆栈信息字符串，null 返回空字符串
     */
    public static String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        StringWriter sw = new StringWriter(DEFAULT_STACK_DEPTH * 64);
        try (PrintWriter pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
        }
        return sw.toString();
    }

    /**
     * 获取异常的简洁描述（异常类名 + 消息）
     *
     * @param throwable 异常对象
     * @return 简洁描述，null 返回 "null"
     */
    public static String getSimpleMessage(Throwable throwable) {
        if (throwable == null) {
            return "null";
        }
        String className = throwable.getClass().getSimpleName();
        String message = throwable.getMessage();
        return StringUtil.isBlank(message) ? className : className + ": " + message;
    }

    /**
     * 获取异常根原因（异常链的最深层 cause）
     *
     * @param throwable 异常对象
     * @return 根原因异常，无 cause 时返回自身
     */
    public static Throwable getRootCause(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    /**
     * 获取异常链中指定类型的异常
     *
     * @param throwable 异常对象
     * @param targetType 目标异常类型
     * @param <T>       目标异常类型
     * @return 目标异常实例，未找到返回 null
     */
    @SuppressWarnings("unchecked")
    public static <T extends Throwable> T findCause(Throwable throwable, Class<T> targetType) {
        if (throwable == null || targetType == null) {
            return null;
        }
        Throwable current = throwable;
        while (current != null) {
            if (targetType.isInstance(current)) {
                return (T) current;
            }
            current = current.getCause();
        }
        return null;
    }

    /**
     * 判断异常链中是否包含指定类型的异常
     *
     * @param throwable 异常对象
     * @param targetType 目标异常类型
     * @return 包含返回 true
     */
    public static boolean containsCause(Throwable throwable, Class<? extends Throwable> targetType) {
        return findCause(throwable, targetType) != null;
    }

    // ======================== Zerx 异常相关 ========================

    /**
     * 判断异常是否为 Zerx 体系内的异常
     *
     * @param throwable 异常对象
     * @return 是 ZerxException 返回 true
     */
    public static boolean isZerxException(Throwable throwable) {
        return throwable instanceof ZerxException;
    }

    /**
     * 从异常中提取错误码
     * <p>
     * 如果是 ZerxException，返回其 ErrorCode 的 code；
     * 如果是 IllegalArgumentException，返回参数校验错误码；
     * 其他异常返回系统错误码。
     * </p>
     *
     * @param throwable 异常对象
     * @return 错误码字符串
     */
    public static String extractCode(Throwable throwable) {
        if (throwable instanceof ZerxException zerxEx) {
            return zerxEx.getCode();
        }
        if (throwable instanceof IllegalArgumentException) {
            return ErrorCode.PARAM_INVALID.code();
        }
        return ErrorCode.SYSTEM_ERROR.code();
    }

    /**
     * 从异常中提取 HTTP 状态码
     *
     * @param throwable 异常对象
     * @return HTTP 状态码
     */
    public static int extractHttpStatus(Throwable throwable) {
        if (throwable instanceof ZerxException zerxEx) {
            return zerxEx.getHttpStatus();
        }
        if (throwable instanceof IllegalArgumentException) {
            return 400;
        }
        return 500;
    }

    /**
     * 从异常中提取错误消息
     * <p>
     * 优先使用异常的 getMessage()，如果为空则使用异常类名的简单名称。
     * </p>
     *
     * @param throwable 异常对象
     * @return 错误消息
     */
    public static String extractMessage(Throwable throwable) {
        if (throwable == null) {
            return "未知错误";
        }
        String message = throwable.getMessage();
        return StringUtil.isNotBlank(message) ? message : throwable.getClass().getSimpleName();
    }

    // ======================== 异常链操作 ========================

    /**
     * 获取异常链中所有异常的列表（从外到内）
     *
     * @param throwable 异常对象
     * @return 异常链列表
     */
    public static List<Throwable> getExceptionChain(Throwable throwable) {
        if (throwable == null) {
            return List.of();
        }
        List<Throwable> chain = new ArrayList<>();
        Throwable current = throwable;
        while (current != null) {
            chain.add(current);
            current = current.getCause();
            // 防止循环引用
            if (chain.contains(current)) {
                break;
            }
        }
        return List.copyOf(chain);
    }

    /**
     * 获取异常链的长度
     *
     * @param throwable 异常对象
     * @return 异常链深度
     */
    public static int getExceptionChainDepth(Throwable throwable) {
        return getExceptionChain(throwable).size();
    }

    /**
     * 将异常链的所有消息合并为一个字符串
     *
     * @param throwable 异常对象
     * @param separator 分隔符
     * @return 合并后的消息字符串
     */
    public static String joinExceptionChainMessages(Throwable throwable, String separator) {
        return getExceptionChain(throwable).stream()
                .map(ExceptionUtil::extractMessage)
                .collect(java.util.stream.Collectors.joining(separator));
    }
}
