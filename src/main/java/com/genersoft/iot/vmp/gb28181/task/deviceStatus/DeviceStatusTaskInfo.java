package com.genersoft.iot.vmp.gb28181.task.deviceStatus;

import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import lombok.Data;

@Data
public class DeviceStatusTaskInfo{

    private String deviceId;

    private SipTransactionInfo transactionInfo;

    /**
     * 过期时间
     */
    private long expireTime;
}
