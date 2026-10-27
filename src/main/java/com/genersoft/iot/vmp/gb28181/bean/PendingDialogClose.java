package com.genersoft.iot.vmp.gb28181.bean;

import lombok.Data;

/** 保存录像会话的结束请求，媒体关闭不等于设备已确认结束。 */
@Data
public class PendingDialogClose {
    public static final String PENDING = "pending";
    public static final String UNKNOWN = "unknown";
    public static final String CONFIRMED = "confirmed";
    public static final String ABSENT = "absent";

    private SsrcTransaction transaction;
    private String channelDeviceId;
    // 仅保存构造BYE必需的路由，不复制设备密码或其他管理凭据。
    private String remoteHost;
    private String localIp;
    private String transport;
    private String attemptId;
    private int attempts;
    private long updatedAt;
    private String state;
    private Integer responseCode;
}
