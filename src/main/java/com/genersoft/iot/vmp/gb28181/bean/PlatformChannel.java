package com.genersoft.iot.vmp.gb28181.bean;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PlatformChannel extends CommonGBChannel{
    private int platformId;
}
