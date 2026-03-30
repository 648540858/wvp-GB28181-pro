package com.genersoft.iot.vmp.common.enums;

public class MediaApp {
    public final static String GB28181 = "rtp";
    public final static String GB28181_TALK = "talk";
    public final static String GB28181_BROADCAST = "broadcast";
    public final static String JT1078 = "1078";

    public static boolean isKeywords(String app) {
        return GB28181.equals(app) || GB28181_TALK.equals(app) || GB28181_BROADCAST.equals(app) || JT1078.equals(app);
    }
}
