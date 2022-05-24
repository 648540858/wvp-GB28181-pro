package com.genersoft.iot.vmp.utils;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

import java.util.Locale;

/**    
 * 全局时间工具类
 * @author lin
 */
public class DateUtil {

	private static final String yyyy_MM_dd_T_HH_mm_ss_SSSXXX = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String yyyy_MM_dd_HH_mm_ss = "yyyy-MM-dd HH:mm:ss";



    public static final DateTimeFormatter formatterISO8601 = DateTimeFormatter.ofPattern(yyyy_MM_dd_T_HH_mm_ss_SSSXXX, Locale.getDefault()).withZone(ZoneId.systemDefault());
    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(yyyy_MM_dd_HH_mm_ss, Locale.getDefault()).withZone(ZoneId.systemDefault());

	public static String yyyy_MM_dd_HH_mm_ssToISO8601(String formatTime) {
        SimpleDateFormat formatISO8601 = new SimpleDateFormat(yyyy_MM_dd_T_HH_mm_ss_SSSXXX, Locale.getDefault());
        SimpleDateFormat format = new SimpleDateFormat(yyyy_MM_dd_HH_mm_ss, Locale.getDefault());
        try {
            return formatISO8601.format(format.parse(formatTime));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }
	
	public static String ISO8601Toyyyy_MM_dd_HH_mm_ss(String formatTime) {
	    SimpleDateFormat formatISO8601 = new SimpleDateFormat(yyyy_MM_dd_T_HH_mm_ss_SSSXXX, Locale.getDefault());
        SimpleDateFormat format = new SimpleDateFormat(yyyy_MM_dd_HH_mm_ss, Locale.getDefault());
        try {
            return format.format(formatISO8601.parse(formatTime));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";

    }
	
	public static long yyyy_MM_dd_HH_mm_ssToTimestamp(String formatTime) {
        TemporalAccessor temporalAccessor = formatter.parse(formatTime);
        Instant instant = Instant.from(temporalAccessor);
        return instant.getEpochSecond();
	}

    public static String getNow() {
        LocalDateTime nowDateTime = LocalDateTime.now();
        return formatter.format(nowDateTime);
    }

    public static boolean verification(String timeStr, DateTimeFormatter dateTimeFormatter) {
        try {
            LocalDate.parse(timeStr, dateTimeFormatter);
            return true;
        }catch (DateTimeParseException exception) {
            return false;
        }
    }
}
