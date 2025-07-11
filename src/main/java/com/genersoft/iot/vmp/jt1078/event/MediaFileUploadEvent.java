package com.genersoft.iot.vmp.jt1078.event;

import com.genersoft.iot.vmp.jt1078.bean.JTMediaEventInfo;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

public class MediaFileUploadEvent extends ApplicationEvent {

    public MediaFileUploadEvent(Object source) {
        super(source);
    }

    @Getter
    @Setter
    private JTMediaEventInfo mediaEventInfo;

    @Getter
    @Setter
    private byte[] bytes;
}
