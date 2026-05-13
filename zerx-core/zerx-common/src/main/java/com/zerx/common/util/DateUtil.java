package com.zerx.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;

/**
 * 日期时间工具类
 * <p>
 * 基于 JDK 21 {@code java.time} 包的日期时间工具，提供格式化、解析、
 * 差值计算、时区转换等功能。对常用日期格式定义标准常量，统一项目中的日期处理风格。
 * </p>
 *
 * @author zerx
 */
public final class DateUtil {

    // ======================== 常用格式常量 ========================

    /** 日期格式：yyyy-MM-dd */
    public static final String PATTERN_DATE = "yyyy-MM-dd";

    /** 时间格式：HH:mm:ss */
    public static final String PATTERN_TIME = "HH:mm:ss";

    /** 日期时间格式：yyyy-MM-dd HH:mm:ss */
    public static final String PATTERN_DATETIME = "yyyy-MM-dd HH:mm:ss";

    /** 日期时间格式（带毫秒）：yyyy-MM-dd HH:mm:ss.SSS */
    public static final String PATTERN_DATETIME_MS = "yyyy-MM-dd HH:mm:ss.SSS";

    /** 紧凑日期格式：yyyyMMdd */
    public static final String PATTERN_COMPACT_DATE = "yyyyMMdd";

    /** 紧凑日期时间格式：yyyyMMddHHmmss */
    public static final String PATTERN_COMPACT_DATETIME = "yyyyMMddHHmmss";

    /** ISO 日期时间格式 */
    public static final String PATTERN_ISO_DATETIME = "yyyy-MM-dd'T'HH:mm:ss";

    // ======================== 常用格式化器 ========================

    /** 日期格式化器（线程安全） */
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(PATTERN_DATE);

    /** 时间格式化器（线程安全） */
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(PATTERN_TIME);

    /** 日期时间格式化器（线程安全） */
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(PATTERN_DATETIME);

    /** 带毫秒的日期时间格式化器（线程安全） */
    public static final DateTimeFormatter DATETIME_MS_FORMATTER = DateTimeFormatter.ofPattern(PATTERN_DATETIME_MS);

    /** ISO 日期时间格式化器（线程安全） */
    public static final DateTimeFormatter ISO_DATETIME_FORMATTER = DateTimeFormatter.ofPattern(PATTERN_ISO_DATETIME);

    /** 紧凑日期格式化器（线程安全） */
    public static final DateTimeFormatter COMPACT_DATE_FORMATTER = DateTimeFormatter.ofPattern(PATTERN_COMPACT_DATE);

    /** 紧凑日期时间格式化器（线程安全） */
    public static final DateTimeFormatter COMPACT_DATETIME_FORMATTER = DateTimeFormatter.ofPattern(PATTERN_COMPACT_DATETIME);

    /** 默认时区 */
    public static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    /** UTC 时区 */
    public static final ZoneId UTC_ZONE = ZoneId.of("UTC");

    /** 秒与毫秒的换算因子 */
    private static final long MILLIS_PER_SECOND = 1000L;

    /** 秒与分钟的换算因子 */
    private static final long SECONDS_PER_MINUTE = 60L;

    /** 秒与小时的换算因子 */
    private static final long SECONDS_PER_HOUR = 3600L;

    /** 秒与天的换算因子 */
    private static final long SECONDS_PER_DAY = 86400L;

    /** 秒与天的换算因子（double） */
    private static final double DAYS_PER_YEAR_AVG = 365.25;

    /** 秒与年的换算因子 */
    private static final long SECONDS_PER_YEAR = (long) (SECONDS_PER_DAY * DAYS_PER_YEAR_AVG);

    /** 秒与月的换算因子 */
    private static final long SECONDS_PER_MONTH = SECONDS_PER_YEAR / 12;

    /** 私有构造器，防止实例化 */
    private DateUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ======================== 获取当前时间 ========================

    /**
     * 获取当前日期（不含时间）
     *
     * @return 当前日期
     */
    public static LocalDate now() {
        return LocalDate.now();
    }

    /**
     * 获取当前日期时间
     *
     * @return 当前日期时间
     */
    public static LocalDateTime nowDateTime() {
        return LocalDateTime.now();
    }

    /**
     * 获取当前时间戳（毫秒）
     *
     * @return 当前毫秒时间戳
     */
    public static long currentMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 获取当前时间戳（秒）
     *
     * @return 当前秒时间戳
     */
    public static long currentSeconds() {
        return System.currentTimeMillis() / MILLIS_PER_SECOND;
    }

