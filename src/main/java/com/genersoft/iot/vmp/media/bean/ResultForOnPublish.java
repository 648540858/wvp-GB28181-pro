package com.genersoft.iot.vmp.media.bean;

public class ResultForOnPublish {

    private boolean enable_audio;
    private boolean enable_mp4;
    private int mp4_max_second;
    private String mp4_save_path;
    private String stream_replace;
    private Integer modify_stamp;

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

    public String getMp4_save_path() {
        return mp4_save_path;
    }

    public void setMp4_save_path(String mp4_save_path) {
        this.mp4_save_path = mp4_save_path;
    }

    public String getStream_replace() {
        return stream_replace;
    }

    public void setStream_replace(String stream_replace) {
        this.stream_replace = stream_replace;
    }

    public Integer getModify_stamp() {
        return modify_stamp;
    }

    public void setModify_stamp(Integer modify_stamp) {
        this.modify_stamp = modify_stamp;
    }
}
