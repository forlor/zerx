package com.zerx.spring.security.service;

import java.util.List;

/**
 * 密码强度校验服务接口 — 密码复杂度策略的 SPI
 * <p>
 * 业务应用实现此接口，提供自定义的密码复杂度校验规则。
 * 框架层仅定义契约，不提供默认实现（各业务对密码策略要求不同）。
 * </p>
 *
 * <h3>推荐实现参考（OWASP 基线）：</h3>
 * <ul>
 *   <li>最小长度 8 位</li>
 *   <li>至少包含大写字母、小写字母、数字</li>
 *   <li>不得与用户名相同</li>
 *   <li>不得包含常见弱密码（如 123456、password）</li>
 * </ul>
 *
 * <h3>使用方式：</h3>
 * <pre>{@code
 * @Service
 * public class ZerxDefaultPasswordValidator implements ZerxPasswordValidator {
 *     public List<String> validate(String rawPassword, String username) {
 *         List<String> errors = new ArrayList<>();
 *         if (rawPassword.length() < 8) errors.add("密码长度不能少于8位");
 *         if (!rawPassword.matches(".*[A-Z].*")) errors.add("密码必须包含大写字母");
 *         if (!rawPassword.matches(".*[a-z].*")) errors.add("密码必须包含小写字母");
 *         if (!rawPassword.matches(".*\\d.*")) errors.add("密码必须包含数字");
 *         if (rawPassword.equalsIgnoreCase(username)) errors.add("密码不能与用户名相同");
 *         return errors;
 *     }
 * }
 * }</pre>
 *
 * @author zerx
 */
public interface ZerxPasswordValidator {

    /**
     * 校验密码强度
     * <p>
     * 对原始密码进行复杂度校验，返回不满足规则的错误消息列表。
     * </p>
     *
     * @param rawPassword 原始密码（未加密）
     * @param username    用户名（用于检查密码是否与用户名相同）
     * @return 校验不通过的错误消息列表，空列表表示校验通过
     */
    List<String> validate(String rawPassword, String username);
}
