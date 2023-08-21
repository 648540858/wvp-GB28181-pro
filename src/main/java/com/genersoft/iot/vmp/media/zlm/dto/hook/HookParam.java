package com.genersoft.iot.vmp.media.zlm.dto.hook;

/**
 * zlm hook事件的参数
 * @author lin
 */
public class HookParam {
    private String mediaServerId;
    private int hook_index;
    public String getMediaServerId() {
        return mediaServerId;
    }

    public void setMediaServerId(String mediaServerId) {
        this.mediaServerId = mediaServerId;
    }

    public int getHook_index() {
        return hook_index;
    }

    public void setHook_index(int hook_index) {
        this.hook_index = hook_index;
    }
}
