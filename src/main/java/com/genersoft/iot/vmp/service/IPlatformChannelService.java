package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;

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

    /**
     * 在一个给定的范围内查出分享了这个通道的上级平台
     */
    List<ParentPlatform> querySharePlatformListByChannelId(int commonGbId, List<String> platforms);

    /**
     * 查询关联了上级平台的所有通道
     */
    List<CommonGbChannel> queryChannelList(ParentPlatform platform);

    /**
     * 查询通道
     */
    CommonGbChannel queryChannelByPlatformIdAndChannelDeviceId(Integer platformId, String channelId);
}
