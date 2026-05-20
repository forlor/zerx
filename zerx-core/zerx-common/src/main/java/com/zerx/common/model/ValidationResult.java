package com.zerx.common.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import com.zerx.common.exception.ValidationException;

/**
 * 统一校验结果封装
 * <p>
 * 提供累积式参数校验能力，支持在一次校验过程中收集所有错误信息，
 * 而非遇到第一个错误就抛出异常。适用于表单提交、批量导入等需要一次性返回所有校验错误的场景。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 基本用法
 * ValidationResult result = ValidationResult.create();
 * result.check(name != null, "姓名不能为空");
 * result.check(age > 0, "年龄必须大于0");
 * result.check(StringUtil.isMobile(phone), "手机号格式不正确");
 *
 * if (result.hasErrors()) {
 *     return Result.fail("30001", result.joinErrors("; "));
 * }
 *
 * // 链式用法
 * ValidationResult result = ValidationResult.create()
 *     .check(name != null, "姓名不能为空")
 *     .check(age > 0, "年龄必须大于0")
 *     .check(StringUtil.isMobile(phone), "手机号格式不正确");
 *
 * // 条件分组校验
 * ValidationResult result = ValidationResult.create();
 * result.check(name != null, "姓名不能为空");
 * if (name != null) {
 *     result.check(name.length() <= 50, "姓名不能超过50个字符");
 * }
 *
 * // 与 Result 配合
 * if (result.hasErrors()) {
 *     return Result.fail(result.toResult());
 * }
 * }</pre>
 *
 * @author zerx
 */
public class ValidationResult implements Serializable {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /** 错误信息列表 */
    private final List<String> errors;

    /** 私有构造器，使用工厂方法创建 */
    private ValidationResult() {
        this.errors = new ArrayList<>();
    }

    // ======================== 工厂方法 ========================

    /**
     * 创建一个空的校验结果
     *
     * @return ValidationResult 实例
     */
    public static ValidationResult create() {
        return new ValidationResult();
    }

    // ======================== 校验方法 ========================

    /**
     * 添加一条校验：条件不满足时记录错误信息
     *
     * @param condition 校验条件，true 表示通过，false 表示失败
     * @param message   校验失败时的错误信息
     * @return 当前实例（支持链式调用）
     */
    public ValidationResult check(boolean condition, String message) {
        if (!condition) {
            Objects.requireNonNull(message, "错误信息不能为 null");
            errors.add(message);
        }
        return this;
    }

    /**
     * 添加一条校验：对象不为 null
     *
     * @param obj     待校验对象
     * @param message 校验失败时的错误信息
     * @return 当前实例
     */
    public ValidationResult notNull(Object obj, String message) {
        return check(obj != null, message);
    }

    /**
     * 添加一条校验：字符串不为空白
     *
     * @param str     待校验字符串
     * @param message 校验失败时的错误信息
     * @return 当前实例
     */
    public ValidationResult notBlank(String str, String message) {
        return check(str != null && !str.trim().isEmpty(), message);
    }

    /**
     * 添加一条校验：字符串不为空
     *
     * @param str     待校验字符串
     * @param message 校验失败时的错误信息
     * @return 当前实例
     */
    public ValidationResult notEmpty(String str, String message) {
        return check(str != null && !str.isEmpty(), message);
    }

    /**
     * 添加一条校验：集合不为空
     *
     * @param collection 待校验集合
     * @param message    校验失败时的错误信息
     * @param <T>        元素类型
     * @return 当前实例
     */
    public <T> ValidationResult notEmpty(java.util.Collection<T> collection, String message) {
        return check(collection != null && !collection.isEmpty(), message);
    }

    /**
     * 添加一条校验：数组不为空
     *
     * @param array   待校验数组
     * @param message 校验失败时的错误信息
     * @param <T>     元素类型
     * @return 当前实例
     */
    public <T> ValidationResult notEmpty(T[] array, String message) {
        return check(array != null && array.length > 0, message);
    }

    /**
     * 添加一条校验：数值大于指定值
     *
     * @param value   待校验数值
     * @param min     最小值（不包含）
     * @param message 校验失败时的错误信息
     * @return 当前实例
     */
    public ValidationResult gt(Number value, Number min, String message) {
        return check(value != null && value.doubleValue() > min.doubleValue(), message);
    }

