package com.genersoft.iot.vmp.gat1400.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateUtil {

    public static String viidDateFormat(String src) {
        if (StringUtils.isBlank(src))
            return null;
        try {
            Date date = DateUtils.parseDate(src, "yyyyMMddHHmmss");
            return DateFormatUtils.format(date, "yyyy-MM-dd HH:mm:ss");
        } catch (ParseException e) {
            return src;
        }
    }

    public static LocalDateTime parseViidDateTime(String src) {
        if (StringUtils.isBlank(src))
            return null;
        try {
            return LocalDateTime.parse(src, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    public static String extractIdDateTime(String id) {
        if (StringUtils.length(id) < 36)
            return null;
        try {
            return StringUtils.substring(id, 22, 36);
        } catch (Exception e) {
            return null;
        }
    }
}
