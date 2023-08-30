package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.service.ICommonGbChannelService;
import com.genersoft.iot.vmp.storager.dao.CommonGbChannelMapper;
import com.genersoft.iot.vmp.storager.dao.DeviceChannelMapper;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.util.ArrayList;
import java.util.List;

@Service
public class CommonGbChannelServiceImpl implements ICommonGbChannelService {

    private final static Logger logger = LoggerFactory.getLogger(CommonGbChannelServiceImpl.class);

    @Autowired
    private CommonGbChannelMapper commonGbChannelMapper;

    @Autowired
    private DeviceChannelMapper deviceChannelMapper;

    @Autowired
    DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    TransactionDefinition transactionDefinition;


    @Override
    public CommonGbChannel getChannel(String channelId) {
        return commonGbChannelMapper.queryByDeviceID(channelId);
    }

    @Override
    public int add(CommonGbChannel channel) {
        return commonGbChannelMapper.add(channel);
    }

    @Override
    public int addFromGbChannel(DeviceChannel channel) {
        CommonGbChannel commonGbChannel = commonGbChannelMapper.queryByDeviceID(channel.getChannelId());
        logger.info("[添加通用通道]来自国标通道，国标编号: {}, 同步所有字段", channel.getChannelId());
        if (commonGbChannel != null) {
            logger.info("[添加通用通道]来自国标通道，失败，已存在。国标编号: {}", channel.getChannelId());
            return 0;
        }
        CommonGbChannel commonChannelFromDeviceChannel = getCommonChannelFromDeviceChannel(channel, null);
        return commonGbChannelMapper.add(commonChannelFromDeviceChannel);
    }

    @Override
    public int delete(String channelId) {
        return commonGbChannelMapper.deleteByDeviceID(channelId);
    }

    @Override
    public int update(CommonGbChannel channel) {
        return commonGbChannelMapper.update(channel);
    }

    @Override
    public boolean checkChannelInPlatform(String channelId, String platformServerId) {
        return commonGbChannelMapper.checkChannelInPlatform(channelId, platformServerId) > 0;
    }

    @Override
    public boolean SyncChannelFromGb28181Device(String gbDeviceId, List<String> syncKeys) {
        logger.info("同步通用通道]来自国标设备，国标编号: {}", gbDeviceId);
        List<DeviceChannel> deviceChannels = deviceChannelMapper.queryAllChannels(gbDeviceId);
        if (deviceChannels.isEmpty()) {
            return false;
        }
        List<CommonGbChannel> commonGbChannelList = new ArrayList<>(deviceChannels.size());
        for (DeviceChannel deviceChannel : deviceChannels) {
            CommonGbChannel commonGbChannel = getCommonChannelFromDeviceChannel(deviceChannel, syncKeys);
            commonGbChannelList.add(commonGbChannel);
        }
        int limit = 300;
        boolean result;
        if (commonGbChannelList.size() <= limit) {
            result = commonGbChannelMapper.addAll(commonGbChannelList) > 0;
        } else {
            TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
            for (int i = 0; i < commonGbChannelList.size(); i += limit) {
                int toIndex = i + limit;
                if (i + limit > commonGbChannelList.size()) {
                    toIndex = commonGbChannelList.size();
                }
                List<CommonGbChannel> commonGbChannelListSub = commonGbChannelList.subList(i, toIndex);
                int currentResult = commonGbChannelMapper.addAll(commonGbChannelListSub);
                if (currentResult <= 0) {
                    dataSourceTransactionManager.rollback(transactionStatus);
                    return false;
                }
            }
            dataSourceTransactionManager.commit(transactionStatus);
            result = true;
        }
        return result;
    }

