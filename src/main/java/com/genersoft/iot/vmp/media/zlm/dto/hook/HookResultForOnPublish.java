package com.genersoft.iot.vmp.media.zlm.dto.hook;

public class HookResultForOnPublish extends HookResult{

    private boolean enable_audio;
    private boolean enable_mp4;
    private int mp4_max_second;

    public HookResultForOnPublish() {
    }

    public static HookResultForOnPublish SUCCESS(){
        return new HookResultForOnPublish(0, "success");
    }

    public HookResultForOnPublish(int code, String msg) {
        setCode(code);
        setMsg(msg);
    }

    public boolean isEnable_audio() {
        return enable_audio;
    }

    public void setEnable_audio(boolean enable_audio) {
        this.enable_audio = enable_audio;
    }

    public boolean isEnable_mp4() {
        return enable_mp4;
    }

    public void setEnable_mp4(boolean enable_mp4) {
        this.enable_mp4 = enable_mp4;
    }

    public int getMp4_max_second() {
        return mp4_max_second;
    }

    public void setMp4_max_second(int mp4_max_second) {
        this.mp4_max_second = mp4_max_second;
    }
}