    /**
     * 添加一条校验：数值大于等于指定值
     *
     * @param value   待校验数值
     * @param min     最小值（包含）
     * @param message 校验失败时的错误信息
     * @return 当前实例
     */
    public ValidationResult ge(Number value, Number min, String message) {
        return check(value != null && value.doubleValue() >= min.doubleValue(), message);
    }

    /**
     * 添加一条校验：数值小于指定值
     *
     * @param value   待校验数值
     * @param max     最大值（不包含）
     * @param message 校验失败时的错误信息
     * @return 当前实例
     */
    public ValidationResult lt(Number value, Number max, String message) {
        return check(value != null && value.doubleValue() < max.doubleValue(), message);
    }

    /**
     * 添加一条校验：数值在指定范围内
     *
     * @param value   待校验数值
     * @param min     最小值（包含）
     * @param max     最大值（包含）
     * @param message 校验失败时的错误信息
     * @return 当前实例
     */
    public ValidationResult between(Number value, Number min, Number max, String message) {
        return check(value != null
                        && value.doubleValue() >= min.doubleValue()
                        && value.doubleValue() <= max.doubleValue(),
                message);
    }

    /**
     * 添加一条校验：字符串长度在指定范围内
     *
     * @param str     待校验字符串
     * @param minLen  最小长度（包含）
     * @param maxLen  最大长度（包含）
     * @param message 校验失败时的错误信息
     * @return 当前实例
     */
    public ValidationResult length(String str, int minLen, int maxLen, String message) {
        return check(str != null && str.length() >= minLen && str.length() <= maxLen, message);
    }

    /**
     * 添加一条校验：正则匹配
     *
     * @param str     待校验字符串
     * @param regex   正则表达式
     * @param message 校验失败时的错误信息
     * @return 当前实例
     */
    public ValidationResult matches(String str, String regex, String message) {
        return check(str != null && str.matches(regex), message);
    }

    /**
     * 合并另一个 ValidationResult 的错误信息
     *
     * @param other 另一个校验结果
     * @return 当前实例
     */
    public ValidationResult merge(ValidationResult other) {
        if (other != null && other.hasErrors()) {
            this.errors.addAll(other.errors);
        }
        return this;
    }

    /**
     * 嵌套校验：对子对象执行校验并合并结果
     *
     * @param supplier 子校验的 Supplier
     * @return 当前实例
     */
    public ValidationResult nested(Supplier<ValidationResult> supplier) {
        if (supplier != null) {
            ValidationResult child = supplier.get();
            if (child != null) {
                merge(child);
            }
        }
        return this;
    }

    // ======================== 结果查询 ========================

    /**
     * 是否存在校验错误
     *
     * @return 有错误返回 true
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * 是否校验通过（无错误）
     *
     * @return 通过返回 true
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /**
     * 获取错误数量
     *
     * @return 错误条数
     */
    public int errorCount() {
        return errors.size();
    }

    /**
     * 获取第一条错误信息
     *
     * @return 第一条错误，无错误返回 null
     */
    public String firstError() {
        return errors.isEmpty() ? null : errors.getFirst();
    }

    /**
     * 获取所有错误信息（不可变列表）
     *
     * @return 错误信息列表
     */
    public List<String> errors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * 将所有错误信息用分隔符拼接为一个字符串
     *
     * @param separator 分隔符
     * @return 拼接后的错误字符串，无错误返回空字符串
     */
    public String joinErrors(String separator) {
        if (errors.isEmpty()) {
            return "";
        }
        return String.join(separator, errors);
    }

    // ======================== 与 Result 集成 ========================

    /**
     * 转换为失败的 Result
     * <p>
     * 将所有校验错误合并为一条消息，包装为失败响应。
     * 如果无错误，返回成功响应。
     * </p>
     *
     * @param <T> 数据类型
     * @return Result 实例
     */
    public <T> Result<T> toResult() {
        if (isValid()) {
            return Result.ok();
        }
        return Result.fail("30001", joinErrors("; "));
    }

    /**
     * 如果存在错误则抛出 ValidationException
     *
     * @throws ValidationException 有校验错误时抛出
     */
    public void throwIfInvalid() {
        if (hasErrors()) {
            throw new ValidationException(
                    com.zerx.common.exception.ErrorCode.PARAM_FORMAT_ERROR, joinErrors("; "));
        }
    }

    // ======================== Object 方法 ========================

    @Override
    public String toString() {
        return isValid() ? "ValidationResult{valid=true}" :
                "ValidationResult{valid=false, errors=" + errors + "}";
    }
}
