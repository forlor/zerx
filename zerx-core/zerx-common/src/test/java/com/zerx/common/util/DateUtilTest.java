package com.zerx.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link DateUtil} 单元测试
 */
@DisplayName("DateUtil 日期时间工具类测试")
class DateUtilTest {

    // ======================== 获取当前时间 ========================

    @Nested
    @DisplayName("now / nowDateTime 测试")
    class NowTests {

        @Test
        @DisplayName("now 返回当前日期")
        void now_returnsCurrentDate() {
            LocalDate result = DateUtil.now();
            assertNotNull(result);
            assertEquals(LocalDate.now(), result);
        }

        @Test
        @DisplayName("nowDateTime 返回当前日期时间")
        void nowDateTime_returnsCurrentDateTime() {
            LocalDateTime result = DateUtil.nowDateTime();
            assertNotNull(result);
            // 允许一定的时间差（1秒内）
            assertTrue(Duration.between(result, LocalDateTime.now()).abs().getSeconds() <= 1);
        }
    }

    @Nested
    @DisplayName("currentMillis / currentSeconds 测试")
    class CurrentTimestampTests {

        @Test
        @DisplayName("currentMillis 返回毫秒时间戳")
        void currentMillis() {
            long before = System.currentTimeMillis();
            long result = DateUtil.currentMillis();
            long after = System.currentTimeMillis();
            assertTrue(result >= before && result <= after);
        }

        @Test
        @DisplayName("currentSeconds 返回秒时间戳")
        void currentSeconds() {
            long before = System.currentTimeMillis() / 1000;
            long result = DateUtil.currentSeconds();
            long after = System.currentTimeMillis() / 1000;
            assertTrue(result >= before && result <= after);
        }

        @Test
        @DisplayName("currentSeconds 应约为 currentMillis / 1000")
        void currentSeconds_equalsMillisDivided() {
            long millis = DateUtil.currentMillis();
            long seconds = DateUtil.currentSeconds();
            assertEquals(millis / 1000, seconds);
        }
    }

    // ======================== 格式化 ========================

    @Nested
    @DisplayName("formatDate / formatDateTime 测试")
    class FormatTests {

        @Test
        @DisplayName("formatDate - null 返回 null")
        void formatDate_null() {
            assertNull(DateUtil.formatDate(null));
        }

        @Test
        @DisplayName("formatDate - 正常格式化")
        void formatDate_normal() {
            LocalDate date = LocalDate.of(2024, 6, 15);
            assertEquals("2024-06-15", DateUtil.formatDate(date));
        }

        @Test
        @DisplayName("formatDateTime - null 返回 null")
        void formatDateTime_null() {
            assertNull(DateUtil.formatDateTime(null));
        }

        @Test
        @DisplayName("formatDateTime - 正常格式化")
        void formatDateTime_normal() {
            LocalDateTime dateTime = LocalDateTime.of(2024, 6, 15, 10, 30, 45);
            assertEquals("2024-06-15 10:30:45", DateUtil.formatDateTime(dateTime));
        }

        @Test
        @DisplayName("format(String pattern) - 正常格式化")
        void format_pattern_normal() {
            LocalDateTime dateTime = LocalDateTime.of(2024, 6, 15, 10, 30, 45);
            assertEquals("2024/06/15", DateUtil.format(dateTime, "yyyy/MM/dd"));
        }

        @Test
        @DisplayName("format(String pattern) - null dateTime 返回 null")
        void format_pattern_nullDateTime() {
            assertNull(DateUtil.format(null, "yyyy-MM-dd"));
        }

        @Test
        @DisplayName("format(String pattern) - null pattern 返回 null")
        void format_pattern_nullPattern() {
            assertNull(DateUtil.format(LocalDateTime.now(), (String) null));
        }

        @Test
        @DisplayName("format(DateTimeFormatter) - 正常格式化")
        void format_formatter_normal() {
            LocalDateTime dateTime = LocalDateTime.of(2024, 6, 15, 10, 30, 45);
            String result = DateUtil.format(dateTime, DateTimeFormatter.ISO_DATE);
            assertEquals("2024-06-15", result);
        }

