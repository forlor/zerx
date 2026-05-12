package com.zerx.common.util;

/**
 * 数据脱敏工具类
 * <p>
 * 提供常见敏感数据的脱敏处理方法，适用于日志输出、列表展示、数据导出等场景。
 * 所有方法均为纯函数，不修改原始数据，返回脱敏后的新字符串。
 * </p>
 *
 * <h3>支持的脱敏类型：</h3>
 * <ul>
 *   <li>手机号：138****5678</li>
 *   <li>固定电话：0755****1234</li>
 *   <li>邮箱：t***t@example.com</li>
 *   <li>身份证号：110***********5678</li>
 *   <li>银行卡号：6222 **** **** 5678</li>
 *   <li>姓名：张*、欧阳**</li>
 *   <li>地址：北京市朝阳区****</li>
 *   <li>密码：******
 *   <li>IPv4：192.168.*.*</li>
 *   <li>自定义脱敏：按指定规则脱敏任意字符串</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 手机号脱敏
 * String phone = SensitiveDataUtil.maskMobile("13812345678");  // "138****5678"
 *
 * // 身份证号脱敏
 * String idCard = SensitiveDataUtil.maskIdCard("110101199001011234");  // "110***********1234"
 *
 * // 自定义脱敏
 * String custom = SensitiveDataUtil.mask("hello", 2, 1, '*');  // "he***o"
 * }</pre>
 *
 * @author zerx
 */
public final class SensitiveDataUtil {

    /** 默认脱敏字符 */
    public static final char DEFAULT_MASK_CHAR = '*';

    /** 手机号前保留位数 */
    private static final int MOBILE_PREFIX_LEN = 3;

    /** 手机号后保留位数 */
    private static final int MOBILE_SUFFIX_LEN = 4;

    /** 邮箱用户名最少显示字符 */
    private static final int EMAIL_MIN_VISIBLE = 1;

    /** 身份证号前保留位数 */
    private static final int IDCARD_PREFIX_LEN = 3;

    /** 身份证号后保留位数 */
    private static final int IDCARD_SUFFIX_LEN = 4;

    /** 银行卡号前保留位数 */
    private static final int BANKCARD_PREFIX_LEN = 4;

    /** 银行卡号后保留位数 */
    private static final int BANKCARD_SUFFIX_LEN = 4;

    /** 姓名最多保留位数 */
    private static final int NAME_MAX_VISIBLE = 1;

    /** 密码显示长度 */
    private static final int PASSWORD_MASK_LEN = 6;

    /** 私有构造器，防止实例化 */
    private SensitiveDataUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ======================== 通用脱敏 ========================

    /**
     * 通用脱敏方法
     * <p>
     * 对字符串中间部分进行脱敏，保留前后指定位数的可见字符。
     * </p>
     *
     * @param str       原始字符串
     * @param prefixLen 前保留位数（从头开始）
     * @param suffixLen 后保留位数（从尾开始）
     * @param maskChar  脱敏字符
     * @return 脱敏后的字符串，null 或空字符串返回原值
     */
    public static String mask(String str, int prefixLen, int suffixLen, char maskChar) {
        if (StringUtil.isBlank(str)) {
            return str;
        }
        int len = str.length();
        // 保留位数之和大于等于总长度时不脱敏
        if (prefixLen + suffixLen >= len) {
            return str;
        }
        int maskLen = len - prefixLen - suffixLen;
        return str.substring(0, prefixLen)
                + String.valueOf(maskChar).repeat(maskLen)
                + str.substring(len - suffixLen);
    }

    /**
     * 通用脱敏方法（使用默认脱敏字符 *）
     *
     * @param str       原始字符串
     * @param prefixLen 前保留位数
     * @param suffixLen 后保留位数
     * @return 脱敏后的字符串
     */
    public static String mask(String str, int prefixLen, int suffixLen) {
        return mask(str, prefixLen, suffixLen, DEFAULT_MASK_CHAR);
    }

    /**
     * 仅保留指定位数，其余全部脱敏
     * <p>
     * 示例：maskKeep("123456", 2, '*') → "12****"
     * </p>
     *
     * @param str      原始字符串
     * @param keepLen  保留前 N 位
     * @param maskChar 脱敏字符
     * @return 脱敏后的字符串
     */
    public static String maskKeep(String str, int keepLen, char maskChar) {
        return mask(str, keepLen, 0, maskChar);
    }

