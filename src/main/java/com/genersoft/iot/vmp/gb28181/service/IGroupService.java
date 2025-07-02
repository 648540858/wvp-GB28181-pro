package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.gb28181.bean.Group;
import com.genersoft.iot.vmp.gb28181.bean.GroupTree;
import com.github.pagehelper.PageInfo;

import java.util.List;


public interface IGroupService {

    void add(Group group);

    void update(Group group);

    Group queryGroupByDeviceId(String regionDeviceId);

    List<GroupTree> queryForTree(String query, Integer parent, Boolean hasChannel);

    void syncFromChannel();

    boolean delete(int id);

    boolean batchAdd(List<Group> groupList);

    List<Group> getPath(String deviceId, String businessGroup);

    PageInfo<Group> queryList(Integer page, Integer count, String query);
}
