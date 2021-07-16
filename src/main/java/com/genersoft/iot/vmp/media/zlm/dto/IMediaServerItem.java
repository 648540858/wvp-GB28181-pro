package com.genersoft.iot.vmp.media.zlm.dto;

public interface IMediaServerItem {

    String getId();

    void setId(String id);

    String getIp();

    void setIp(String ip);

    String getHookIp();

    void setHookIp(String hookIp);

    String getSdpIp();

    void setSdpIp(String sdpIp);

    String getStreamIp();

    void setStreamIp(String streamIp);

    int getHttpPort();

    void setHttpPort(int httpPort);

    int getHttpSSlPort();

    void setHttpSSlPort(int httpSSlPort);

    int getRtmpPort();

    void setRtmpPort(int rtmpPort);

    int getRtmpSSlPort();

    void setRtmpSSlPort(int rtmpSSlPort);

    int getRtpProxyPort();

    void setRtpProxyPort(int rtpProxyPort);

    int getRtspPort();

    void setRtspPort(int rtspPort);

    int getRtspSSLPort();

    void setRtspSSLPort(int rtspSSLPort);

    boolean isAutoConfig();

    void setAutoConfig(boolean autoConfig);

    String getSecret();

    void setSecret(String secret);

    String getStreamNoneReaderDelayMS();

    void setStreamNoneReaderDelayMS(String streamNoneReaderDelayMS);

    boolean isRtpEnable();

    void setRtpEnable(boolean rtpEnable);

    String getRtpPortRange();

    void setRtpPortRange(String rtpPortRange);

    int getRecordAssistPort();

    void setRecordAssistPort(int recordAssistPort);

    boolean isDocker();

    void setDocker(boolean docker);

    String getUpdateTime();

    void setUpdateTime(String updateTime);

    String getCreateTime();

    void setCreateTime(String createTime);

    int getCount();

    void setCount(int count);
}
