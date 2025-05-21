package com.genersoft.iot.vmp.gb28181.task.platformStatus;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 平台注册任务可序列化的信息
 */
@Slf4j
@Data
public class PlatformRegisterTaskInfo{

    private String platformServerId;

    private SipTransactionInfo sipTransactionInfo;


}
