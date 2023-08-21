package com.genersoft.iot.vmp.media.zlm.dto.hook;

/**
 * zlm hook事件中的on_record_mp4和on_record_ts事件的参数
 * @author AlphaWu
 */
public class OnRecordHookParam extends HookParam{
    private String app;
    private String file_name;
    private String file_path;
    private int file_size;
    private String folder;
    private int start_time;
    private String stream;
    private double time_len;
    private String url;
    private String vhost;

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
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

    public int getFile_size() {
        return file_size;
    }

    public void setFile_size(int file_size) {
        this.file_size = file_size;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public int getStart_time() {
        return start_time;
    }

    public void setStart_time(int start_time) {
        this.start_time = start_time;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public double getTime_len() {
        return time_len;
    }

    public void setTime_len(double time_len) {
        this.time_len = time_len;
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

    @Override
    public String toString() {
        return "OnRecordHookParam{" +
                "mediaServerId='" + super.getMediaServerId() + '\'' +
                ", app='" + app + '\'' +
                ", file_name='" + file_name + '\'' +
                ", file_path='" + file_path + '\'' +
                ", file_size=" + file_size +
                ", folder='" + folder + '\'' +
                ", start_time=" + start_time +
                ", stream='" + stream + '\'' +
                ", time_len=" + time_len +
                ", url='" + url + '\'' +
                ", vhost='" + vhost + '\'' +
                '}';
    }
}
