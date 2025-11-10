package com.genersoft.iot.vmp.gb28181.service.impl;

import com.alibaba.excel.support.cglib.beans.BeanMap;
import com.alibaba.excel.util.BeanMapUtils;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.controller.bean.Extent;
import com.genersoft.iot.vmp.gb28181.dao.CommonGBChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.GroupMapper;
import com.genersoft.iot.vmp.gb28181.dao.PlatformChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.RegionMapper;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.channel.ChannelEvent;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelService;
import com.genersoft.iot.vmp.gb28181.service.IPlatformChannelService;
import com.genersoft.iot.vmp.gb28181.utils.VectorTileCatch;
import com.genersoft.iot.vmp.service.bean.GPSMsgInfo;
import com.genersoft.iot.vmp.streamPush.bean.StreamPush;
import com.genersoft.iot.vmp.utils.Coordtransform;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.utils.TileUtils;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.base.CaseFormat;
import lombok.extern.slf4j.Slf4j;
import no.ecc.vectortile.VectorTileEncoder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class GbChannelServiceImpl implements IGbChannelService, CommandLineRunner {

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private CommonGBChannelMapper commonGBChannelMapper;

    @Autowired
    private PlatformChannelMapper platformChannelMapper;

    @Autowired
    private IPlatformChannelService platformChannelService;

    @Autowired
    private RegionMapper regionMapper;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private VectorTileCatch vectorTileCatch;

    private final GeometryFactory geometryFactory = new GeometryFactory();


    @Override
    public void run(String... args) throws Exception {
        // 启动时重新发布抽稀图层
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryAllWithPosition();
        Map<Integer, List<CommonGBChannel>> zoomCameraMap = new ConcurrentHashMap<>();

        channelList.stream().forEach(commonGBChannel ->  {
            if (commonGBChannel.getMapLevel() == null) {
                return;
            }
            List<CommonGBChannel> channelListForZoom = zoomCameraMap.computeIfAbsent(commonGBChannel.getMapLevel(), k -> new ArrayList<>());
            channelListForZoom.add(commonGBChannel);
        });

        String id = "DEFAULT";
        List<CommonGBChannel> beforeData = new ArrayList<>();
        for (Integer zoom : zoomCameraMap.keySet()) {
            beforeData.addAll(zoomCameraMap.get(zoom));
            log.info("[抽稀-发布mvt矢量瓦片] ID：{}，当前层级： {}, ", id, zoom);
            // 按照 z/x/y 数据组织数据， 矢量数据暂时保存在内存中
            // 按照范围生成 x y范围，
            saveTile(id, zoom, "WGS84", beforeData);
            saveTile(id, zoom, "GCJ02", beforeData);
        }
    }

    @Override
    public CommonGBChannel queryByDeviceId(String gbDeviceId) {
        List<CommonGBChannel> commonGBChannels = commonGBChannelMapper.queryByDeviceId(gbDeviceId);
        if (commonGBChannels.isEmpty()) {
            return null;
        }else {
            return commonGBChannels.get(0);
        }
    }

    @Override
    public int add(CommonGBChannel commonGBChannel) {
        if (commonGBChannel.getDataType() == null || commonGBChannel.getDataDeviceId() == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "缺少通道数据类型或通道数据关联设备ID");
        }
        CommonGBChannel commonGBChannelInDb =  commonGBChannelMapper.queryByDataId(commonGBChannel.getDataType(), commonGBChannel.getDataDeviceId());
        Assert.isNull(commonGBChannelInDb, "此推流已经关联通道");

        // 检验国标编号是否重复
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByDeviceId(commonGBChannel.getGbDeviceId());
        Assert.isTrue(channelList.isEmpty(), "国标编号已经存在");

        commonGBChannel.setCreateTime(DateUtil.getNow());
        commonGBChannel.setUpdateTime(DateUtil.getNow());
        int result = commonGBChannelMapper.insert(commonGBChannel);
        try {
            // 发送通知
            eventPublisher.channelEventPublish(commonGBChannel, ChannelEvent.ChannelEventMessageType.ADD);
        } catch (Exception e) {
            log.warn("[通道移除通知] 发送失败，{}", commonGBChannel.getGbDeviceId(), e);
        }
        return result;
    }

    @Override
    @Transactional
    public int delete(int gbId) {
        // 移除国标级联关联的信息
        try {
            platformChannelService.removeChannel(gbId);
        }catch (Exception e) {
            log.error("[移除通道国标级联共享失败]", e);
        }

        CommonGBChannel channel = commonGBChannelMapper.queryById(gbId);
        if (channel != null) {
            commonGBChannelMapper.delete(gbId);
            try {
                // 发送通知
                eventPublisher.channelEventPublish(channel, ChannelEvent.ChannelEventMessageType.DEL);
            } catch (Exception e) {
                log.warn("[通道移除通知] 发送失败，{}", channel.getGbDeviceId(), e);
            }
        }
        return 1;
    }

    @Override
    @Transactional
    public void delete(Collection<Integer> ids) {
        // 移除国标级联关联的信息
        try {
            platformChannelService.removeChannels(new ArrayList<>(ids));
        }catch (Exception e) {
            log.error("[移除通道国标级联共享失败]", e);
        }
        List<CommonGBChannel> channelListInDb = commonGBChannelMapper.queryByIds(ids);
        if (channelListInDb.isEmpty()) {
            return;
        }
        commonGBChannelMapper.batchDelete(channelListInDb);
        try {
            // 发送通知
            eventPublisher.channelEventPublish(channelListInDb, ChannelEvent.ChannelEventMessageType.DEL);
        } catch (Exception e) {
            log.warn("[通道移除通知] 发送失败", e);
        }
    }

    @Override
    public int update(CommonGBChannel commonGBChannel) {
        log.info("[更新通道] 通道ID: {}, ", commonGBChannel.getGbId());
        if (commonGBChannel.getGbId() <= 0) {
            log.warn("[更新通道] 未找到数据库ID，更新失败， {}({})", commonGBChannel.getGbName(), commonGBChannel.getGbDeviceId());
            return 0;
        }
        // 确定编号是否重复
        List<CommonGBChannel> channels = commonGBChannelMapper.queryByDeviceId(commonGBChannel.getGbDeviceId());
        if (channels.size() > 1) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "国标编号重复，请修改编号后保存");
        }
        CommonGBChannel oldChannel = commonGBChannelMapper.queryById(commonGBChannel.getGbId());
        commonGBChannel.setUpdateTime(DateUtil.getNow());
        int result = commonGBChannelMapper.update(commonGBChannel);

        if (result > 0) {
            try {
                CommonGBChannel newChannel = commonGBChannelMapper.queryById(commonGBChannel.getGbId());
                // 发送通知
                eventPublisher.channelEventPublishForUpdate(newChannel, oldChannel);

                if (newChannel.getGbLongitude() != null && !Objects.equals(oldChannel.getGbLongitude(), newChannel.getGbLongitude())
                        && newChannel.getGbLatitude() != null && !Objects.equals(oldChannel.getGbLatitude(), newChannel.getGbLatitude())) {
                    MobilePosition mobilePosition = new MobilePosition();
                    mobilePosition.setDeviceId(newChannel.getGbDeviceId());
                    mobilePosition.setChannelId(newChannel.getGbId());
                    mobilePosition.setChannelDeviceId(newChannel.getGbDeviceId());
                    mobilePosition.setDeviceName(newChannel.getGbName());
                    mobilePosition.setCreateTime(DateUtil.getNow());
                    mobilePosition.setTime(DateUtil.getNow());
                    mobilePosition.setLongitude(newChannel.getGbLongitude());
                    mobilePosition.setLatitude(newChannel.getGbLatitude());
                    eventPublisher.mobilePositionEventPublish(mobilePosition);
                }

            } catch (Exception e) {
                log.warn("[更新通道通知] 发送失败，{}", JSONObject.toJSONString(commonGBChannel), e);
            }
        }
        return result;
    }

    @Override
    public int offline(CommonGBChannel commonGBChannel) {
        if (commonGBChannel.getGbId() <= 0) {
            log.warn("[通道离线] 未找到数据库ID，更新失败， {}({})", commonGBChannel.getGbName(), commonGBChannel.getGbDeviceId());
            return 0;
        }
        int result = commonGBChannelMapper.updateStatusById(commonGBChannel.getGbId(), "OFF");
        if (result > 0) {
            try {
                // 发送通知
                eventPublisher.channelEventPublish(commonGBChannel, ChannelEvent.ChannelEventMessageType.OFF);
            } catch (Exception e) {
                log.warn("[通道离线通知] 发送失败，{}", commonGBChannel.getGbDeviceId(), e);
            }
        }
        return result;
    }

    @Override
    @Transactional
    public int offline(List<CommonGBChannel> commonGBChannelList) {
        if (commonGBChannelList.isEmpty()) {
            log.warn("[多个通道离线] 通道数量为0，更新失败");
            return 0;
        }
        log.info("[通道离线] 共 {} 个", commonGBChannelList.size());
        int limitCount = 1000;
        int result = 0;
        if (commonGBChannelList.size() > limitCount) {
            for (int i = 0; i < commonGBChannelList.size(); i += limitCount) {
                int toIndex = i + limitCount;
                if (i + limitCount > commonGBChannelList.size()) {
                    toIndex = commonGBChannelList.size();
                }
                result += commonGBChannelMapper.updateStatusForListById(commonGBChannelList.subList(i, toIndex), "OFF");
            }
        } else {
            result += commonGBChannelMapper.updateStatusForListById(commonGBChannelList, "OFF");
        }
        if (result > 0) {
            try {
                // 发送catalog
                eventPublisher.channelEventPublish(commonGBChannelList, ChannelEvent.ChannelEventMessageType.OFF);
            } catch (Exception e) {
                log.warn("[多个通道离线] 发送失败，数量：{}", commonGBChannelList.size(), e);
            }
        }
        return result;
    }

    @Override
    public int online(CommonGBChannel commonGBChannel) {
        if (commonGBChannel.getGbId() <= 0) {
            log.warn("[通道上线] 未找到数据库ID，更新失败， {}({})", commonGBChannel.getGbName(), commonGBChannel.getGbDeviceId());
            return 0;
        }
        int result = commonGBChannelMapper.updateStatusById(commonGBChannel.getGbId(), "ON");
        if (result > 0) {
            try {
                // 发送通知
                eventPublisher.channelEventPublish(commonGBChannel, ChannelEvent.ChannelEventMessageType.ON);
            } catch (Exception e) {
                log.warn("[通道上线通知] 发送失败，{}", commonGBChannel.getGbDeviceId(), e);
            }
        }
        return 0;
    }

    @Override
    @Transactional
    public int online(List<CommonGBChannel> commonGBChannelList) {
        if (commonGBChannelList.isEmpty()) {
            log.warn("[多个通道上线] 通道数量为0，更新失败");
            return 0;
        }
        // 批量更新
        int limitCount = 1000;
        int result = 0;
        if (commonGBChannelList.size() > limitCount) {
            for (int i = 0; i < commonGBChannelList.size(); i += limitCount) {
                int toIndex = i + limitCount;
                if (i + limitCount > commonGBChannelList.size()) {
                    toIndex = commonGBChannelList.size();
                }
                result += commonGBChannelMapper.updateStatusForListById(commonGBChannelList.subList(i, toIndex), "ON");
            }
        } else {
            result += commonGBChannelMapper.updateStatusForListById(commonGBChannelList, "ON");
        }
        try {
            // 发送catalog
            eventPublisher.channelEventPublish(commonGBChannelList, ChannelEvent.ChannelEventMessageType.ON);
        } catch (Exception e) {
            log.warn("[多个通道上线] 发送失败，数量：{}", commonGBChannelList.size(), e);
        }

        return result;
    }

    @Override
    @Transactional
    public void batchAdd(List<CommonGBChannel> commonGBChannels) {
        if (commonGBChannels.isEmpty()) {
            log.warn("[新增多个通道] 通道数量为0，更新失败");
            return;
        }
        // 批量保存
        int limitCount = 1000;
        int result = 0;
        if (commonGBChannels.size() > limitCount) {
            for (int i = 0; i < commonGBChannels.size(); i += limitCount) {
                int toIndex = i + limitCount;
                if (i + limitCount > commonGBChannels.size()) {
                    toIndex = commonGBChannels.size();
                }
                result += commonGBChannelMapper.batchAdd(commonGBChannels.subList(i, toIndex));
            }
        } else {
            result += commonGBChannelMapper.batchAdd(commonGBChannels);
        }
        try {
            // 发送catalog
            eventPublisher.channelEventPublish(commonGBChannels, ChannelEvent.ChannelEventMessageType.ADD);
        } catch (Exception e) {
            log.warn("[多个通道新增] 发送失败，数量：{}", commonGBChannels.size(), e);
        }
        log.warn("[新增多个通道] 通道数量为{}，成功保存：{}", commonGBChannels.size(), result);
    }

    @Override
    public void batchUpdate(List<CommonGBChannel> commonGBChannels) {
        if (commonGBChannels.isEmpty()) {
            log.warn("[更新多个通道] 通道数量为0，更新失败");
            return;
        }
        List<CommonGBChannel> oldCommonGBChannelList = commonGBChannelMapper.queryOldChanelListByChannels(commonGBChannels);
        // 批量保存
        int limitCount = 1000;
        int result = 0;
        if (commonGBChannels.size() > limitCount) {
            for (int i = 0; i < commonGBChannels.size(); i += limitCount) {
                int toIndex = i + limitCount;
                if (i + limitCount > commonGBChannels.size()) {
                    toIndex = commonGBChannels.size();
                }
                result += commonGBChannelMapper.batchUpdate(commonGBChannels.subList(i, toIndex));
            }
        } else {
            result += commonGBChannelMapper.batchUpdate(commonGBChannels);
        }
        log.info("[更新多个通道] 通道数量为{}，成功保存：{}", commonGBChannels.size(), result);
        // 发送通过更新通知
        try {
            // 发送通知
            eventPublisher.channelEventPublishForUpdate(commonGBChannels, oldCommonGBChannelList);
        } catch (Exception e) {
            log.warn("[更新多个通道] 发送失败，{}个", commonGBChannels.size(), e);
        }
    }

    @Override
    @Transactional
    public void updateStatus(List<CommonGBChannel> commonGBChannels) {
        if (commonGBChannels.isEmpty()) {
            log.warn("[更新多个通道状态] 通道数量为0，更新失败");
            return;
        }
        List<CommonGBChannel> oldChanelListByChannels = commonGBChannelMapper.queryOldChanelListByChannels(commonGBChannels);
        int limitCount = 1000;
        int result = 0;
        if (commonGBChannels.size() > limitCount) {
            for (int i = 0; i < commonGBChannels.size(); i += limitCount) {
                int toIndex = i + limitCount;
                if (i + limitCount > commonGBChannels.size()) {
                    toIndex = commonGBChannels.size();
                }
                result += commonGBChannelMapper.updateStatus(commonGBChannels.subList(i, toIndex));
            }
        } else {
            result += commonGBChannelMapper.updateStatus(commonGBChannels);
        }
        log.warn("[更新多个通道状态] 通道数量为{}，成功保存：{}", commonGBChannels.size(), result);
        // 发送通过更新通知
        try {
            // 发送通知
            eventPublisher.channelEventPublishForUpdate(commonGBChannels, oldChanelListByChannels);
        } catch (Exception e) {
            log.warn("[更新多个通道] 发送失败，{}个", commonGBChannels.size(), e);
        }
    }



    @Override
    public CommonGBChannel getOne(int id) {
        return commonGBChannelMapper.queryById(id);
    }

    @Override
    public List<IndustryCodeType> getIndustryCodeList() {
        IndustryCodeTypeEnum[] values = IndustryCodeTypeEnum.values();
        List<IndustryCodeType> result = new ArrayList<>(values.length);
        for (IndustryCodeTypeEnum value : values) {
            result.add(IndustryCodeType.getInstance(value));
        }
        Collections.sort(result);
        return result;
    }

    @Override
    public List<DeviceType> getDeviceTypeList() {
        DeviceTypeEnum[] values = DeviceTypeEnum.values();
        List<DeviceType> result = new ArrayList<>(values.length);
        for (DeviceTypeEnum value : values) {
            result.add(DeviceType.getInstance(value));
        }
        Collections.sort(result);
        return result;
    }

    @Override
    public List<NetworkIdentificationType> getNetworkIdentificationTypeList() {
        NetworkIdentificationTypeEnum[] values = NetworkIdentificationTypeEnum.values();
        List<NetworkIdentificationType> result = new ArrayList<>(values.length);
        for (NetworkIdentificationTypeEnum value : values) {
            result.add(NetworkIdentificationType.getInstance(value));
        }
        Collections.sort(result);
        return result;
    }

    @Override
    public void reset(int id, List<String> chanelFields) {
        log.info("[重置国标通道] id: {}", id);
        Assert.notEmpty(chanelFields, "待重置字段为空");
        CommonGBChannel channel = getOne(id);
        if (channel == null) {
            log.warn("[重置国标通道] 未找到对应Id的通道: id: {}", id);
            throw new ControllerException(ErrorCode.ERROR400);
        }
        if (channel.getDataType() != ChannelDataType.GB28181) {
            log.warn("[重置国标通道] 非国标下级通道无法重置: id: {}", id);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "非国标下级通道无法重置");
        }
        List<String> dbFields = new ArrayList<>();

        for (String chanelField : chanelFields) {
            BeanWrapperImpl wrapper = new BeanWrapperImpl(channel);
            if (wrapper.isReadableProperty(chanelField)) {
                dbFields.add(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, chanelField));
            }
        }
        Assert.notEmpty(dbFields, "待重置字段为空");

        // 这个多加一个参数,为了防止将非国标的通道通过此方法清空内容,导致意外发生
        commonGBChannelMapper.reset(id, dbFields, DateUtil.getNow());
        CommonGBChannel channelNew = getOne(id);
        // 发送通过更新通知
        try {
            // 发送通知
            eventPublisher.channelEventPublishForUpdate(channelNew, channel);
        } catch (Exception e) {
            log.warn("[通道移除通知] 发送失败，{}", channelNew.getGbDeviceId(), e);
        }
    }

    @Override
    public PageInfo<CommonGBChannel> queryListByCivilCode(int page, int count, String query, Boolean online, Integer channelType, String civilCode) {
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<CommonGBChannel> all = commonGBChannelMapper.queryListByCivilCode(query, online, channelType, civilCode);
        return new PageInfo<>(all);
    }

    @Override
    public PageInfo<CommonGBChannel> queryListByParentId(int page, int count, String query, Boolean online, Integer channelType, String groupDeviceId) {
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<CommonGBChannel> all = commonGBChannelMapper.queryListByParentId(query, online, channelType, groupDeviceId);
        return new PageInfo<>(all);
    }

    @Override
    public void removeCivilCode(List<Region> allChildren) {
        commonGBChannelMapper.removeCivilCode(allChildren);
        // TODO 是否需要通知上级, 或者等添加新的行政区划时发送更新通知

    }

    @Override
    public void addChannelToRegion(String civilCode, List<Integer> channelIds) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByIds(channelIds);
        if (channelList.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "所有通道Id不存在");
        }
        List<CommonGBChannel> channelListForOld = new ArrayList<>(channelList);
        for (CommonGBChannel channel : channelList) {
            channel.setGbCivilCode(civilCode);
        }
        int result = commonGBChannelMapper.updateRegion(civilCode, channelList);
        // 发送通知
        if (result > 0) {
            platformChannelService.checkRegionAdd(channelList);
            try {
                // 发送catalog
                eventPublisher.channelEventPublishForUpdate(channelList, channelListForOld);
            } catch (Exception e) {
                log.warn("[多个通道添加行政区划] 发送失败，数量：{}", channelList.size(), e);
            }
        }
    }

    @Override
    @Transactional
    public void deleteChannelToRegion(String civilCode, List<Integer> channelIds) {
        if (!ObjectUtils.isEmpty(civilCode)) {
            deleteChannelToRegionByCivilCode(civilCode);
        }
        if (!ObjectUtils.isEmpty(channelIds)) {
            deleteChannelToRegionByChannelIds(channelIds);
        }
    }

    @Override
    public void deleteChannelToRegionByCivilCode(String civilCode) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByCivilCode(civilCode);
        if (channelList.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "所有通道Id不存在");
        }
        int result = commonGBChannelMapper.removeCivilCodeByChannels(channelList);
        Region region = regionMapper.queryByDeviceId(civilCode);
        if (region == null) {
            platformChannelService.checkRegionRemove(channelList, null);
        }else {
            List<Region> regionList = new ArrayList<>();
            regionList.add(region);
            platformChannelService.checkRegionRemove(channelList, regionList);
        }
        // TODO 发送通知
