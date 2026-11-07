package com.genersoft.iot.vmp.media.zlm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssistProtocolTest {

    private static final String HOST = "assist.internal.example";
    private static final int PORT = 18081;

    // 内部端点只更换协议，保留路径、查询和片段，确保回调地址不被截断。
    @Test
    void normalize_internalEndpoint_preservesAddressParts() {
        assertEquals("http://" + HOST + ":18081/base?task=1#result", AssistProtocol.normalize(
                "https://" + HOST + ":18081/base?task=1#result", HOST, PORT));
        assertEquals("http://ASSIST.INTERNAL.EXAMPLE:18081", AssistProtocol.normalize(
                "HTTPS://ASSIST.INTERNAL.EXAMPLE:18081", HOST, PORT));
    }

    // 外部端点、其他端口及带认证信息的地址不能被自动降为明文。
    @Test
    void normalize_nonMatchingEndpoint_keepsProtocol() {
        for (String endpoint : new String[]{
                "https://public.example:18081", "https://" + HOST + ":443",
                "https://" + HOST, "https://user@" + HOST + ":18081",
                "http://" + HOST + ":18081", "relative/path"}) {
            assertEquals(endpoint, AssistProtocol.normalize(endpoint, HOST, PORT), endpoint);
        }
        assertEquals("https://" + HOST + ":18081", AssistProtocol.normalize(
                "https://" + HOST + ":18081", null, PORT));
    }

    // 缺省地址不构造替代端点，非法 URL 继续显式拒绝。
    @Test
    void normalize_missingOrInvalidEndpoint_preservesBoundaryBehavior() {
        assertNull(AssistProtocol.normalize(null, HOST, PORT));
        assertThrows(IllegalArgumentException.class,
                () -> AssistProtocol.normalize("https://bad host:18081", HOST, PORT));
    }
}
