package com.genersoft.iot.vmp.media.event.hook;

/**
 * zlm hook事件的参数
 * @author lin
 */
public class Hook {

    private HookType hookType;

    private String app;

    private String stream;

    private String mediaServerId;

    private Long expireTime;


    public static Hook getInstance(HookType hookType, String app, String stream, String mediaServerId) {
        Hook hookSubscribe = new Hook();
        hookSubscribe.setApp(app);
        hookSubscribe.setStream(stream);
        hookSubscribe.setHookType(hookType);
        hookSubscribe.setMediaServerId(mediaServerId);
        hookSubscribe.setExpireTime(System.currentTimeMillis() + 5 * 60 * 1000);
        return hookSubscribe;
    }

    public HookType getHookType() {
        return hookType;
    }

    public void setHookType(HookType hookType) {
        this.hookType = hookType;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }


    public Long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Long expireTime) {
        this.expireTime = expireTime;
    }

    public String getMediaServerId() {
        return mediaServerId;
    }

    public void setMediaServerId(String mediaServerId) {
        this.mediaServerId = mediaServerId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Hook) {
            Hook param = (Hook) obj;
            return param.getHookType().equals(this.hookType)
                    && param.getApp().equals(this.app)
                    && param.getStream().equals(this.stream)
                    && param.getMediaServerId().equals(this.mediaServerId);
        }else {
            return false;
        }
    }

    @Override
    public String toString() {
        return this.getHookType() + this.getApp() + this.getStream() + this.getMediaServerId();
    }
}
