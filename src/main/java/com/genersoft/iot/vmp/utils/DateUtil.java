package com.genersoft.iot.vmp.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**    
 * 全局时间工具类
 * @author swwheihei
 */
public class DateUtil {

	private static final String yyyy_MM_dd_T_HH_mm_ss_SSSXXX = "yyyy-MM-dd'T'HH:mm:ss";
    private static final String yyyy_MM_dd_HH_mm_ss = "yyyy-MM-dd HH:mm:ss";

    public static final SimpleDateFormat formatISO8601 = new SimpleDateFormat(yyyy_MM_dd_T_HH_mm_ss_SSSXXX, Locale.getDefault());
    public static final SimpleDateFormat format = new SimpleDateFormat(yyyy_MM_dd_HH_mm_ss, Locale.getDefault());

	public static String yyyy_MM_dd_HH_mm_ssToISO8601(String formatTime) {

        try {
            return formatISO8601.format(format.parse(formatTime));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }
	
	public static String ISO8601Toyyyy_MM_dd_HH_mm_ss(String formatTime) {

        try {
            return format.format(formatISO8601.parse(formatTime));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }
	
	public static long yyyy_MM_dd_HH_mm_ssToTimestamp(String formatTime) {
		//设置要读取的时间字符串格式
		Date date;
		try {
			date = format.parse(formatTime);
			Long timestamp=date.getTime()/1000;
			//转换为Date类
			return timestamp;
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return 0;
	}

    public static String getNow() {
        return format.format(System.currentTimeMillis());
    }
}