    private CommonGbChannel getCommonChannelFromDeviceChannel(DeviceChannel deviceChannel, List<String> syncKeys) {
        if (deviceChannel == null) {
            return null;
        }
        CommonGbChannel commonGbChannel = new CommonGbChannel();
        commonGbChannel.setCommonGbDeviceID(deviceChannel.getChannelId());
        if (syncKeys == null || syncKeys.isEmpty()) {
            commonGbChannel.setCommonGbName(deviceChannel.getName());
            commonGbChannel.setCommonGbManufacturer(deviceChannel.getManufacture());
            commonGbChannel.setCommonGbModel(deviceChannel.getModel());
            commonGbChannel.setCommonGbOwner(deviceChannel.getOwner());
            commonGbChannel.setCommonGbCivilCode(deviceChannel.getCivilCode());
            commonGbChannel.setCommonGbBlock(deviceChannel.getBlock());
            commonGbChannel.setCommonGbAddress(deviceChannel.getAddress());
            commonGbChannel.setCommonGbParental(deviceChannel.getParental());
            commonGbChannel.setCommonGbParentID(deviceChannel.getParentId());
            commonGbChannel.setCommonGbSafetyWay(deviceChannel.getSafetyWay());
            commonGbChannel.setCommonGbRegisterWay(deviceChannel.getRegisterWay());
            commonGbChannel.setCommonGbCertNum(deviceChannel.getCertNum());
            commonGbChannel.setCommonGbCertifiable(deviceChannel.getCertifiable());
            commonGbChannel.setCommonGbErrCode(deviceChannel.getErrCode());
            commonGbChannel.setCommonGbEndTime(deviceChannel.getEndTime());
            if (NumberUtils.isParsable(deviceChannel.getSecrecy())) {
                commonGbChannel.setCommonGbSecrecy(Integer.parseInt(deviceChannel.getSecrecy()));
            }
            commonGbChannel.setCommonGbIPAddress(deviceChannel.getIpAddress());
            commonGbChannel.setCommonGbPort(deviceChannel.getPort());
            commonGbChannel.setCommonGbPassword(deviceChannel.getPassword());
            commonGbChannel.setCommonGbStatus(deviceChannel.isStatus());
            commonGbChannel.setCommonGbLongitude(deviceChannel.getLongitude());
            commonGbChannel.setCommonGbLatitude(deviceChannel.getLatitude());
            commonGbChannel.setCommonGbPtzType(deviceChannel.getPTZType());
            commonGbChannel.setCommonGbPositionType(deviceChannel.getCommonGbPositionType());
            commonGbChannel.setCommonGbBusinessGroupID(deviceChannel.getBusinessGroupId());
        } else {
            for (String key : syncKeys) {
                switch (key) {
                    case "commonGbName":
                        commonGbChannel.setCommonGbName(deviceChannel.getName());
                        break;
                    case "commonGbManufacturer":
                        commonGbChannel.setCommonGbManufacturer(deviceChannel.getManufacture());
                        break;
                    case "commonGbModel":
                        commonGbChannel.setCommonGbModel(deviceChannel.getModel());
                        break;
                    case "commonGbOwner":
                        commonGbChannel.setCommonGbOwner(deviceChannel.getOwner());
                        break;
                    case "commonGbCivilCode":
                        commonGbChannel.setCommonGbCivilCode(deviceChannel.getCivilCode());
                        break;
                    case "commonGbBlock":
                        commonGbChannel.setCommonGbBlock(deviceChannel.getBlock());
                        break;
                    case "commonGbAddress":
                        commonGbChannel.setCommonGbAddress(deviceChannel.getAddress());
                        break;
                    case "commonGbParental":
                        commonGbChannel.setCommonGbParental(deviceChannel.getParental());
                        break;
                    case "commonGbParentID":
                        commonGbChannel.setCommonGbParentID(deviceChannel.getParentId());
                        break;
                    case "commonGbSafetyWay":
                        commonGbChannel.setCommonGbSafetyWay(deviceChannel.getSafetyWay());
                        break;
                    case "commonGbRegisterWay":
                        commonGbChannel.setCommonGbRegisterWay(deviceChannel.getRegisterWay());
                        break;
                    case "commonGbCertNum":
                        commonGbChannel.setCommonGbCertNum(deviceChannel.getCertNum());
                        break;
                    case "commonGbCertifiable":
                        commonGbChannel.setCommonGbCertifiable(deviceChannel.getCertifiable());
                        break;
                    case "commonGbErrCode":
                        commonGbChannel.setCommonGbErrCode(deviceChannel.getErrCode());
                        break;
                    case "commonGbEndTime":
                        commonGbChannel.setCommonGbEndTime(deviceChannel.getEndTime());
                        break;
                    case "commonGbSecrecy":
                        if (NumberUtils.isParsable(deviceChannel.getSecrecy())) {
                            commonGbChannel.setCommonGbSecrecy(Integer.parseInt(deviceChannel.getSecrecy()));
                        }
                        break;
                    case "commonGbIPAddress":
                        commonGbChannel.setCommonGbIPAddress(deviceChannel.getIpAddress());
                        break;
                    case "commonGbPort":
                        commonGbChannel.setCommonGbPort(deviceChannel.getPort());
                        break;
                    case "commonGbPassword":
                        commonGbChannel.setCommonGbPassword(deviceChannel.getPassword());
                        break;
                    case "commonGbStatus":
                        commonGbChannel.setCommonGbStatus(deviceChannel.isStatus());
                        break;
                    case "commonGbLongitude":
                        commonGbChannel.setCommonGbLongitude(deviceChannel.getLongitude());
                        break;
                    case "commonGbLatitude":
                        commonGbChannel.setCommonGbLatitude(deviceChannel.getLatitude());
                        break;
                    case "commonGbPtzType":
                        commonGbChannel.setCommonGbPtzType(deviceChannel.getPTZType());
                        break;
                    case "commonGbPositionType":
                        commonGbChannel.setCommonGbPositionType(deviceChannel.getCommonGbPositionType());
                        break;
                    case "commonGbRoomType":
                        break;
                    case "commonGbUseType":
                        break;
                    case "commonGbSupplyLightType":
                        break;
                    case "commonGbDirectionType":
                        break;
                    case "commonGbResolution":
                        break;
                    case "commonGbBusinessGroupID":
                        commonGbChannel.setCommonGbBusinessGroupID(deviceChannel.getBusinessGroupId());
                        break;
                    case "commonGbDownloadSpeed":
                        break;
                    case "commonGbSVCTimeSupportMode":
                        break;

                }
            }
        }

        return commonGbChannel;
    }
}
