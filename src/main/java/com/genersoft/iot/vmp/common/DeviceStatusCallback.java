package com.genersoft.iot.vmp.common;

import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;

public interface DeviceStatusCallback {
    public void run(String deviceId, SipTransactionInfo transactionInfo);
}
