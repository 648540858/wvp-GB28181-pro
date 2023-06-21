package com.genersoft.iot.vmp.media.zlm.dto.hook;

public class OnStreamNoneReaderHookParam extends HookParam{

    private String schema;
    private String app;
    private String stream;
    private String vhost;

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
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

    public String getVhost() {
        return vhost;
    }

    public void setVhost(String vhost) {
        this.vhost = vhost;
    }

    @Override
    public String toString() {
        return "OnStreamNoneReaderHookParam{" +
                "schema='" + schema + '\'' +
                ", app='" + app + '\'' +
                ", stream='" + stream + '\'' +
                ", vhost='" + vhost + '\'' +
                '}';
    }
}
