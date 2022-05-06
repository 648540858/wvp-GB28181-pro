package com.genersoft.iot.vmp.gb28181.task;

import javax.sip.DialogState;

/**
 * @author lin
 */
public interface ISubscribeTask extends Runnable{
    void stop();

    DialogState getDialogState();
}
