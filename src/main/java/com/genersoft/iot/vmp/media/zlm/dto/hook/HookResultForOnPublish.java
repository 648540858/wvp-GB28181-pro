package com.genersoft.iot.vmp.media.zlm.dto.hook;

import com.genersoft.iot.vmp.media.bean.ResultForOnPublish;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class HookResultForOnPublish extends HookResult{

    private boolean enable_audio;
    private boolean enable_mp4;
    private int mp4_max_second;
    private String mp4_save_path;
    private String stream_replace;
    private Integer modify_stamp;

    public HookResultForOnPublish() {
    }

    public static HookResultForOnPublish SUCCESS(){
        return new HookResultForOnPublish(0, "success");
    }

    public static HookResultForOnPublish getInstance(ResultForOnPublish resultForOnPublish){
        HookResultForOnPublish successResult = new HookResultForOnPublish(0, "success");
        successResult.setEnable_audio(resultForOnPublish.isEnable_audio());
        successResult.setEnable_mp4(resultForOnPublish.isEnable_mp4());
        successResult.setModify_stamp(resultForOnPublish.getModify_stamp());
        successResult.setStream_replace(resultForOnPublish.getStream_replace());
        successResult.setMp4_max_second(resultForOnPublish.getMp4_max_second());
        successResult.setMp4_save_path(resultForOnPublish.getMp4_save_path());
        return successResult;
    }

    public HookResultForOnPublish(int code, String msg) {
        setCode(code);
        setMsg(msg);
    }

    @Override
    public String toString() {
        return "HookResultForOnPublish{" +
                "enable_audio=" + enable_audio +
                ", enable_mp4=" + enable_mp4 +
                ", mp4_max_second=" + mp4_max_second +
                ", mp4_save_path='" + mp4_save_path + '\'' +
                ", stream_replace='" + stream_replace + '\'' +
                ", modify_stamp='" + modify_stamp + '\'' +
                '}';
    }
}
