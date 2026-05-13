package com.zerx.common.util;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机数工具类
 * <p>
 * 提供随机数、随机字符串、随机字节等生成能力。
 * 普通场景使用 {@link ThreadLocalRandom}（高性能、无竞争），
 * 安全敏感场景使用 {@link SecureRandom}（密码学安全）。
 * 所有方法均为静态方法，线程安全。
 * </p>
 *
 * @author zerx
 */
public final class RandomUtil {

    /** 小写字母 */
    private static final String LOWER_ALPHA = "abcdefghijklmnopqrstuvwxyz";

    /** 大写字母 */
    private static final String UPPER_ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /** 数字字符 */
    private static final String DIGITS = "0123456789";

    /** 字母+数字混合字符集（小写） */
    private static final String ALPHANUMERIC = LOWER_ALPHA + DIGITS;

    /** 字母+数字混合字符集（大小写） */
    private static final String ALPHANUMERIC_MIXED = LOWER_ALPHA + UPPER_ALPHA + DIGITS;

    /** 十六进制字符集（小写） */
    private static final String HEX_CHARS = "0123456789abcdef";

    /** 私有构造器，防止实例化 */
    private RandomUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ======================== 随机数生成（普通） ========================

    /**
     * 生成随机的 int 值（整个 int 范围）
     *
     * @return 随机 int 值
     */
    public static int randomInt() {
        return ThreadLocalRandom.current().nextInt();
    }

