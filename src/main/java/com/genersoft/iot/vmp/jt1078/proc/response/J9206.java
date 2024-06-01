package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;

/**
 * 文件上传指令
 *
 */
@MsgId(id = "9206")
public class J9206 extends Rs {

    // 服务器地址
    private String serverIp;
    // 服务器端口
    private int port;
    // 用户名
    private String username;
    // 密码
    private String password;
    // 文件上传路径
    private String path;
    // 逻辑通道号
    private int channelId;

    // 开始时间YYMMDDHHMMSS,全0表示无起始时间
    private String startTime;

    // 结束时间YYMMDDHHMMSS,全0表示无终止时间
    private String endTime;

    // 报警标志
    private final int warnType = 0;

    // 音视频资源类型：0.音视频 1.音频 2.视频 3.视频或音视频
    private int mediaType;

    // 码流类型：0.所有码流 1.主码流 2.子码流
    private int streamType = 0;

    // 存储器类型：0.所有存储器 1.主存储器 2.灾备存储器
    private int storageType = 0;

    // 任务执行条件，
    // 1：仅WI-FI 下可下载，
    // 2： 仅LAN 连接时可下载；
    // 3： WI-FI + LAN 连接时可下载；
    // 4： 仅3G/ 4G 连接时可下载
    // 5： WI-FI + 3G/ 4G 连接时可下载
    // 6： WI-FI + LAN 连接时可下载
    // 7： WI-FI + LAN + 3G/ 4G 连接时可下载
    private int taskConditions = 7;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();

        buffer.writeByte(serverIp.getBytes(Charset.forName("GBK")).length);
        buffer.writeCharSequence(serverIp, Charset.forName("GBK"));
        buffer.writeShort(port);
        buffer.writeByte(username.getBytes(Charset.forName("GBK")).length);
        buffer.writeCharSequence(username, Charset.forName("GBK"));
        buffer.writeByte(password.getBytes(Charset.forName("GBK")).length);
        buffer.writeCharSequence(password, Charset.forName("GBK"));
        buffer.writeByte(path.getBytes(Charset.forName("GBK")).length);
        buffer.writeCharSequence(path, Charset.forName("GBK"));
        buffer.writeByte(channelId);
        buffer.writeBytes(ByteBufUtil.decodeHexDump(startTime));
        buffer.writeBytes(ByteBufUtil.decodeHexDump(endTime));
        buffer.writeLong(warnType);
        buffer.writeByte(mediaType);
        buffer.writeByte(streamType);
        buffer.writeByte(storageType);
        buffer.writeByte(taskConditions);
        return buffer;
    }


    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public int getWarnType() {
        return warnType;
    }

    public int getMediaType() {
        return mediaType;
    }

    public void setMediaType(int mediaType) {
        this.mediaType = mediaType;
    }

    public int getStreamType() {
        return streamType;
    }

    public void setStreamType(int streamType) {
        this.streamType = streamType;
    }

    public int getStorageType() {
        return storageType;
    }

    public void setStorageType(int storageType) {
        this.storageType = storageType;
    }

    public int getTaskConditions() {
        return taskConditions;
    }

    public void setTaskConditions(int taskConditions) {
        this.taskConditions = taskConditions;
    }

    @Override
    public String toString() {
        return "J9206{" +
                "serverIp='" + serverIp + '\'' +
                ", port=" + port +
                ", user='" + username + '\'' +
                ", password='" + password + '\'' +
                ", path='" + path + '\'' +
                ", channelId=" + channelId +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", warnType=" + warnType +
                ", mediaType=" + mediaType +
                ", streamType=" + streamType +
                ", storageType=" + storageType +
                ", taskConditions=" + taskConditions +
                '}';
    }
}
