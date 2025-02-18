package com.genersoft.iot.vmp.utils;


import org.apache.commons.lang3.ObjectUtils;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;

import java.util.Locale;

/**    
 * 全局时间工具类
 * @author lin
 */
public class DateUtil {

    /**
     * 兼容不规范的iso8601时间格式
     */
	private static final String ISO8601_COMPATIBLE_PATTERN = "yyyy-M-d'T'H:m:s";

    /**
     * 用以输出标准的iso8601时间格式
     */
	private static final String ISO8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    /**
     * iso8601时间格式带时区，例如：2024-02-21T11:10:36+08:00
     */
    private static final String ISO8601_ZONE_PATTERN = "yyyy-MM-dd'T'HH:mm:ssXXX";

    /**
     * 兼容的时间格式 iso8601时间格式带毫秒
     */
    private static final String ISO8601_MILLISECOND_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    /**
     * wvp内部统一时间格式
     */
    public static final String PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * wvp内部统一时间格式
     */
    public static final String URL_PATTERN = "yyyyMMddHHmmss";

    /**
     * 日期格式
     */
    public static final String date_PATTERN = "yyyy-MM-dd";

    public static final String zoneStr = "Asia/Shanghai";

    public static final DateTimeFormatter formatterCompatibleISO8601 = DateTimeFormatter.ofPattern(ISO8601_COMPATIBLE_PATTERN, Locale.getDefault()).withZone(ZoneId.of(zoneStr));
    public static final DateTimeFormatter formatterISO8601 = DateTimeFormatter.ofPattern(ISO8601_PATTERN, Locale.getDefault()).withZone(ZoneId.of(zoneStr));
    public static final DateTimeFormatter formatterZoneISO8601 = DateTimeFormatter.ofPattern(ISO8601_ZONE_PATTERN, Locale.getDefault()).withZone(ZoneId.of(zoneStr));
    public static final DateTimeFormatter formatterMillisecondISO8601 = DateTimeFormatter.ofPattern(ISO8601_MILLISECOND_PATTERN, Locale.getDefault()).withZone(ZoneId.of(zoneStr));

    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PATTERN, Locale.getDefault()).withZone(ZoneId.of(zoneStr));
    public static final DateTimeFormatter DateFormatter = DateTimeFormatter.ofPattern(date_PATTERN, Locale.getDefault()).withZone(ZoneId.of(zoneStr));
    public static final DateTimeFormatter urlFormatter = DateTimeFormatter.ofPattern(URL_PATTERN, Locale.getDefault()).withZone(ZoneId.of(zoneStr));

	public static String yyyy_MM_dd_HH_mm_ssToISO8601(@NotNull String formatTime) {
        return formatterISO8601.format(formatter.parse(formatTime));
    }
	
	public static String ISO8601Toyyyy_MM_dd_HH_mm_ss(String formatTime) {
        // 三种日期格式都尝试，为了兼容不同厂家的日期格式
        if (verification(formatTime, formatterCompatibleISO8601)) {
            return formatter.format(formatterCompatibleISO8601.parse(formatTime));
        } else if (verification(formatTime, formatterZoneISO8601)) {
            return formatter.format(formatterZoneISO8601.parse(formatTime));
        } else if (verification(formatTime, formatterMillisecondISO8601)) {
            return formatter.format(formatterMillisecondISO8601.parse(formatTime));
        }
        return formatter.format(formatterISO8601.parse(formatTime));
    }

	public static String urlToyyyy_MM_dd_HH_mm_ss(String formatTime) {
        return formatter.format(urlFormatter.parse(formatTime));
    }

    /**
     * yyyy_MM_dd_HH_mm_ss 转时间戳
     * @param formatTime
     * @return
     */
	public static long yyyy_MM_dd_HH_mm_ssToTimestamp(String formatTime) {
        TemporalAccessor temporalAccessor = formatter.parse(formatTime);
        Instant instant = Instant.from(temporalAccessor);
        return instant.getEpochSecond();
	}

    /**
     * 时间戳 转 yyyy_MM_dd_HH_mm_ss
     */
	public static String timestampTo_yyyy_MM_dd_HH_mm_ss(long timestamp) {
        Instant instant = Instant.ofEpochSecond(timestamp);
        return formatter.format(LocalDateTime.ofInstant(instant, ZoneId.of(zoneStr)));
	}

    /**
     * 时间戳 转 yyyy_MM_dd_HH_mm_ss
     */
	public static String timestampMsToUrlToyyyy_MM_dd_HH_mm_ss(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        return urlFormatter.format(LocalDateTime.ofInstant(instant, ZoneId.of(zoneStr)));
	}

    /**
     * yyyy_MM_dd_HH_mm_ss 转时间戳（毫秒）
     *
     * @param formatTime
     * @return
     */
    public static long yyyy_MM_dd_HH_mm_ssToTimestampMs(String formatTime) {
        TemporalAccessor temporalAccessor = formatter.parse(formatTime);
        Instant instant = Instant.from(temporalAccessor);
        return instant.toEpochMilli();
    }

    /**
     * 时间戳（毫秒） 转 yyyy_MM_dd_HH_mm_ss
     */
    public static String timestampMsTo_yyyy_MM_dd_HH_mm_ss(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        return formatter.format(LocalDateTime.ofInstant(instant, ZoneId.of(zoneStr)));
    }

    /**
     * 时间戳 转 yyyy_MM_dd
     */
    public static String timestampTo_yyyy_MM_dd(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        return DateFormatter.format(LocalDateTime.ofInstant(instant, ZoneId.of(zoneStr)));
    }

    /**
     * 获取当前时间
     * @return
     */
    public static String getNow() {
        LocalDateTime nowDateTime = LocalDateTime.now();
        return formatter.format(nowDateTime);
    }

    /**
     * 获取当前时间
     * @return
     */
    public static String getNowForUrl() {
        LocalDateTime nowDateTime = LocalDateTime.now();
        return urlFormatter.format(nowDateTime);
    }


    /**
     * 格式校验
     * @param timeStr 时间字符串
     * @param dateTimeFormatter 待校验的格式
     * @return
     */
    public static boolean verification(String timeStr, DateTimeFormatter dateTimeFormatter) {
        try {
            LocalDate.parse(timeStr, dateTimeFormatter);
            return true;
        }catch (DateTimeParseException exception) {
            return false;
        }
    }

    public static String getNowForISO8601() {
        LocalDateTime nowDateTime = LocalDateTime.now();
        return formatterISO8601.format(nowDateTime);
    }

    public static long getDifferenceForNow(String keepaliveTime) {
        if (ObjectUtils.isEmpty(keepaliveTime)) {
            return 0;
        }
        Instant beforeInstant = Instant.from(formatter.parse(keepaliveTime));
        return ChronoUnit.MILLIS.between(beforeInstant, Instant.now());
    }

    public static long getDifference(String startTime, String endTime) {
        if (ObjectUtils.isEmpty(startTime) || ObjectUtils.isEmpty(endTime)) {
            return 0;
        }
        Instant startInstant = Instant.from(formatter.parse(startTime));
        Instant endInstant = Instant.from(formatter.parse(endTime));
        return ChronoUnit.MILLIS.between(endInstant, startInstant);
    }
}
