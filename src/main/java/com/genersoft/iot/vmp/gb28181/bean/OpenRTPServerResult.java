package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.media.event.hook.HookData;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import lombok.Data;

@Data
public class OpenRTPServerResult {

    private SSRCInfo ssrcInfo;
    private HookData hookData;
}
