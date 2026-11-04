package com.genersoft.iot.vmp.common;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;

import java.util.Locale;
import java.util.regex.Pattern;

/** 约束回放会话键，避免播放器共享控制状态或把任意路径字符带入媒体流名。 */
public final class PlaybackSessionId {
    private static final Pattern FORMAT = Pattern.compile(
            "[0-9a-fA-F]{32}|[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    /** 工具类不提供实例。 */
    private PlaybackSessionId() {
    }

    /** 未指定身份时保持共享回放，只有显式身份才要求设备建立独立会话。 */
    public static String resolve(String requested) {
        // 旧集成依赖同区间共享媒体，不能默认要求设备支持并发回放。
        return requested == null ? null : normalize(requested);
    }

    /** 只允许规范 UUID，统一大小写和分隔符后形成固定长度的流名会话字段。 */
    public static String normalize(String value) {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new ControllerException(ErrorCode.ERROR400.getCode(), "sessionId 必须是有效的 UUID");
        }
        return value.replace("-", "").toLowerCase(Locale.ROOT);
    }
}
