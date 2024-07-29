package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface IGbChannelService {

    CommonGBChannel queryByDeviceId(String gbDeviceId);

    int add(CommonGBChannel commonGBChannel);

    int delete(int gbId);

    void delete(List<CommonGBChannel> commonGBChannelList);

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
}
