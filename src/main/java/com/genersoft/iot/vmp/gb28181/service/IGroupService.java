package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.gb28181.bean.Group;
import com.genersoft.iot.vmp.gb28181.bean.GroupTree;

import java.util.List;


public interface IGroupService {

    void add(Group group);

    boolean deleteByDeviceId(String deviceId, String groupId);
    
    /**
     * 更新区域
     */
    void update(Group group);

    List<Group> getAllChild(String parent);

    Group queryGroupByDeviceId(String regionDeviceId);

    List<GroupTree> queryForTree(String query, String parent);

    void syncFromChannel();

    boolean delete(int id);
}