    // ======================== 格式化 ========================

    /**
     * 格式化日期为 yyyy-MM-dd
     *
     * @param date 日期
     * @return 格式化后的字符串，null 返回 null
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(DATE_FORMATTER);
    }

    /**
     * 格式化日期时间为 yyyy-MM-dd HH:mm:ss
     *
     * @param dateTime 日期时间
     * @return 格式化后的字符串，null 返回 null
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DATETIME_FORMATTER);
    }

    /**
     * 使用自定义格式格式化日期时间
     *
     * @param dateTime 日期时间
     * @param pattern  日期格式
     * @return 格式化后的字符串
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null || pattern == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 使用自定义格式化器格式化日期时间
     *
     * @param dateTime  日期时间
     * @param formatter 格式化器
     * @return 格式化后的字符串
     */
    public static String format(LocalDateTime dateTime, DateTimeFormatter formatter) {
        if (dateTime == null || formatter == null) {
            return null;
        }
        return dateTime.format(formatter);
    }

    /**
     * 将毫秒时间戳格式化为 yyyy-MM-dd HH:mm:ss
     *
     * @param millis 毫秒时间戳
     * @return 格式化后的字符串
     */
    public static String formatMillis(long millis) {
        return formatDateTime(ofMillis(millis));
    }

    // ======================== 解析 ========================

    /**
     * 解析日期字符串（yyyy-MM-dd）
     *
     * @param dateStr 日期字符串
     * @return 解析后的 LocalDate
     * @throws DateTimeParseException 解析失败时抛出
     */
    public static LocalDate parseDate(String dateStr) {
        if (StringUtil.isBlank(dateStr)) {
            return null;
        }
        return LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
    }

    /**
     * 解析日期时间字符串（yyyy-MM-dd HH:mm:ss）
     *
     * @param dateTimeStr 日期时间字符串
     * @return 解析后的 LocalDateTime
     * @throws DateTimeParseException 解析失败时抛出
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        if (StringUtil.isBlank(dateTimeStr)) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr.trim(), DATETIME_FORMATTER);
    }

    /**
     * 使用自定义格式解析日期时间字符串
     *
     * @param dateTimeStr 日期时间字符串
     * @param pattern     日期格式
     * @return 解析后的 LocalDateTime
     * @throws DateTimeParseException 解析失败时抛出
     */
    public static LocalDateTime parse(String dateTimeStr, String pattern) {
        if (StringUtil.isBlank(dateTimeStr) || StringUtil.isBlank(pattern)) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr.trim(), DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 使用自定义格式化器解析日期时间字符串
     *
     * @param dateTimeStr 日期时间字符串
     * @param formatter   格式化器
     * @return 解析后的 LocalDateTime
     * @throws DateTimeParseException 解析失败时抛出
     */
    public static LocalDateTime parse(String dateTimeStr, DateTimeFormatter formatter) {
        if (StringUtil.isBlank(dateTimeStr) || formatter == null) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr.trim(), formatter);
    }

    // ======================== 时间戳转换 ========================

