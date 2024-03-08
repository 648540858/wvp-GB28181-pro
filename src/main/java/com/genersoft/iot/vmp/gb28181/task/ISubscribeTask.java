package com.genersoft.iot.vmp.gb28181.task;

import com.genersoft.iot.vmp.common.CommonCallback;

/**
 * @author lin
 */
public interface ISubscribeTask extends Runnable{
    void stop(CommonCallback<Boolean> callback);
}