        @Test
        @DisplayName("format(DateTimeFormatter) - null 参数返回 null")
        void format_formatter_null() {
            assertNull(DateUtil.format(null, DateTimeFormatter.ISO_DATE));
            assertNull(DateUtil.format(LocalDateTime.now(), (DateTimeFormatter) null));
        }

        @Test
        @DisplayName("formatMillis - 格式化毫秒时间戳")
        void formatMillis_normal() {
            LocalDateTime dateTime = LocalDateTime.of(2024, 6, 15, 10, 30, 45);
            long millis = dateTime.atZone(DateUtil.DEFAULT_ZONE).toInstant().toEpochMilli();
            assertEquals("2024-06-15 10:30:45", DateUtil.formatMillis(millis));
        }
    }

    // ======================== 解析 ========================

    @Nested
    @DisplayName("parseDate / parseDateTime / parse 测试")
    class ParseTests {

        @Test
        @DisplayName("parseDate - null 返回 null")
        void parseDate_null() {
            assertNull(DateUtil.parseDate(null));
        }

        @Test
        @DisplayName("parseDate - 空字符串返回 null")
        void parseDate_empty() {
            assertNull(DateUtil.parseDate(""));
            assertNull(DateUtil.parseDate("   "));
        }

        @Test
        @DisplayName("parseDate - 正常解析")
        void parseDate_normal() {
            LocalDate result = DateUtil.parseDate("2024-06-15");
            assertEquals(LocalDate.of(2024, 6, 15), result);
        }

        @Test
        @DisplayName("parseDate - 带空格自动 trim")
        void parseDate_withSpaces() {
            LocalDate result = DateUtil.parseDate("  2024-06-15  ");
            assertEquals(LocalDate.of(2024, 6, 15), result);
        }

        @Test
        @DisplayName("parseDateTime - null 返回 null")
        void parseDateTime_null() {
            assertNull(DateUtil.parseDateTime(null));
        }

        @Test
        @DisplayName("parseDateTime - 正常解析")
        void parseDateTime_normal() {
            LocalDateTime result = DateUtil.parseDateTime("2024-06-15 10:30:45");
            assertEquals(LocalDateTime.of(2024, 6, 15, 10, 30, 45), result);
        }

        @Test
        @DisplayName("parse(String, String) - 正常解析")
        void parse_pattern_normal() {
            LocalDateTime result = DateUtil.parse("2024/06/15 10:30", "yyyy/MM/dd HH:mm");
            assertEquals(LocalDateTime.of(2024, 6, 15, 10, 30), result);
        }

        @Test
        @DisplayName("parse(String, String) - null 参数返回 null")
        void parse_pattern_null() {
            assertNull(DateUtil.parse((String) null, "yyyy-MM-dd"));
            assertNull(DateUtil.parse("2024-06-15", (String) null));
            assertNull(DateUtil.parse("", "yyyy-MM-dd"));
        }

        @Test
        @DisplayName("parse(String, DateTimeFormatter) - 正常解析")
        void parse_formatter_normal() {
            LocalDateTime result = DateUtil.parse("2024-06-15T10:30:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            assertEquals(LocalDateTime.of(2024, 6, 15, 10, 30), result);
        }

        @Test
        @DisplayName("parse(String, DateTimeFormatter) - null 参数返回 null")
        void parse_formatter_null() {
            assertNull(DateUtil.parse(null, DateTimeFormatter.ISO_DATE));
            assertNull(DateUtil.parse("2024-06-15", (DateTimeFormatter) null));
        }
    }

    // ======================== 时间戳转换 ========================

    @Nested
    @DisplayName("ofMillis / ofSeconds / toMillis / toSeconds 测试")
    class TimestampConversionTests {

        @Test
        @DisplayName("ofMillis / toMillis 双向转换一致")
        void millis_roundTrip() {
            LocalDateTime original = LocalDateTime.of(2024, 6, 15, 10, 30, 45);
            long millis = DateUtil.toMillis(original);
            LocalDateTime result = DateUtil.ofMillis(millis);
            assertEquals(original, result);
        }