    /**
     * 将毫秒时间戳转换为 LocalDateTime
     *
     * @param millis 毫秒时间戳
     * @return LocalDateTime
     */
    public static LocalDateTime ofMillis(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), DEFAULT_ZONE);
    }

    /**
     * 将秒时间戳转换为 LocalDateTime
     *
     * @param seconds 秒时间戳
     * @return LocalDateTime
     */
    public static LocalDateTime ofSeconds(long seconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds), DEFAULT_ZONE);
    }

    /**
     * 将 LocalDateTime 转换为毫秒时间戳
     *
     * @param dateTime 日期时间
     * @return 毫秒时间戳
     */
    public static long toMillis(LocalDateTime dateTime) {
        if (dateTime == null) {
            return 0L;
        }
        return dateTime.atZone(DEFAULT_ZONE).toInstant().toEpochMilli();
    }

    /**
     * 将 LocalDateTime 转换为秒时间戳
     *
     * @param dateTime 日期时间
     * @return 秒时间戳
     */
    public static long toSeconds(LocalDateTime dateTime) {
        if (dateTime == null) {
            return 0L;
        }
        return dateTime.atZone(DEFAULT_ZONE).toInstant().getEpochSecond();
    }

    // ======================== 差值计算 ========================

    /**
     * 计算两个日期之间的天数差
     *
     * @param start 开始日期
     * @param end   结束日期
     * @return 天数差（end - start），null 参数返回 0
     */
    public static long daysBetween(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 计算两个日期时间之间的小时差
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 小时差（end - start）
     */
    public static long hoursBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.HOURS.between(start, end);
    }

    /**
     * 计算两个日期时间之间的分钟差
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 分钟差（end - start）
     */
    public static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(start, end);
    }

    /**
     * 计算两个日期时间之间的秒差
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return 秒差（end - start）
     */
    public static long secondsBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.SECONDS.between(start, end);
    }

    // ======================== 日期调整 ========================

    /**
     * 获取某天的开始时间（00:00:00）
     *
     * @param date 日期
     * @return 当天 00:00:00
     */
    public static LocalDateTime startOfDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atStartOfDay();
    }

    /**
     * 获取某天的结束时间（23:59:59）
     *
     * @param date 日期
     * @return 当天 23:59:59
     */
    public static LocalDateTime endOfDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atTime(23, 59, 59);
    }

    /**
     * 获取当月第一天
     *
     * @param date 日期
     * @return 当月第一天
     */
    public static LocalDate firstDayOfMonth(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.with(TemporalAdjusters.firstDayOfMonth());
    }

    /**
     * 获取当月最后一天
     *
     * @param date 日期
     * @return 当月最后一天
     */
    public static LocalDate lastDayOfMonth(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.with(TemporalAdjusters.lastDayOfMonth());
    }

    /**
     * 判断是否为闰年
     *
     * @param date 日期
     * @return 闰年返回 true
     */
    public static boolean isLeapYear(LocalDate date) {
        return date != null && date.isLeapYear();
    }

    // ======================== 时区转换 ========================

    /**
     * 将日期时间从默认时区转换为目标时区
     *
     * @param dateTime   日期时间
     * @param targetZone 目标时区
     * @return 转换后的日期时间
     */
    public static LocalDateTime convertZone(LocalDateTime dateTime, ZoneId targetZone) {
        if (dateTime == null || targetZone == null) {
            return dateTime;
        }
        return dateTime.atZone(DEFAULT_ZONE).withZoneSameInstant(targetZone).toLocalDateTime();
    }

    /**
     * 将日期时间从默认时区转换为 UTC 时区
     *
     * @param dateTime 日期时间
     * @return UTC 时间的日期时间
     */
    public static LocalDateTime toUtc(LocalDateTime dateTime) {
        return convertZone(dateTime, UTC_ZONE);
    }

    /**
     * 将 UTC 日期时间转换为默认时区
     *
     * @param utcDateTime UTC 时间
     * @return 默认时区的日期时间
     */
    public static LocalDateTime fromUtc(LocalDateTime utcDateTime) {
        if (utcDateTime == null) {
            return null;
        }
        return utcDateTime.atZone(UTC_ZONE).withZoneSameInstant(DEFAULT_ZONE).toLocalDateTime();
    }

    // ======================== 判断辅助 ========================

    /**
     * 判断日期是否在指定范围内（包含边界）
     *
     * @param date 待判断的日期
     * @param start 范围开始
     * @param end   范围结束
     * @return 在范围内返回 true
     */
    public static boolean isBetween(LocalDate date, LocalDate start, LocalDate end) {
        if (date == null) {
            return false;
        }
        return !date.isBefore(start) && !date.isAfter(end);
    }

    /**
     * 判断日期时间是否在指定范围内（包含边界）
     *
     * @param dateTime 待判断的日期时间
     * @param start    范围开始
     * @param end      范围结束
     * @return 在范围内返回 true
     */
    public static boolean isBetween(LocalDateTime dateTime, LocalDateTime start, LocalDateTime end) {
        if (dateTime == null) {
            return false;
        }
        return !dateTime.isBefore(start) && !dateTime.isAfter(end);
    }

    /**
     * 判断是否为今天
     *
     * @param date 日期
     * @return 是今天返回 true
     */
    public static boolean isToday(LocalDate date) {
        return Objects.equals(date, LocalDate.now());
    }

    /**
     * 判断是否为过去时间
     *
     * @param dateTime 日期时间
     * @return 是过去时间返回 true
     */
    public static boolean isPast(LocalDateTime dateTime) {
        return dateTime != null && dateTime.isBefore(LocalDateTime.now());
    }

    /**
     * 判断是否为未来时间
     *
     * @param dateTime 日期时间
     * @return 是未来时间返回 true
     */
    public static boolean isFuture(LocalDateTime dateTime) {
        return dateTime != null && dateTime.isAfter(LocalDateTime.now());
    }
}
