package top.harrylei.bitlog.common.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * 日期工具类
 *
 * @author harry
 * @since 2026-03-17
 */
public class DateUtil {

    private static final Logger log = LoggerFactory.getLogger(DateUtil.class);

    public static final DateTimeFormatter STANDARD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter UTC_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    public static final DateTimeFormatter DB_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final List<DateTimeFormatter> SUPPORTED_FORMATTERS = Arrays.asList(
            STANDARD_FORMAT, DATE_FORMAT, UTC_FORMAT, DB_FORMAT, DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );

    public static final Long ONE_DAY_MILL = 86400_000L;
    public static final Long ONE_DAY_SECONDS = 86400L;
    public static final Long THREE_DAY_MILL = 3 * ONE_DAY_MILL;

    private DateUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 多格式自动识别解析日期字符串为 LocalDateTime
     *
     * @param dateStr 日期字符串
     * @return LocalDateTime，解析失败返回 null
     */
    public static LocalDateTime parseDateTime(String dateStr) {
        if (StringUtils.isBlank(dateStr)) return null;
        for (DateTimeFormatter formatter : SUPPORTED_FORMATTERS) {
            try {
                if (dateStr.length() == 10) {
                    return LocalDate.parse(dateStr, formatter).atStartOfDay();
                } else {
                    return LocalDateTime.parse(dateStr, formatter);
                }
            } catch (Exception ignored) {
            }
        }
        log.warn("无法解析日期: {}", dateStr);
        return null;
    }

    /**
     * 毫秒时间戳转 LocalDateTime
     */
    public static LocalDateTime time2LocalTime(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }

    /**
     * LocalDateTime 格式化为标准字符串（yyyy-MM-dd HH:mm:ss）
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : STANDARD_FORMAT.format(dateTime);
    }

    /**
     * 获取指定日期的开始时刻（00:00:00）
     */
    public static LocalDateTime startOfDay(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDate().atStartOfDay();
    }

    /**
     * 获取指定日期的结束时刻（23:59:59.999999999）
     */
    public static LocalDateTime endOfDay(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDate().atTime(23, 59, 59, 999999999);
    }
}