        @Test
        @DisplayName("ofSeconds / toSeconds 双向转换一致")
        void seconds_roundTrip() {
            LocalDateTime original = LocalDateTime.of(2024, 6, 15, 10, 30, 0);
            long seconds = DateUtil.toSeconds(original);
            LocalDateTime result = DateUtil.ofSeconds(seconds);
            assertEquals(original, result);
        }

        @Test
        @DisplayName("ofMillis - Unix 纪元")
        void ofMillis_epoch() {
            LocalDateTime result = DateUtil.ofMillis(0);
            assertNotNull(result);
        }

        @Test
        @DisplayName("ofSeconds - Unix 纪元")
        void ofSeconds_epoch() {
            LocalDateTime result = DateUtil.ofSeconds(0);
            assertNotNull(result);
        }

        @Test
        @DisplayName("toMillis - null 返回 0")
        void toMillis_null() {
            assertEquals(0L, DateUtil.toMillis(null));
        }

        @Test
        @DisplayName("toSeconds - null 返回 0")
        void toSeconds_null() {
            assertEquals(0L, DateUtil.toSeconds(null));
        }

        @Test
        @DisplayName("toMillis 和 toSeconds 的关系")
        void millisSeconds_relation() {
            LocalDateTime dt = LocalDateTime.of(2024, 6, 15, 10, 30, 0);
            long millis = DateUtil.toMillis(dt);
            long seconds = DateUtil.toSeconds(dt);
            assertEquals(millis / 1000, seconds);
        }
    }

    // ======================== 差值计算 ========================

    @Nested
    @DisplayName("daysBetween / hoursBetween / minutesBetween / secondsBetween 测试")
    class BetweenTests {

        @Test
        @DisplayName("daysBetween - 正向差")
        void daysBetween_positive() {
            long days = DateUtil.daysBetween(
                    LocalDate.of(2024, 6, 1),
                    LocalDate.of(2024, 6, 10));
            assertEquals(9, days);
        }

        @Test
        @DisplayName("daysBetween - 反向差（负数）")
        void daysBetween_negative() {
            long days = DateUtil.daysBetween(
                    LocalDate.of(2024, 6, 10),
                    LocalDate.of(2024, 6, 1));
            assertEquals(-9, days);
        }

        @Test
        @DisplayName("daysBetween - 同一天返回 0")
        void daysBetween_sameDay() {
            assertEquals(0, DateUtil.daysBetween(LocalDate.of(2024, 6, 15), LocalDate.of(2024, 6, 15)));
        }

        @Test
        @DisplayName("daysBetween - null 参数返回 0")
        void daysBetween_null() {
            assertEquals(0, DateUtil.daysBetween(null, LocalDate.of(2024, 6, 15)));
            assertEquals(0, DateUtil.daysBetween(LocalDate.of(2024, 6, 15), null));
            assertEquals(0, DateUtil.daysBetween(null, null));
        }

        @Test
        @DisplayName("hoursBetween - 正常计算")
        void hoursBetween_normal() {
            long hours = DateUtil.hoursBetween(
                    LocalDateTime.of(2024, 6, 15, 10, 0),
                    LocalDateTime.of(2024, 6, 15, 16, 0));
            assertEquals(6, hours);
        }

        @Test
        @DisplayName("hoursBetween - null 参数返回 0")
        void hoursBetween_null() {
            assertEquals(0, DateUtil.hoursBetween(null, LocalDateTime.now()));
        }

        @Test
        @DisplayName("minutesBetween - 正常计算")
        void minutesBetween_normal() {
            long minutes = DateUtil.minutesBetween(
                    LocalDateTime.of(2024, 6, 15, 10, 0),
                    LocalDateTime.of(2024, 6, 15, 10, 30));
            assertEquals(30, minutes);
        }

        @Test
        @DisplayName("minutesBetween - null 参数返回 0")
        void minutesBetween_null() {
            assertEquals(0, DateUtil.minutesBetween(null, LocalDateTime.now()));
        }