    /**
     * 全部脱敏
     * <p>
     * 示例：maskAll("secret") → "******"
     * </p>
     *
     * @param str      原始字符串
     * @param maskChar 脱敏字符
     * @return 全部脱敏后的字符串
     */
    public static String maskAll(String str, char maskChar) {
        if (StringUtil.isBlank(str)) {
            return str;
        }
        return String.valueOf(maskChar).repeat(str.length());
    }

    // ======================== 手机号 ========================

    /**
     * 手机号脱敏
     * <p>
     * 保留前 3 位和后 4 位，中间 4 位脱敏。
     * 示例：13812345678 → 138****5678
     * </p>
     *
     * @param mobile 手机号
     * @return 脱敏后的手机号
     */
    public static String maskMobile(String mobile) {
        return mask(mobile, MOBILE_PREFIX_LEN, MOBILE_SUFFIX_LEN, DEFAULT_MASK_CHAR);
    }

    /**
     * 手机号脱敏（自定义脱敏字符）
     *
     * @param mobile   手机号
     * @param maskChar 脱敏字符
     * @return 脱敏后的手机号
     */
    public static String maskMobile(String mobile, char maskChar) {
        return mask(mobile, MOBILE_PREFIX_LEN, MOBILE_SUFFIX_LEN, maskChar);
    }

    // ======================== 固定电话 ========================

    /**
     * 固定电话脱敏
     * <p>
     * 保留前 4 位（区号）和后 4 位，中间部分脱敏。
     * 示例：075512345678 → 0755****5678
     * </p>
     *
     * @param phone 固定电话号码
     * @return 脱敏后的电话号码
     */
    public static String maskPhone(String phone) {
        return mask(phone, 4, 4, DEFAULT_MASK_CHAR);
    }

    // ======================== 邮箱 ========================

    /**
     * 邮箱地址脱敏
     * <p>
     * 保留用户名首字符和域名，中间部分脱敏。
     * 示例：testuser@example.com → t******r@example.com
     * 对于用户名很短的邮箱：ab@example.com → a*@example.com
     * </p>
     *
     * @param email 邮箱地址
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String email) {
        if (StringUtil.isBlank(email)) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            // 非 @ 分隔，按通用规则处理
            return mask(email, 1, 0);
        }
        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (username.length() <= EMAIL_MIN_VISIBLE) {
            // 用户名只有 1 个字符
            return username.charAt(0) + "***" + domain;
        }

        // 保留首尾字符
        String masked = String.valueOf(username.charAt(0))
                + String.valueOf(DEFAULT_MASK_CHAR).repeat(username.length() - 2)
                + username.charAt(username.length() - 1);
        return masked + domain;
    }

    // ======================== 身份证号 ========================

    /**
     * 身份证号脱敏
     * <p>
     * 保留前 3 位和后 4 位，中间部分脱敏。
     * 示例：110101199001011234 → 110***********1234
     * </p>
     *
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    public static String maskIdCard(String idCard) {
        return mask(idCard, IDCARD_PREFIX_LEN, IDCARD_SUFFIX_LEN, DEFAULT_MASK_CHAR);
    }

    // ======================== 银行卡号 ========================

    /**
     * 银行卡号脱敏（无空格）
     * <p>
     * 保留前 4 位和后 4 位，中间部分脱敏。
     * 示例：6222021234567890123 → 6222***********0123
     * </p>
     *
     * @param bankCard 银行卡号
     * @return 脱敏后的银行卡号
     */
    public static String maskBankCard(String bankCard) {
        return mask(bankCard, BANKCARD_PREFIX_LEN, BANKCARD_SUFFIX_LEN, DEFAULT_MASK_CHAR);
    }