    /**
     * 生成指定范围内的随机 int 值（包含 min 和 max）
     *
     * @param min 最小值（包含）
     * @param max 最大值（包含）
     * @return 范围内的随机 int 值
     * @throws IllegalArgumentException 当 min 大于 max 时抛出
     */
    public static int randomInt(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("最小值不能大于最大值: min=" + min + ", max=" + max);
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /**
     * 生成随机的 long 值（整个 long 范围）
     *
     * @return 随机 long 值
     */
    public static long randomLong() {
        return ThreadLocalRandom.current().nextLong();
    }

    /**
     * 生成指定范围内的随机 long 值（包含 min 和 max）
     *
     * @param min 最小值（包含）
     * @param max 最大值（包含）
     * @return 范围内的随机 long 值
     * @throws IllegalArgumentException 当 min 大于 max 时抛出
     */
    public static long randomLong(long min, long max) {
        if (min > max) {
            throw new IllegalArgumentException("最小值不能大于最大值: min=" + min + ", max=" + max);
        }
        return ThreadLocalRandom.current().nextLong(min, max + 1);
    }

    /**
     * 生成 0.0（包含）到 1.0（不包含）之间的随机 double 值
     *
     * @return 随机 double 值
     */
    public static double randomDouble() {
        return ThreadLocalRandom.current().nextDouble();
    }

    /**
     * 生成指定范围内的随机 double 值（包含 min，不包含 max）
     *
     * @param min 最小值（包含）
     * @param max 最大值（不包含）
     * @return 范围内的随机 double 值
     * @throws IllegalArgumentException 当 min 大于等于 max 时抛出
     */
    public static double randomDouble(double min, double max) {
        if (min >= max) {
            throw new IllegalArgumentException("最小值必须小于最大值: min=" + min + ", max=" + max);
        }
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    /**
     * 生成随机的 boolean 值
     *
     * @return 随机 boolean 值
     */
    public static boolean randomBoolean() {
        return ThreadLocalRandom.current().nextBoolean();
    }

    // ======================== 随机数生成（安全） ========================

    /**
     * 生成密码学安全的随机 int 值（整个 int 范围）
     * <p>
     * 适用于生成令牌、密钥、验证码等安全敏感场景。
     * 性能低于 {@link #randomInt()}，仅在安全需求场景使用。
     * </p>
     *
     * @return 密码学安全的随机 int 值
     */
    public static int secureRandomInt() {
        return new SecureRandom().nextInt();
    }

    /**
     * 生成指定范围内的密码学安全随机 int 值（包含 min 和 max）
     *
     * @param min 最小值（包含）
     * @param max 最大值（包含）
     * @return 范围内的密码学安全随机 int 值
     * @throws IllegalArgumentException 当 min 大于 max 时抛出
     */
    public static int secureRandomInt(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("最小值不能大于最大值: min=" + min + ", max=" + max);
        }
        return min + new SecureRandom().nextInt(max - min + 1);
    }

    /**
     * 生成指定数量的密码学安全随机字节
     *
     * @param length 字节数组长度
     * @return 随机字节数组
     */
    public static byte[] secureRandomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    // ======================== 随机字符串 ========================

    /**
     * 生成指定长度的随机字母数字字符串（小写字母 + 数字）
     * <p>
     * 示例：randomString(8) → "a3f8k2m1"
     * </p>
     *
     * @param length 字符串长度
     * @return 随机字符串
     */
    public static String randomString(int length) {
        return randomString(ALPHANUMERIC, length);
    }

    /**
     * 生成指定长度的随机字母数字字符串（大小写字母 + 数字）
     * <p>
     * 示例：randomStringMixed(8) → "Kx3fA8q2"
     * </p>
     *
     * @param length 字符串长度
     * @return 随机字符串
     */
    public static String randomStringMixed(int length) {
        return randomString(ALPHANUMERIC_MIXED, length);
    }

    /**
     * 生成指定长度的纯数字随机字符串
     * <p>
     * 示例：randomNumeric(6) → "382951"
     * </p>
     *
     * @param length 字符串长度
     * @return 随机数字字符串
     */
    public static String randomNumeric(int length) {
        return randomString(DIGITS, length);
    }

    /**
     * 生成指定长度的纯字母随机字符串（大小写混合）
     * <p>
     * 示例：randomAlpha(6) → "KxAmPq"
     * </p>
     *
     * @param length 字符串长度
     * @return 随机字母字符串
     */
    public static String randomAlpha(int length) {
        return randomString(LOWER_ALPHA + UPPER_ALPHA, length);
    }

    /**
     * 生成指定长度的随机十六进制字符串（小写）
     * <p>
     * 示例：randomHex(8) → "3f8a2b1c"
     * </p>
     *
     * @param length 字符串长度
     * @return 随机十六进制字符串
     */
    public static String randomHex(int length) {
        return randomString(HEX_CHARS, length);
    }

    /**
     * 从指定字符集中生成指定长度的随机字符串
     *
     * @param source  字符集
     * @param length  字符串长度
     * @return 随机字符串
     * @throws IllegalArgumentException 字符集为空或长度为负时抛出
     */
    public static String randomString(String source, int length) {
        if (source == null || source.isEmpty()) {
            throw new IllegalArgumentException("字符集不能为空");
        }
        if (length < 0) {
            throw new IllegalArgumentException("长度不能为负数: " + length);
        }
        if (length == 0) {
            return StringUtil.EMPTY;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(length);
        int sourceLength = source.length();
        for (int i = 0; i < length; i++) {
            sb.append(source.charAt(random.nextInt(sourceLength)));
        }
        return sb.toString();
    }

    /**
     * 生成密码学安全的随机字母数字字符串（大小写字母 + 数字）
     * <p>
     * 适用于生成临时密码、安全令牌、API Key 等安全敏感场景。
     * </p>
     *
     * @param length 字符串长度
     * @return 密码学安全的随机字符串
     */
    public static String secureRandomString(int length) {
        return secureRandomString(ALPHANUMERIC_MIXED, length);
    }

    /**
     * 从指定字符集中生成密码学安全的随机字符串
     *
     * @param source 字符集
     * @param length 字符串长度
     * @return 密码学安全的随机字符串
     */
    public static String secureRandomString(String source, int length) {
        if (source == null || source.isEmpty()) {
            throw new IllegalArgumentException("字符集不能为空");
        }
        if (length < 0) {
            throw new IllegalArgumentException("长度不能为负数: " + length);
        }
        if (length == 0) {
            return StringUtil.EMPTY;
        }
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        int sourceLength = source.length();
        for (int i = 0; i < length; i++) {
            sb.append(source.charAt(random.nextInt(sourceLength)));
        }
        return sb.toString();
    }

    // ======================== 随机字节 ========================

    /**
     * 生成指定长度的随机字节数组
     *
     * @param length 字节数组长度
     * @return 随机字节数组
     */
    public static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        ThreadLocalRandom.current().nextBytes(bytes);
        return bytes;
    }

    /**
     * 生成指定长度的随机十六进制字符串（基于字节数组）
     * <p>
     * 与 {@link #randomHex(int)} 不同，此方法生成的是完整的字节级随机，
     * 每个 byte 产生两个十六进制字符，实际长度为 length * 2。
     * </p>
     *
     * @param length 字节数组长度（输出的十六进制字符串长度为 length * 2）
     * @return 随机十六进制字符串
     */
    public static String randomBytesHex(int length) {
        return HexFormat.of().formatHex(randomBytes(length));
    }

    // ======================== 随机选取 ========================

    /**
     * 从数组中随机选取一个元素
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 随机选中的元素，空数组返回 null
     */
    public static <T> T randomElement(T[] array) {
        if (ArrayUtil.isEmpty(array)) {
            return null;
        }
        return array[ThreadLocalRandom.current().nextInt(array.length)];
    }

    /**
     * 从 List 中随机选取一个元素
     *
     * @param list 列表
     * @param <T>  元素类型
     * @return 随机选中的元素，空列表返回 null
     */
    public static <T> T randomElement(List<T> list) {
        if (CollectionUtil.isEmpty(list)) {
            return null;
        }
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    /**
     * 生成指定数量的不重复随机整数（范围 [min, max]）
     * <p>
     * 示例：randomUniqueInts(3, 1, 10) → [3, 7, 1]
     * </p>
     *
     * @param count 生成数量
     * @param min   最小值（包含）
     * @param max   最大值（包含）
     * @return 不重复随机整数列表
     * @throws IllegalArgumentException 当 count 大于范围可用数时抛出
     */
    public static java.util.List<Integer> randomUniqueInts(int count, int min, int max) {
        if (count < 0) {
            throw new IllegalArgumentException("数量不能为负数: " + count);
        }
        if (min > max) {
            throw new IllegalArgumentException("最小值不能大于最大值: min=" + min + ", max=" + max);
        }
        long range = (long) max - min + 1;
        if (count > range) {
            throw new IllegalArgumentException(
                    "请求数量大于可用范围: count=" + count + ", range=" + range);
        }
        if (count == 0) {
            return List.of();
        }
        java.util.Set<Integer> set = new java.util.LinkedHashSet<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        while (set.size() < count) {
            set.add(random.nextInt(min, max + 1));
        }
        return List.copyOf(set);
    }

    // ======================== 打乱顺序 ========================

    /**
     * 打乱数组的顺序（Fisher-Yates 洗牌算法，原位修改）
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 打乱后的数组（与原数组是同一个引用）
     */
    public static <T> T[] shuffle(T[] array) {
        if (ArrayUtil.isEmpty(array) || array.length == 1) {
            return array;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = array.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            ArrayUtil.swap(array, i, j);
        }
        return array;
    }

    /**
     * 打乱 List 的顺序（Fisher-Yates 洗牌算法，返回新列表）
     *
     * @param list 列表
     * @param <T>  元素类型
     * @return 打乱顺序后的新 List
     */
    public static <T> List<T> shuffle(List<T> list) {
        if (CollectionUtil.isEmpty(list) || list.size() == 1) {
            return list;
        }
        T[] array = list.toArray(Arrays.copyOf(new Object[0], 0));
        shuffle(array);
        return List.of(array);
    }

    // ======================== 验证码相关 ========================

    /**
     * 生成指定长度的纯数字验证码
     * <p>
     * 使用密码学安全随机数生成器，适用于短信验证码、图形验证码等场景。
     * 自动去除前导零（最小位数为 1 位）。
     * </p>
     *
     * @param length 期望长度
     * @return 数字验证码字符串
     */
    public static String verificationCode(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("验证码长度必须大于 0: " + length);
        }
        if (length == 1) {
            return String.valueOf(new SecureRandom().nextInt(10));
        }
        // 使用大范围随机 + 取模保证无前导零
        int min = (int) Math.pow(10, length - 1);
        int max = (int) Math.pow(10, length) - 1;
        return String.valueOf(min + new SecureRandom().nextInt(max - min + 1));
    }
}