        @Test
        @DisplayName("secondsBetween - 正常计算")
        void secondsBetween_normal() {
            long seconds = DateUtil.secondsBetween(
                    LocalDateTime.of(2024, 6, 15, 10, 0, 0),
                    LocalDateTime.of(2024, 6, 15, 10, 0, 45));
            assertEquals(45, seconds);
        }

        @Test
        @DisplayName("secondsBetween - null 参数返回 0")
        void secondsBetween_null() {
            assertEquals(0, DateUtil.secondsBetween(null, LocalDateTime.now()));
        }
    }

    // ======================== 日期调整 ========================

    @Nested
    @DisplayName("startOfDay / endOfDay 测试")
    class DayBoundaryTests {

        @Test
        @DisplayName("startOfDay - null 返回 null")
        void startOfDay_null() {
            assertNull(DateUtil.startOfDay(null));
        }

        @Test
        @DisplayName("startOfDay - 返回 00:00:00")
        void startOfDay_normal() {
            LocalDateTime result = DateUtil.startOfDay(LocalDate.of(2024, 6, 15));
            assertEquals(LocalDateTime.of(2024, 6, 15, 0, 0, 0), result);
        }

        @Test
        @DisplayName("endOfDay - null 返回 null")
        void endOfDay_null() {
            assertNull(DateUtil.endOfDay(null));
        }

        @Test
        @DisplayName("endOfDay - 返回 23:59:59")
        void endOfDay_normal() {
            LocalDateTime result = DateUtil.endOfDay(LocalDate.of(2024, 6, 15));
            assertEquals(LocalDateTime.of(2024, 6, 15, 23, 59, 59), result);
        }
    }

    @Nested
    @DisplayName("firstDayOfMonth / lastDayOfMonth 测试")
    class MonthBoundaryTests {

        @Test
        @DisplayName("firstDayOfMonth - null 返回 null")
        void firstDayOfMonth_null() {
            assertNull(DateUtil.firstDayOfMonth(null));
        }

        @Test
        @DisplayName("firstDayOfMonth - 正常返回")
        void firstDayOfMonth_normal() {
            assertEquals(LocalDate.of(2024, 6, 1), DateUtil.firstDayOfMonth(LocalDate.of(2024, 6, 15)));
        }

        @Test
        @DisplayName("firstDayOfMonth - 闰年二月")
        void firstDayOfMonth_leapFeb() {
            assertEquals(LocalDate.of(2024, 2, 1), DateUtil.firstDayOfMonth(LocalDate.of(2024, 2, 15)));
        }

        @Test
        @DisplayName("lastDayOfMonth - null 返回 null")
        void lastDayOfMonth_null() {
            assertNull(DateUtil.lastDayOfMonth(null));
        }

        @Test
        @DisplayName("lastDayOfMonth - 30 天的月份")
        void lastDayOfMonth_30Days() {
            assertEquals(LocalDate.of(2024, 6, 30), DateUtil.lastDayOfMonth(LocalDate.of(2024, 6, 15)));
        }

        @Test
        @DisplayName("lastDayOfMonth - 31 天的月份")
        void lastDayOfMonth_31Days() {
            assertEquals(LocalDate.of(2024, 1, 31), DateUtil.lastDayOfMonth(LocalDate.of(2024, 1, 15)));
        }

        @Test
        @DisplayName("lastDayOfMonth - 非闰年二月")
        void lastDayOfMonth_nonLeapFeb() {
            assertEquals(LocalDate.of(2023, 2, 28), DateUtil.lastDayOfMonth(LocalDate.of(2023, 2, 15)));
        }

        @Test
        @DisplayName("lastDayOfMonth - 闰年二月")
        void lastDayOfMonth_leapFeb() {
            assertEquals(LocalDate.of(2024, 2, 29), DateUtil.lastDayOfMonth(LocalDate.of(2024, 2, 15)));
        }
    }

    @Nested
    @DisplayName("isLeapYear 测试")
    class IsLeapYearTests {

