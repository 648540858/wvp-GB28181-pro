package com.genersoft.iot.vmp.gb28181.task.deviceSubscribe;

import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import lombok.Data;

@Data
public class SubscribeTaskInfo {

    private String deviceId;

    private SipTransactionInfo transactionInfo;

    private String name;

    private String key;

    /**
     * 过期时间，单位： 秒
     */
    private long expireTime;

}
