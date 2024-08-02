package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.github.pagehelper.PageInfo;

import java.util.Collection;
import java.util.List;

public interface IGbChannelService {

    CommonGBChannel queryByDeviceId(String gbDeviceId);

    int add(CommonGBChannel commonGBChannel);

    int delete(int gbId);

    void delete(Collection<Integer> ids);

    int update(CommonGBChannel commonGBChannel);

    int offline(CommonGBChannel commonGBChannel);

    int offline(List<CommonGBChannel> commonGBChannelList);

    int online(CommonGBChannel commonGBChannel);

    int online(List<CommonGBChannel> commonGBChannelList);

    void batchAdd(List<CommonGBChannel> commonGBChannels);

    void updateStatus(List<CommonGBChannel> channelList);

    List<CommonGBChannel> queryByPlatformId(Integer platformId);

    CommonGBChannel getOne(int id);

    List<IndustryCodeType> getIndustryCodeList();

    List<DeviceType> getDeviceTypeList();

    List<NetworkIdentificationType> getNetworkIdentificationTypeList();

    void reset(int id);

    PageInfo<CommonGBChannel> queryList(int page, int count, String query, Boolean online, Boolean hasCivilCode);

    void removeCivilCode(List<Region> allChildren);

    void addChannelToRegion(String civilCode, List<Integer> channelIds);

    void deleteChannelToRegion(String civilCode, List<Integer> channelIds);

    void deleteChannelToRegionByCivilCode(String civilCode);

    void deleteChannelToRegionByChannelIds(List<Integer> channelIds);

    void addChannelToRegionBYGbDevice(String civilCode, List<Integer> deviceIds);

    void deleteChannelToRegionBYGbDevice(List<Integer> deviceIds);

    void removeParentIdByBusinessGroup(String businessGroup);

    void removeParentIdByGroupList(List<Group> groupList);

    void updateBusinessGroup(String oldBusinessGroup, String newBusinessGroup);

    void updateParentIdGroup(String oldParentId, String newParentId);
}
