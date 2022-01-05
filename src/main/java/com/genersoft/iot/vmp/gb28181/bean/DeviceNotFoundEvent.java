package com.genersoft.iot.vmp.gb28181.bean;

import javax.sip.Dialog;
import java.util.EventObject;

public class DeviceNotFoundEvent extends EventObject {
    /**
     * Constructs a prototypical Event.
     *
     * @param dialog
     * @throws IllegalArgumentException if source is null.
     */
    public DeviceNotFoundEvent(Dialog dialog) {
        super(dialog);
    }


    public Dialog getDialog() {
        return (Dialog)super.getSource();
    }
}
