package com.genersoft.iot.vmp.gb28181.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "国标通道关联分组表ID")
public class CommonGBChannelWitchGroupChannelId extends CommonGBChannel {

    private int groupChannelId;

}
