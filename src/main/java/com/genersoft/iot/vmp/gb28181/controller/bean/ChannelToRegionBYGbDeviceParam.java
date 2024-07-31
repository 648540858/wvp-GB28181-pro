package com.genersoft.iot.vmp.gb28181.controller.bean;

import lombok.Data;

import java.util.List;

@Data
public class ChannelToRegionBYGbDeviceParam {
    private List<Integer> deviceIds;
    private String civilCode;
}