//        if (result > 0) {
//            try {
//                // 发送catalog
//                eventPublisher.catalogEventPublish(null, channelList, CatalogEvent.UPDATE);
//            }catch (Exception e) {
//                log.warn("[多个通道添加行政区划] 发送失败，数量：{}", channelList.size(), e);
//            }
//        }
    }

    @Override
    public void deleteChannelToRegionByChannelIds(List<Integer> channelIds) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByIds(channelIds);
        if (channelList.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "所有通道Id不存在");
        }
        int result = commonGBChannelMapper.removeCivilCodeByChannels(channelList);

        platformChannelService.checkRegionRemove(channelList, null);
        // TODO 发送通知
//        if (result > 0) {
//            try {
//                // 发送catalog
//                eventPublisher.catalogEventPublish(null, channelList, CatalogEvent.UPDATE);
//            }catch (Exception e) {
//                log.warn("[多个通道添加行政区划] 发送失败，数量：{}", channelList.size(), e);
//            }
//        }
    }

    @Override
    public void addChannelToRegionByGbDevice(String civilCode, List<Integer> deviceIds) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByDataTypeAndDeviceIds(ChannelDataType.GB28181, deviceIds);
        if (channelList.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "所有通道Id不存在");
        }
        List<CommonGBChannel> channelListForOld = new ArrayList<>(channelList);
        for (CommonGBChannel channel : channelList) {
            channel.setGbCivilCode(civilCode);
        }
        int result = commonGBChannelMapper.updateRegion(civilCode, channelList);
        // 发送通知
        if (result > 0) {
            try {
                // 发送catalog
                eventPublisher.channelEventPublishForUpdate(channelList, channelListForOld);
            } catch (Exception e) {
                log.warn("[多个通道添加行政区划] 发送失败，数量：{}", channelList.size(), e);
            }
        }
    }

    @Override
    public void deleteChannelToRegionByGbDevice(List<Integer> deviceIds) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByDataTypeAndDeviceIds(ChannelDataType.GB28181, deviceIds);
        if (channelList.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "所有通道Id不存在");
        }
        int result = commonGBChannelMapper.removeCivilCodeByChannels(channelList);
        platformChannelService.checkRegionRemove(channelList, null);
    }

    @Override
    @Transactional
    public void removeParentIdByBusinessGroup(String businessGroup) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByBusinessGroup(businessGroup);
        if (channelList.isEmpty()) {
            return;
        }
        int result = commonGBChannelMapper.removeParentIdByChannels(channelList);
        List<Group> groupList = groupMapper.queryByBusinessGroup(businessGroup);
        platformChannelService.checkGroupRemove(channelList, groupList);

    }

    @Override
    public void removeParentIdByGroupList(List<Group> groupList) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByGroupList(groupList);
        if (channelList.isEmpty()) {
            return;
        }
        commonGBChannelMapper.removeParentIdByChannels(channelList);
        platformChannelService.checkGroupRemove(channelList, groupList);
    }

    @Override
    public void updateBusinessGroup(String oldBusinessGroup, String newBusinessGroup) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByBusinessGroup(oldBusinessGroup);
        if (channelList.isEmpty()) {
            log.info("[更新业务分组] 发现未关联任何通道： {}", oldBusinessGroup);
            return;
        }
        List<CommonGBChannel> channelListForOld = new ArrayList<>(channelList);
        int result = commonGBChannelMapper.updateBusinessGroupByChannelList(newBusinessGroup, channelList);
        if (result > 0) {
            for (CommonGBChannel channel : channelList) {
                channel.setGbBusinessGroupId(newBusinessGroup);
            }
            // 发送catalog
            try {
                eventPublisher.channelEventPublishForUpdate(channelList, channelListForOld);
            } catch (Exception e) {
                log.warn("[多个通道业务分组] 发送失败，数量：{}", channelList.size(), e);
            }
        }
    }

    @Override
    public void updateParentIdGroup(String oldParentId, String newParentId) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByParentId(oldParentId);
        if (channelList.isEmpty()) {
            return;
        }
        List<CommonGBChannel> channelListForOld = new ArrayList<>(channelList);
        int result = commonGBChannelMapper.updateParentIdByChannelList(newParentId, channelList);
        if (result > 0) {
            for (CommonGBChannel channel : channelList) {
                channel.setGbParentId(newParentId);
            }
            // 发送catalog
            try {
                eventPublisher.channelEventPublishForUpdate(channelList, channelListForOld);
            } catch (Exception e) {
                log.warn("[多个通道业务分组] 发送失败，数量：{}", channelList.size(), e);
            }
        }
    }

    @Override
    @Transactional
    public void addChannelToGroup(String parentId, String businessGroup, List<Integer> channelIds) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByIds(channelIds);
        if (channelList.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "所有通道Id不存在");
        }
        List<CommonGBChannel> channelListForOld = new ArrayList<>(channelList);
        int result = commonGBChannelMapper.updateGroup(parentId, businessGroup, channelList);
        for (CommonGBChannel commonGBChannel : channelList) {
            commonGBChannel.setGbParentId(parentId);
            commonGBChannel.setGbBusinessGroupId(businessGroup);
        }

        // 发送通知
        if (result > 0) {
            platformChannelService.checkGroupAdd(channelList);
            try {
                // 发送catalog
                eventPublisher.channelEventPublishForUpdate(channelList, channelListForOld);
            } catch (Exception e) {
                log.warn("[多个通道添加行政区划] 发送失败，数量：{}", channelList.size(), e);
            }
        }
    }

    @Override
    public void deleteChannelToGroup(String parentId, String businessGroup, List<Integer> channelIds) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByIds(channelIds);
        if (channelList.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "所有通道Id不存在");
        }
        commonGBChannelMapper.removeParentIdByChannels(channelList);

        Group group = groupMapper.queryOneByDeviceId(parentId, businessGroup);
        if (group == null) {
            platformChannelService.checkGroupRemove(channelList, null);
        }else {
            List<Group> groupList = new ArrayList<>();
            groupList.add(group);
            platformChannelService.checkGroupRemove(channelList, groupList);
        }
    }

    @Override
    @Transactional
    public void addChannelToGroupByGbDevice(String parentId, String businessGroup, List<Integer> deviceIds) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByDataTypeAndDeviceIds(ChannelDataType.GB28181, deviceIds);
        if (channelList.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "所有通道Id不存在");
        }
        List<CommonGBChannel>  channelListForOld = new ArrayList<>(channelList);

        for (CommonGBChannel channel : channelList) {
            channel.setGbParentId(parentId);
            channel.setGbBusinessGroupId(businessGroup);
        }
        int result = commonGBChannelMapper.updateGroup(parentId, businessGroup, channelList);

        for (CommonGBChannel commonGBChannel : channelList) {
            commonGBChannel.setGbParentId(parentId);
            commonGBChannel.setGbBusinessGroupId(businessGroup);
        }
        // 发送通知
        if (result > 0) {
            platformChannelService.checkGroupAdd(channelList);
            try {
                // 发送catalog
                eventPublisher.channelEventPublishForUpdate(channelList, channelListForOld);
            } catch (Exception e) {
                log.warn("[多个通道添加行政区划] 发送失败，数量：{}", channelList.size(), e);
            }
        }
    }

    @Override
    public void deleteChannelToGroupByGbDevice(List<Integer> deviceIds) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByDataTypeAndDeviceIds(ChannelDataType.GB28181, deviceIds);
        if (channelList.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "所有通道Id不存在");
        }
        commonGBChannelMapper.removeParentIdByChannels(channelList);
        platformChannelService.checkGroupRemove(channelList, null);
    }

    @Override
    public CommonGBChannel queryOneWithPlatform(Integer platformId, String channelDeviceId) {
        // 防止共享的通道编号重复
        List<CommonGBChannel> channelList = platformChannelMapper.queryOneWithPlatform(platformId, channelDeviceId);
        if (!channelList.isEmpty()) {
            return channelList.get(channelList.size() - 1);
        }else {
            return null;
        }
    }

    @Override
    public void updateCivilCode(String oldCivilCode, String newCivilCode) {
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByCivilCode(oldCivilCode);
        if (channelList.isEmpty()) {
            return;
        }
        List<CommonGBChannel>  channelListForOld = new ArrayList<>(channelList);
        int result = commonGBChannelMapper.updateCivilCodeByChannelList(newCivilCode, channelList);
        if (result > 0) {
            for (CommonGBChannel channel : channelList) {
                channel.setGbCivilCode(newCivilCode);
            }
            // 发送catalog
            try {
                eventPublisher.channelEventPublishForUpdate(channelList, channelListForOld);
            } catch (Exception e) {
                log.warn("[多个通道业务分组] 发送失败，数量：{}", channelList.size(), e);
            }
        }
    }

    @Override
    public List<CommonGBChannel> queryListByStreamPushList(List<StreamPush> streamPushList) {
        return commonGBChannelMapper.queryListByStreamPushList(ChannelDataType.STREAM_PUSH, streamPushList);
    }

    @Override
    public PageInfo<CommonGBChannel> queryList(int page, int count, String query, Boolean online, Boolean hasRecordPlan,
                                               Integer channelType, String civilCode, String parentDeviceId) {
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<CommonGBChannel> all = commonGBChannelMapper.queryList(query, online,  hasRecordPlan, channelType, civilCode, parentDeviceId);
        return new PageInfo<>(all);
    }

    @Override
    public PageInfo<CommonGBChannel> queryListByCivilCodeForUnusual(int page, int count, String query, Boolean online, Integer channelType) {
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<CommonGBChannel> all = commonGBChannelMapper.queryListByCivilCodeForUnusual(query, online, channelType);
        return new PageInfo<>(all);
    }

    @Override
    public void clearChannelCivilCode(Boolean all, List<Integer> channelIds) {

        List<Integer> channelIdsForClear;
        if (all != null && all) {
            channelIdsForClear = commonGBChannelMapper.queryAllForUnusualCivilCode();
        }else {
            channelIdsForClear = channelIds;
        }
        commonGBChannelMapper.removeCivilCodeByChannelIds(channelIdsForClear);
    }

    @Override
    public PageInfo<CommonGBChannel> queryListByParentForUnusual(int page, int count, String query, Boolean online, Integer channelType) {
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        List<CommonGBChannel> all = commonGBChannelMapper.queryListByParentForUnusual(query, online, channelType);
        return new PageInfo<>(all);
    }

    @Override
    public void clearChannelParent(Boolean all, List<Integer> channelIds) {
        List<Integer> channelIdsForClear;
        if (all != null && all) {
            channelIdsForClear = commonGBChannelMapper.queryAllForUnusualParent();
        }else {
            channelIdsForClear = channelIds;
        }
        commonGBChannelMapper.removeParentIdByChannelIds(channelIdsForClear);
    }

    @Override
    public void updateGPSFromGPSMsgInfo(List<GPSMsgInfo> gpsMsgInfoList) {
        if (gpsMsgInfoList == null || gpsMsgInfoList.isEmpty()) {
            return;
        }
        // 此处来源默认为WGS84, 所以直接入库
        commonGBChannelMapper.updateGpsByDeviceId(gpsMsgInfoList);
//
//        Map<String, GPSMsgInfo> gpsMsgInfoMap = new ConcurrentReferenceHashMap<>();
//        for (GPSMsgInfo gpsMsgInfo : gpsMsgInfoList) {
//            gpsMsgInfoMap.put(gpsMsgInfo.getId(), gpsMsgInfo);
//        }
//
//        List<CommonGBChannel> channelList = commonGBChannelMapper.queryByGbDeviceIds(new ArrayList<>(gpsMsgInfoMap.keySet()));
//        if (channelList.isEmpty()) {
//            return;
//        }
//        channelList.forEach(commonGBChannel -> {
//            MobilePosition mobilePosition = new MobilePosition();
//            mobilePosition.setDeviceId(commonGBChannel.getGbDeviceId());
//            mobilePosition.setChannelId(commonGBChannel.getGbId());
//            mobilePosition.setDeviceName(commonGBChannel.getGbName());
//            mobilePosition.setCreateTime(DateUtil.getNow());
//            mobilePosition.setTime(DateUtil.getNow());
//            mobilePosition.setLongitude(commonGBChannel.getGbLongitude());
//            mobilePosition.setLatitude(commonGBChannel.getGbLatitude());
//            eventPublisher.mobilePositionEventPublish(mobilePosition);
//        });
    }

    @Transactional
    @Override
    public void updateGPS(List<CommonGBChannel> commonGBChannels) {
        int limitCount = 1000;
        if (commonGBChannels.size() > limitCount) {
            for (int i = 0; i < commonGBChannels.size(); i += limitCount) {
                int toIndex = i + limitCount;
                if (i + limitCount > commonGBChannels.size()) {
                    toIndex = commonGBChannels.size();
                }
                commonGBChannelMapper.updateGps(commonGBChannels.subList(i, toIndex));
            }
        } else {
            commonGBChannelMapper.updateGps(commonGBChannels);
        }
    }

    @Override
    public List<CommonGBChannel> queryListForMap(String query, Boolean online, Boolean hasRecordPlan, Integer channelType) {
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }
        return commonGBChannelMapper.queryList(query, online,  hasRecordPlan, channelType, null, null);
    }

    @Override
    public CommonGBChannel queryCommonChannelByDeviceChannel(DeviceChannel channel) {
        return commonGBChannelMapper.queryCommonChannelByDeviceChannel(channel.getDataType(), channel.getDataDeviceId(), channel.getDeviceId());
    }

    @Override
    public void resetLevel() {
        commonGBChannelMapper.resetLevel();
    }

    @Override
    public byte[] getTile(int z, int x, int y, String geoCoordSys) {
        double minLon = TileUtils.tile2lon(x, z);
        double maxLon = TileUtils.tile2lon(x + 1, z);
        double maxLat = TileUtils.tile2lat(y, z);
        double minLat = TileUtils.tile2lat(y + 1, z);

        if (geoCoordSys != null) {
            if (geoCoordSys.equalsIgnoreCase("GCJ02")) {
                Double[] minPosition = Coordtransform.GCJ02ToWGS84(minLon, minLat);
                minLon = minPosition[0];
                minLat = minPosition[1];

                Double[] maxPosition = Coordtransform.GCJ02ToWGS84(maxLon, maxLat);
                maxLon = maxPosition[0];
                maxLat = maxPosition[1];
            }
        }
        // 从数据库查询对应的数据
        List<CommonGBChannel> channelList = commonGBChannelMapper.queryCameraChannelInBox(minLon, maxLon, minLat, maxLat);
        VectorTileEncoder encoder = new VectorTileEncoder();
        if (!channelList.isEmpty()) {
            channelList.forEach(commonGBChannel -> {
                double lon = commonGBChannel.getGbLongitude();
                double lat = commonGBChannel.getGbLatitude();
                // 转换为目标坐标系
                if (geoCoordSys != null) {
                    if (geoCoordSys.equalsIgnoreCase("GCJ02")) {
                        Double[] minPosition = Coordtransform.WGS84ToGCJ02(lon, lat);
                        lon = minPosition[0];
                        lat = minPosition[1];
                    }
                }

                // 将 lon/lat 转为瓦片内像素坐标（0..256）
                double[] px = TileUtils.lonLatToTilePixel(lon, lat, z, x, y);
                Point pointGeom = geometryFactory.createPoint(new Coordinate(px[0], px[1]));

                BeanMap beanMap = BeanMapUtils.create(commonGBChannel);
                encoder.addFeature("points", beanMap, pointGeom);
            });
        }
        return encoder.encode();
    }


    @Override
    public String drawThin(Map<Integer, Double> zoomParam, Extent extent, String geoCoordSys) {
        long time = System.currentTimeMillis();

        String id = UUID.randomUUID().toString();
        List<CommonGBChannel> channelListInExtent;
        if (extent == null) {
            log.info("[抽稀] ID: {}, 未设置范围，从数据库读取摄像头的范围", id);
            extent = commonGBChannelMapper.queryExtent();
            channelListInExtent = commonGBChannelMapper.queryAllWithPosition();
        }else {
            if (geoCoordSys != null && geoCoordSys.equalsIgnoreCase("GCJ02")) {
                Double[] maxPosition = Coordtransform.GCJ02ToWGS84(extent.getMaxLng(), extent.getMaxLat());
                Double[] minPosition = Coordtransform.GCJ02ToWGS84(extent.getMinLng(), extent.getMinLat());

                extent.setMaxLng(maxPosition[0]);
                extent.setMaxLat(maxPosition[1]);

                extent.setMinLng(minPosition[0]);
                extent.setMinLat(minPosition[1]);
            }
            // 获取数据源
            channelListInExtent = commonGBChannelMapper.queryListInExtent(extent.getMinLng(), extent.getMaxLng(), extent.getMinLat(), extent.getMaxLat());
        }
        Assert.isTrue(!channelListInExtent.isEmpty(), "通道数据为空");

        log.info("[开始抽稀] ID： {}， 范围，[{}, {}, {}, {}]", id, extent.getMinLng(), extent.getMinLat(), extent.getMaxLng(), extent.getMaxLat());

        Extent finalExtent = extent;
        // 记录进度
        saveProcess(id, 0, "开始抽稀");
        dynamicTask.startDelay(id, () -> {
            try {
                // 存储每层的抽稀结果， key为层级（zoom），value为摄像头数组
                Map<Integer, Collection<CommonGBChannel>> zoomCameraMap = new HashMap<>();

                // 冗余一份已经处理过的摄像头的数据， 避免多次循环获取
                Map<Integer, CommonGBChannel> useCameraMap = new HashMap<>();
                AtomicReference<Double> process = new AtomicReference<>((double) 0);
                for (Integer zoom : zoomParam.keySet()) {

                    Map<String, CommonGBChannel> useCameraMapForZoom = new HashMap<>();
                    Map<String, CommonGBChannel> cameraMapForZoom = new HashMap<>();

                    if (Objects.equals(zoom, Collections.max(zoomParam.keySet()))) {
                        // 最大层级不进行抽稀， 将未进行抽稀的数据直接存储到这个层级
                        for (CommonGBChannel channel : channelListInExtent) {
                            if (!useCameraMap.containsKey(channel.getGbId())) {
                                channel.setMapLevel(zoom);
                                // 这个的key跟后面的不一致是因为无需抽稀， 直接存储原始数据
                                cameraMapForZoom.put(channel.getGbId() + "", channel);
                                useCameraMap.put(channel.getGbId(), channel);
                            }
                        }
                    }else {
                        Double diff = zoomParam.get(zoom);
                        // 对这个层级展开抽稀
                        log.info("[抽稀] ID：{}，当前层级： {}, 坐标间隔： {}", id, zoom, diff);

                        // 更新上级图层的数据到当前层级，确保当前层级展示时考虑到之前层级的数据
                        for (CommonGBChannel channel : useCameraMap.values()) {
                            int lngGrid = (int)(channel.getGbLongitude() / diff);
                            int latGrid = (int)(channel.getGbLatitude() / diff);
                            String gridKey = latGrid + ":" + lngGrid;
                            useCameraMapForZoom.put(gridKey, channel);
                        }

                        // 对数据开始执行抽稀
                        for (CommonGBChannel channel : channelListInExtent) {
                            // 已经分配再其他层级的，本层级不再使用
                            if (useCameraMap.containsKey(channel.getGbId())) {
                                continue;
                            }
                            int lngGrid = (int)(channel.getGbLongitude() / diff);
                            int latGrid = (int)(channel.getGbLatitude() / diff);
                            // 数据网格Id
                            String gridKey = latGrid + ":" + lngGrid;
                            if (useCameraMapForZoom.containsKey(gridKey)) {
                                continue;
                            }
                            if (cameraMapForZoom.containsKey(gridKey)) {
                                CommonGBChannel oldChannel = cameraMapForZoom.get(gridKey);
                                // 如果一个网格存在多个数据，则选择最接近中心点的， 目前只选择了经度方向作为参考
                                if (channel.getGbLongitude() % diff < oldChannel.getGbLongitude() % diff) {
                                    channel.setMapLevel(zoom);
                                    cameraMapForZoom.put(gridKey, channel);
                                    useCameraMap.put(channel.getGbId(), channel);
                                    useCameraMap.remove(oldChannel.getGbId());
                                    oldChannel.setMapLevel(null);
                                }
                            }else {
                                channel.setMapLevel(zoom);
                                cameraMapForZoom.put(gridKey, channel);
                                useCameraMap.put(channel.getGbId(), channel);
                            }
                        }
                    }
                    // 存储
                    zoomCameraMap.put(zoom, cameraMapForZoom.values());
                    process.updateAndGet(v -> (v + 0.5 / zoomParam.size()));
                    saveProcess(id, process.get(), "抽稀图层： " + zoom);
                }

                // 抽稀完成, 对数据发布mvt矢量瓦片
                List<CommonGBChannel> beforeData = new ArrayList<>();
                for (Integer zoom : zoomCameraMap.keySet()) {
                    beforeData.addAll(zoomCameraMap.get(zoom));
                    log.info("[抽稀-发布mvt矢量瓦片] ID：{}，当前层级： {}", id, zoom);
                    // 按照 z/x/y 数据组织数据， 矢量数据暂时保存在内存中
                    // 按照范围生成 x y范围，
                    saveTile(id, zoom, "WGS84", beforeData);
                    saveTile(id, zoom, "GCJ02", beforeData);
                    process.updateAndGet(v -> (v + 0.5 / zoomParam.size()));
                    saveProcess(id, process.get(), "发布矢量瓦片： " + zoom);
                }
                // 记录原始数据，未保存做准备
                vectorTileCatch.addSource(id, new ArrayList<>(useCameraMap.values()));

                log.info("[抽稀完成] ID：{}, 耗时： {}ms", id, (System.currentTimeMillis() - time));
                saveProcess(id, 1, "抽稀完成");
            } catch (Exception e) {
                log.info("[抽稀] 失败 ID：{}", id, e);
            }

        }, 1);

        return id;
    }

    private void saveTile(String id, int z, String geoCoordSys, Collection<CommonGBChannel> commonGBChannelList ) {
        Map<String, VectorTileEncoder> encoderMap = new HashMap<>();
        commonGBChannelList.forEach(commonGBChannel -> {
            double lon = commonGBChannel.getGbLongitude();
            double lat = commonGBChannel.getGbLatitude();
            if (geoCoordSys != null && geoCoordSys.equalsIgnoreCase("GCJ02")) {
                Double[] minPosition = Coordtransform.WGS84ToGCJ02(lon, lat);
                lon = minPosition[0];
                lat = minPosition[1];
            }
            double[] doubles = TileUtils.lonLatToTileXY(lon, lat, z);
            int x = (int) doubles[0];
            int y = (int) doubles[1];
            String key = z + "_" + x + "_" + y + "_" + geoCoordSys;
            VectorTileEncoder encoder =encoderMap.get(key);
            if (encoder == null) {
                encoder = new VectorTileEncoder();
                encoderMap.put(key, encoder);
            }
            // 将 lon/lat 转为瓦片内像素坐标（0..256）
            double[] px = TileUtils.lonLatToTilePixel(lon, lat, z, x, y);
            Point pointGeom = geometryFactory.createPoint(new Coordinate(px[0], px[1]));
            BeanMap beanMap = BeanMapUtils.create(commonGBChannel);
            encoder.addFeature("points", beanMap, pointGeom);
        });
        encoderMap.forEach((key, encoder) -> {
            vectorTileCatch.addVectorTile(id, key, encoder.encode());
        });
    }

    private void saveProcess(String id, double process, String msg) {
        String key = VideoManagerConstants.DRAW_THIN_PROCESS_PREFIX + id;
        Duration duration = Duration.ofMinutes(30);
        redisTemplate.opsForValue().set(key, new DrawThinProcess(process, msg), duration);
    }

    @Override
    public DrawThinProcess thinProgress(String id) {
        String key = VideoManagerConstants.DRAW_THIN_PROCESS_PREFIX + id;
        return (DrawThinProcess) redisTemplate.opsForValue().get(key);
    }

    @Override
    @Transactional
    public void saveThin(String id) {
        commonGBChannelMapper.resetLevel();
        List<CommonGBChannel> channelList = vectorTileCatch.getChannelList(id);
        if (channelList != null && !channelList.isEmpty()) {
            int limitCount = 1000;
            if (channelList.size() > limitCount) {
                for (int i = 0; i < channelList.size(); i += limitCount) {
                    int toIndex = i + limitCount;
                    if (i + limitCount > channelList.size()) {
                        toIndex = channelList.size();
                    }
                    commonGBChannelMapper.saveLevel(channelList.subList(i, toIndex));
                }
            } else {
                commonGBChannelMapper.saveLevel(channelList);
            }
        }
        vectorTileCatch.save(id);
    }
}