        @Test
        @DisplayName("null 返回 false")
        void isLeapYear_null() {
            assertFalse(DateUtil.isLeapYear(null));
        }

        @Test
        @DisplayName("2024 是闰年")
        void isLeapYear_2024() {
            assertTrue(DateUtil.isLeapYear(LocalDate.of(2024, 1, 1)));
        }

        @Test
        @DisplayName("2023 不是闰年")
        void isLeapYear_2023() {
            assertFalse(DateUtil.isLeapYear(LocalDate.of(2023, 1, 1)));
        }

        @Test
        @DisplayName("2000 是闰年（能被 400 整除）")
        void isLeapYear_2000() {
            assertTrue(DateUtil.isLeapYear(LocalDate.of(2000, 1, 1)));
        }

        @Test
        @DisplayName("1900 不是闰年（能被 100 但不能被 400 整除）")
        void isLeapYear_1900() {
            assertFalse(DateUtil.isLeapYear(LocalDate.of(1900, 1, 1)));
        }
    }

    // ======================== 时区转换 ========================

    @Nested
    @DisplayName("convertZone / toUtc / fromUtc 测试")
    class TimeZoneTests {

        @Test
        @DisplayName("convertZone - null dateTime 返回 null")
        void convertZone_nullDateTime() {
            assertNull(DateUtil.convertZone(null, ZoneId.of("America/New_York")));
        }

        @Test
        @DisplayName("convertZone - null zone 返回原值")
        void convertZone_nullZone() {
            LocalDateTime dt = LocalDateTime.of(2024, 6, 15, 10, 0);
            assertEquals(dt, DateUtil.convertZone(dt, null));
        }

        @Test
        @DisplayName("convertZone - 转换为 UTC")
        void convertZone_toUtc() {
            LocalDateTime local = LocalDateTime.of(2024, 6, 15, 10, 0);
            LocalDateTime utc = DateUtil.convertZone(local, ZoneId.of("UTC"));
            assertNotNull(utc);
            // 结果取决于系统时区，这里只验证返回非 null
        }

        @Test
        @DisplayName("toUtc - 等于 convertZone(dateTime, UTC_ZONE)")
        void toUtc_equalsConvertZone() {
            LocalDateTime dt = LocalDateTime.of(2024, 6, 15, 10, 0);
            assertEquals(DateUtil.convertZone(dt, DateUtil.UTC_ZONE), DateUtil.toUtc(dt));
        }

        @Test
        @DisplayName("fromUtc - null 返回 null")
        void fromUtc_null() {
            assertNull(DateUtil.fromUtc(null));
        }

        @Test
        @DisplayName("fromUtc - toUtc 互逆")
        void fromUtc_roundTrip() {
            LocalDateTime original = LocalDateTime.of(2024, 6, 15, 10, 0);
            LocalDateTime utc = DateUtil.toUtc(original);
            LocalDateTime result = DateUtil.fromUtc(utc);
            assertEquals(original, result);
        }
    }

    // ======================== 判断辅助 ========================

    @Nested
    @DisplayName("isBetween(LocalDate) 测试")
    class IsBetweenDateTests {

        @Test
        @DisplayName("null 返回 false")
        void isBetween_date_null() {
            assertFalse(DateUtil.isBetween(null, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)));
        }

        @Test
        @DisplayName("在范围内")
        void isBetween_date_inRange() {
            assertTrue(DateUtil.isBetween(
                    LocalDate.of(2024, 6, 15),
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 12, 31)));
        }