    /**
     * 银行卡号脱敏（每 4 位一组，带空格格式）
     * <p>
     * 保留前 4 位和后 4 位，中间按 4 位分组脱敏。
     * 示例：6222021234567890123 → 6222 **** **** 0123
     * </p>
     *
     * @param bankCard 银行卡号
     * @return 格式化脱敏后的银行卡号
     */
    public static String maskBankCardFormatted(String bankCard) {
        if (StringUtil.isBlank(bankCard)) {
            return bankCard;
        }
        // 先去除已有空格
        String clean = bankCard.replace(" ", "");
        String masked = maskBankCard(clean);
        // 每 4 位加空格
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < masked.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                sb.append(' ');
            }
            sb.append(masked.charAt(i));
        }
        return sb.toString();
    }

    // ======================== 姓名 ========================

    /**
     * 姓名脱敏
     * <p>
     * 保留姓氏（第一个字符），其余部分脱敏。
     * 复姓也只保留第一个字符。
     * 示例：张三 → 张*，欧阳锋 → 欧**
     * </p>
     *
     * @param name 姓名
     * @return 脱敏后的姓名
     */
    public static String maskName(String name) {
        if (StringUtil.isBlank(name)) {
            return name;
        }
        int len = name.length();
        if (len <= NAME_MAX_VISIBLE) {
            return String.valueOf(name.charAt(0)) + DEFAULT_MASK_CHAR;
        }
        return name.charAt(0)
                + String.valueOf(DEFAULT_MASK_CHAR).repeat(len - NAME_MAX_VISIBLE);
    }

    // ======================== 地址 ========================

    /**
     * 地址脱敏
     * <p>
     * 保留前半部分，后面部分脱敏。
     * 示例：北京市朝阳区建国路88号 → 北京市朝阳区****
     * </p>
     *
     * @param address 地址
     * @return 脱敏后的地址
     */
    public static String maskAddress(String address) {
        if (StringUtil.isBlank(address)) {
            return address;
        }
        int len = address.length();
        // 保留前 60% 的字符
        int keepLen = Math.max((int) Math.ceil(len * 0.6), 2);
        // 脱敏部分不超过 4 个字符
        int maskLen = Math.min(len - keepLen, 4);
        if (maskLen <= 0) {
            return address;
        }
        return address.substring(0, keepLen)
                + String.valueOf(DEFAULT_MASK_CHAR).repeat(maskLen);
    }

    // ======================== 密码 ========================

    /**
     * 密码脱敏
     * <p>
     * 无论密码长度如何，统一显示 6 个脱敏字符。
     * 示例：mypassword → ******
     * </p>
     *
     * @param password 密码
     * @return 脱敏后的密码
     */
    public static String maskPassword(String password) {
        if (StringUtil.isBlank(password)) {
            return password;
        }
        return String.valueOf(DEFAULT_MASK_CHAR).repeat(PASSWORD_MASK_LEN);
    }

    // ======================== IP 地址 ========================

    /**
     * IPv4 地址脱敏
     * <p>
     * 保留前两段，后两段脱敏。
     * 示例：192.168.1.100 → 192.168.*.*
     * </p>
     *
     * @param ipv4 IPv4 地址
     * @return 脱敏后的 IPv4
     */
    public static String maskIpv4(String ipv4) {
        if (StringUtil.isBlank(ipv4)) {
            return ipv4;
        }
        String[] parts = ipv4.split("\\.");
        if (parts.length != 4) {
            return mask(ipv4, 4, 0);
        }
        return parts[0] + "." + parts[1] + ".*.*";
    }

    /**
     * IPv6 地址脱敏
     * <p>
     * 保留前两段，后四段脱敏。
     * 示例：2001:0db8:85a3:0000:0000:8a2e:0370:7334 → 2001:0db8:****:****
     * </p>
     *
     * @param ipv6 IPv6 地址
     * @return 脱敏后的 IPv6
     */
    public static String maskIpv6(String ipv6) {
        if (StringUtil.isBlank(ipv6)) {
            return ipv6;
        }
        String[] parts = ipv6.split(":");
        if (parts.length <= 2) {
            return mask(ipv6, 4, 0);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(parts[0]).append(":").append(parts[1]);
        for (int i = 2; i < parts.length; i++) {
            sb.append(":****");
        }
        return sb.toString();
    }

    // ======================== 车牌号 ========================

    /**
     * 车牌号脱敏
     * <p>
     * 保留省份简称和后两位，中间部分脱敏。
     * 示例：京A12345 → 京A***45
     * </p>
     *
     * @param plateNumber 车牌号
     * @return 脱敏后的车牌号
     */
    public static String maskPlateNumber(String plateNumber) {
        if (StringUtil.isBlank(plateNumber)) {
            return plateNumber;
        }
        // 新能源车牌 8 位，普通车牌 7 位
        int len = plateNumber.length();
        if (len <= 4) {
            return mask(plateNumber, 2, 0);
        }
        return plateNumber.substring(0, 2)
                + String.valueOf(DEFAULT_MASK_CHAR).repeat(len - 4)
                + plateNumber.substring(len - 2);
    }

    // ======================== key / token ========================

    /**
     * API Key / Token 脱敏
     * <p>
     * 保留前 4 位和后 4 位，中间全部脱敏。
     * 示例：abcdefghijklmnopqrstuvwxyz → abcd****************yz
     * </p>
     *
     * @param token API Key 或 Token
     * @return 脱敏后的 Token
     */
    public static String maskToken(String token) {
        return mask(token, 4, 4, DEFAULT_MASK_CHAR);
    }
}
