package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.vmanager.gb28181.platform.bean.ChannelReduce;

import java.util.List;

/**
 * 平台关联通道管理
 * @author lin
 */
public interface IPlatformChannelService {

    /**
     * 添加共享通道
     */
    int addChannelForGB(ParentPlatform platform, List<Integer> commonGbChannelIds);

    /**
     * 移除共享通道
     */
    int removeChannelForGB(ParentPlatform platform, List<Integer> commonGbChannelIds);
}