        @Test
        @DisplayName("边界值通过")
        void isBetween_date_boundaries() {
            assertTrue(DateUtil.isBetween(
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 12, 31)));
            assertTrue(DateUtil.isBetween(
                    LocalDate.of(2024, 12, 31),
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 12, 31)));
        }

        @Test
        @DisplayName("超出范围返回 false")
        void isBetween_date_outOfRange() {
            assertFalse(DateUtil.isBetween(
                    LocalDate.of(2023, 12, 31),
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 12, 31)));
        }
    }

    @Nested
    @DisplayName("isBetween(LocalDateTime) 测试")
    class IsBetweenDateTimeTests {

        @Test
        @DisplayName("null 返回 false")
        void isBetween_dateTime_null() {
            assertFalse(DateUtil.isBetween(null,
                    LocalDateTime.of(2024, 1, 1, 0, 0),
                    LocalDateTime.of(2024, 12, 31, 23, 59)));
        }

        @Test
        @DisplayName("在范围内")
        void isBetween_dateTime_inRange() {
            assertTrue(DateUtil.isBetween(
                    LocalDateTime.of(2024, 6, 15, 12, 0),
                    LocalDateTime.of(2024, 1, 1, 0, 0),
                    LocalDateTime.of(2024, 12, 31, 23, 59)));
        }

        @Test
        @DisplayName("超出范围返回 false")
        void isBetween_dateTime_outOfRange() {
            assertFalse(DateUtil.isBetween(
                    LocalDateTime.of(2025, 1, 1, 0, 0),
                    LocalDateTime.of(2024, 1, 1, 0, 0),
                    LocalDateTime.of(2024, 12, 31, 23, 59)));
        }
    }

    @Nested
    @DisplayName("isToday 测试")
    class IsTodayTests {

        @Test
        @DisplayName("今天是今天")
        void isToday_true() {
            assertTrue(DateUtil.isToday(LocalDate.now()));
        }

        @Test
        @DisplayName("昨天不是今天")
        void isToday_false() {
            assertFalse(DateUtil.isToday(LocalDate.now().minusDays(1)));
        }

        @Test
        @DisplayName("null 不是今天")
        void isToday_null() {
            assertFalse(DateUtil.isToday(null));
        }
    }

    @Nested
    @DisplayName("isPast / isFuture 测试")
    class PastFutureTests {

        @Test
        @DisplayName("isPast - 过去时间返回 true")
        void isPast_true() {
            assertTrue(DateUtil.isPast(LocalDateTime.now().minusDays(1)));
        }

        @Test
        @DisplayName("isPast - 未来时间返回 false")
        void isPast_false() {
            assertFalse(DateUtil.isPast(LocalDateTime.now().plusDays(1)));
        }

        @Test
        @DisplayName("isPast - null 返回 false")
        void isPast_null() {
            assertFalse(DateUtil.isPast(null));
        }

        @Test
        @DisplayName("isFuture - 未来时间返回 true")
        void isFuture_true() {
            assertTrue(DateUtil.isFuture(LocalDateTime.now().plusDays(1)));
        }

        @Test
        @DisplayName("isFuture - 过去时间返回 false")
        void isFuture_false() {
            assertFalse(DateUtil.isFuture(LocalDateTime.now().minusDays(1)));
        }

        @Test
        @DisplayName("isFuture - null 返回 false")
        void isFuture_null() {
            assertFalse(DateUtil.isFuture(null));
        }
    }

    // ======================== 常量验证 ========================

    @Nested
    @DisplayName("格式常量验证")
    class PatternConstantTests {

        @Test
        @DisplayName("PATTERN_DATE")
        void patternDate() {
            assertEquals("yyyy-MM-dd", DateUtil.PATTERN_DATE);
        }

        @Test
        @DisplayName("PATTERN_TIME")
        void patternTime() {
            assertEquals("HH:mm:ss", DateUtil.PATTERN_TIME);
        }

        @Test
        @DisplayName("PATTERN_DATETIME")
        void patternDateTime() {
            assertEquals("yyyy-MM-dd HH:mm:ss", DateUtil.PATTERN_DATETIME);
        }

        @Test
        @DisplayName("DATE_FORMATTER 格式化正确")
        void dateFormatter() {
            assertEquals("2024-06-15", LocalDate.of(2024, 6, 15).format(DateUtil.DATE_FORMATTER));
        }

        @Test
        @DisplayName("DATETIME_FORMATTER 格式化正确")
        void dateTimeFormatter() {
            assertEquals("2024-06-15 10:30:45",
                    LocalDateTime.of(2024, 6, 15, 10, 30, 45).format(DateUtil.DATETIME_FORMATTER));
        }
    }
}
