package com.genersoft.iot.vmp.common.enums;

public class MediaStreamUtil {
    public final static String RTP_APP = "rtp";
    public final static String RTP_STREAM_REST_PREFIX = "s";

    public final static String GB28181_TALK = "talk";
    public final static String GB28181_BROADCAST = "broadcast";

    public final static String JT_TALK = "jt_talk";
    public final static String JT1078_STREAM_PREFIX = RTP_STREAM_REST_PREFIX + "_jt";
    public final static String JT1078_STREAM_PLAY_PREFIX = RTP_STREAM_REST_PREFIX + "_jt_play";
    public final static String JT1078_STREAM_PLAYBACK_PREFIX = RTP_STREAM_REST_PREFIX + "_jt_playback";

    public static boolean isKeywords(String app) {
        return RTP_APP.equals(app) || GB28181_TALK.equals(app) || GB28181_BROADCAST.equals(app);
    }

    public static boolean isGB28181(String app, String streamId) {
        return RTP_APP.equals(app) && !streamId.startsWith(RTP_STREAM_REST_PREFIX);
    }

    public static boolean isTalk(String app, String streamId) {
        return GB28181_TALK.equals(app);
    }

     public static boolean isBroadcast(String app, String streamId) {
        return GB28181_BROADCAST.equals(app);
    }

     public static boolean isJT1078(String app, String streamId) {
        return RTP_APP.equals(app) || streamId.startsWith(JT1078_STREAM_PREFIX);
    }

    public static String getJTPlayStreamId(String phoneNumber, int channelId) {
        return String.format("%s_%s_%s", JT1078_STREAM_PLAY_PREFIX, phoneNumber, channelId);
    }

    public static boolean isJT1078Play(String app, String stream) {
        return RTP_APP.equals(app) || stream.startsWith(JT1078_STREAM_PLAY_PREFIX);
    }

    public static boolean isJT1078Playback(String app, String stream) {
        return RTP_APP.equals(app) || stream.startsWith(JT1078_STREAM_PLAYBACK_PREFIX);
    }

    public static boolean isJT1078Talk(String app, String stream) {
        return JT_TALK.equals(app);
    }

    public static String getJTPlaybackStreamId(String phoneNumber, Integer channelId, String startTime, String endTime) {
        return String.format("%s_%s_%s_%s_%s", JT1078_STREAM_PLAYBACK_PREFIX, phoneNumber, channelId, startTime, endTime);
    }

    public static String getJTTalkStreamId(String phoneNumber, Integer channelId) {
        return String.format("%s_%s_%s", JT_TALK, phoneNumber, channelId);
    }

    public static String getJTTalkReceiveStreamId(String phoneNumber, Integer channelId) {
        return getJTTalkStreamId(phoneNumber, channelId) + "_receive";
    }
}
