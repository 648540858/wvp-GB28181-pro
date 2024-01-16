package com.genersoft.iot.vmp.media.zlm.dto.hook;

/**
 * zlm hook事件中的on_rtp_server_timeout事件的参数
 * @author lin
 */
public class OnRecordMp4HookParam extends HookParam{
    private String app;
    private String stream;
    private String file_name;
    private String file_path;
    private long file_size;
    private String folder;
    private String url;
    private String vhost;
    private long start_time;
    private double time_len;

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

    public String getFile_name() {
        return file_name;
    }

    public void setFile_name(String file_name) {
        this.file_name = file_name;
    }

    public String getFile_path() {
        return file_path;
    }

    public void setFile_path(String file_path) {
        this.file_path = file_path;
    }

    public long getFile_size() {
        return file_size;
    }

    public void setFile_size(long file_size) {
        this.file_size = file_size;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getVhost() {
        return vhost;
    }

    public void setVhost(String vhost) {
        this.vhost = vhost;
    }

    public long getStart_time() {
        return start_time;
    }

    public void setStart_time(long start_time) {
        this.start_time = start_time;
    }

    public double getTime_len() {
        return time_len;
    }

    public void setTime_len(double time_len) {
        this.time_len = time_len;
    }

    @Override
    public String toString() {
        return "OnRecordMp4HookParam{" +
                "app='" + app + '\'' +
                ", stream='" + stream + '\'' +
                ", file_name='" + file_name + '\'' +
                ", file_path='" + file_path + '\'' +
                ", file_size='" + file_size + '\'' +
                ", folder='" + folder + '\'' +
                ", url='" + url + '\'' +
                ", vhost='" + vhost + '\'' +
                ", start_time=" + start_time +
                ", time_len=" + time_len +
                '}';
    }
}
