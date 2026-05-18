package com.zerx.spring.security.service;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码工具服务
 * <p>
 * 提供 BCrypt 密码哈希和验证功能。封装 {@link PasswordEncoder}，
 * 为业务层提供简洁的密码操作 API。
 * </p>
 *
 * @author zerx
 */
public class ZerxPasswordService {

    private final PasswordEncoder passwordEncoder;

    /**
     * 构造密码服务
     *
     * @param passwordEncoder Spring Security 密码编码器
     */
    public ZerxPasswordService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 对原始密码进行 BCrypt 哈希
     *
     * @param rawPassword 原始密码
     * @return 哈希后的密码
     */
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * 验证原始密码是否匹配哈希值
     *
     * @param rawPassword     原始密码
     * @param encodedPassword 哈希后的密码
     * @return 匹配返回 {@code true}，否则返回 {@code false}
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
