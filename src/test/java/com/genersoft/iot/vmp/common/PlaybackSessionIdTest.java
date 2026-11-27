package com.genersoft.iot.vmp.common;

import com.genersoft.iot.vmp.conf.exception.ControllerException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlaybackSessionIdTest {
    /** 未指定会话身份时保持旧集成的共享回放，避免设备额外建立第二路回放。 */
    @Test
    void missingIdentityKeepsSharedPlayback() {
        assertNull(PlaybackSessionId.resolve(null));
    }

    /** 大小写与 UUID 分隔符不同的同一会话必须得到相同幂等键。 */
    @Test
    void normalizesExplicitIdentity() {
        assertEquals("0123456789abcdef0123456789abcdef",
                PlaybackSessionId.resolve("01234567-89AB-CDEF-0123-456789ABCDEF"));
        assertEquals("0123456789abcdef0123456789abcdef",
                PlaybackSessionId.normalize("0123456789abcdef0123456789abcdef"));
    }

    /** 拒绝空身份、路径字符和非规范 UUID，不能把外部参数拼入媒体路径。 */
    @Test
    void rejectsMalformedIdentity() {
        for (String invalid : new String[]{"", " ", "../other", "1-1-1-1-1", "0123456789abcdef0123456789abcdef_"}) {
            assertThrows(ControllerException.class, () -> PlaybackSessionId.resolve(invalid));
        }
    }
}
