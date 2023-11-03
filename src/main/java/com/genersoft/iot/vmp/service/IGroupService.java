package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.service.bean.Group;
import com.github.pagehelper.PageInfo;

import java.util.List;

/**
 * 业务分组
 */
public interface IGroupService {

    /**
     * 查询业务分组
     */
    List<Group> getNodes(String parentId);

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
    boolean add(Group group);

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
    boolean update(Group group);

    /**
     * 设置国标设备到相关的分组中
     */
    boolean updateChannelsToGroup(int id, List<CommonGbChannel> channels);

    /**
     * 设置国标设备到相关的分组中
     */
    boolean updateChannelsToGroup(String deviceId, List<CommonGbChannel> channels);

    /**
     * 移除分组分组中的通道
     */
    boolean removeChannelsFromGroup(List<CommonGbChannel> channels);


    /**
     * 查询分组
     */
    PageInfo<Group> queryGroup(String query, int page, int count);

    /**
     * 查询子节点
     */
    PageInfo<Group> queryChildGroupList(String groupParentId, int page, int count);
}
