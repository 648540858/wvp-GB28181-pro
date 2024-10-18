package com.genersoft.iot.vmp.gb28181.controller.bean;

import lombok.Data;

import java.util.List;

@Data
public class ChannelToRegionParam {

    private String civilCode;
    private List<Integer> channelIds;

}
