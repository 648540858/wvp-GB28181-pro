package com.genersoft.iot.vmp.conf.trace;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从报警描述中提取链路ID。
 * <p>
 * 优先使用描述里的雪花ID（15~20 位数字，取最后一个），
 * 提取不到时使用描述内容的 MD5 前 16 位，保证 A/B 两端对同一描述得到相同结果。
 */
public final class TraceIdUtil {

    private static final Pattern SNOWFLAKE_PATTERN = Pattern.compile("(\\d{15,20})");

    private TraceIdUtil() {
    }

    public static String extract(String description) {
        if (description == null || description.isEmpty()) {
            return UUID.randomUUID().toString();
        }
        Matcher matcher = SNOWFLAKE_PATTERN.matcher(description);
        String last = null;
        while (matcher.find()) {
            last = matcher.group(1);
        }
        if (last != null) {
            return last;
        }
        return md5(description);
    }

    private static String md5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.substring(0, 16);
        } catch (Exception e) {
            return Integer.toHexString(text.hashCode());
        }
    }
}
