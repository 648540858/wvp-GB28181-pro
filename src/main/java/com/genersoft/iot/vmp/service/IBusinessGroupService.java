package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.service.bean.BusinessGroup;

import java.util.List;

/**
 * 业务分组
 */
public interface IBusinessGroupService {

    /**
     * 查询业务分组
     */
    List<BusinessGroup> getNodes(String parentId);

    /**
     * 查询业务分组下的通道
     */
    List<CommonGbChannel> getChannels(int id);

    /**
     * 查询业务分组下的通道
     */
    List<CommonGbChannel> getChannels(String deviceId);

    /**
     * 添加业务分组
     */
    boolean add(BusinessGroup businessGroup);

    /**
     * 移除业务分组
     */
    boolean remove(int id);

    /**
     * 移除业务分组
     */
    boolean remove(String deviceId);

    /**
     * 更新业务分组
     */
    boolean update(BusinessGroup businessGroup);

    /**
     * 设置国标设备到相关的分组中
     */
    boolean updateChannelsToBusinessGroup(int id, List<CommonGbChannel> channels);

    /**
     * 设置国标设备到相关的分组中
     */
    boolean updateChannelsToBusinessGroup(String deviceId, List<CommonGbChannel> channels);

    /**
     * 移除分组分组中的通道
     */
    boolean removeChannelsFromBusinessGroup(List<CommonGbChannel> channels);


}
