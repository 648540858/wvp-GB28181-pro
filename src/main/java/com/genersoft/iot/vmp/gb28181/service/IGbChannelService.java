package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.streamPush.bean.StreamPush;
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

    CommonGBChannel getOne(int id);

    List<IndustryCodeType> getIndustryCodeList();

    List<DeviceType> getDeviceTypeList();

    List<NetworkIdentificationType> getNetworkIdentificationTypeList();

    void reset(int id);

    PageInfo<CommonGBChannel> queryListByCivilCode(int page, int count, String query, Boolean online, Integer channelType, String civilCode);

    PageInfo<CommonGBChannel> queryListByParentId(int page, int count, String query, Boolean online, Integer channelType, String groupDeviceId);

    void removeCivilCode(List<Region> allChildren);

    void addChannelToRegion(String civilCode, List<Integer> channelIds);

    void deleteChannelToRegion(String civilCode, List<Integer> channelIds);

    void deleteChannelToRegionByCivilCode(String civilCode);

    void deleteChannelToRegionByChannelIds(List<Integer> channelIds);

    void addChannelToRegionByGbDevice(String civilCode, List<Integer> deviceIds);

    void deleteChannelToRegionByGbDevice(List<Integer> deviceIds);

    void removeParentIdByBusinessGroup(String businessGroup);

    void removeParentIdByGroupList(List<Group> groupList);

    void updateBusinessGroup(String oldBusinessGroup, String newBusinessGroup);

    void updateParentIdGroup(String oldParentId, String newParentId);

    void addChannelToGroup(String parentId, String businessGroup, List<Integer> channelIds);

    void deleteChannelToGroup(String parentId, String businessGroup, List<Integer> channelIds);

    void addChannelToGroupByGbDevice(String parentId, String businessGroup, List<Integer> deviceIds);

    void deleteChannelToGroupByGbDevice(List<Integer> deviceIds);

    void batchUpdate(List<CommonGBChannel> commonGBChannels);

    CommonGBChannel queryOneWithPlatform(Integer platformId, String channelDeviceId);

    void updateCivilCode(String oldCivilCode, String newCivilCode);

    List<CommonGBChannel> queryListByStreamPushList(List<StreamPush> streamPushList);

    PageInfo<CommonGBChannel> queryList(int page, int count, String query, Boolean online, Boolean hasRecordPlan, Integer channelType);

    void queryRecordInfo(CommonGBChannel channel, String startTime, String endTime, ErrorCallback<RecordInfo> callback);

    PageInfo<CommonGBChannel> queryListByCivilCodeForUnusual(int page, int count, String query, Boolean online, Integer channelType);

    void clearChannelCivilCode(Boolean all, List<Integer> channelIds);

    PageInfo<CommonGBChannel> queryListByParentForUnusual(int page, int count, String query, Boolean online, Integer channelType);

    void clearChannelParent(Boolean all, List<Integer> channelIds);

    void updateGPSFromGPSMsgInfo(List<GPSMsgInfo> gpsMsgInfoList);

}
