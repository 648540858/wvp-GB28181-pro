package com.genersoft.iot.vmp.web.custom.service;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.Group;
import com.genersoft.iot.vmp.gb28181.dao.CommonGBChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.DeviceChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.DeviceMapper;
import com.genersoft.iot.vmp.gb28181.dao.GroupMapper;
import com.genersoft.iot.vmp.utils.Coordtransform;
import com.genersoft.iot.vmp.web.custom.bean.CameraChannel;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.github.xiaoymin.knife4j.core.util.Assert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class CameraChannelService implements CommandLineRunner {

    @Autowired
    private CommonGBChannelMapper channelMapper;

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Override
    public void run(String... args) throws Exception {
        // 启动时获取全局token

    }

    public PageInfo<CameraChannel> queryList(Integer page, Integer count, String query, String sortName, String order, String groupAlias, Boolean status, Boolean containMobileDevice) {

        // 构建组织结构信息
        Group group = groupMapper.queryGroupByAlias(groupAlias);
        Assert.notNull(group, "获取组织结构失败");
        String groupDeviceId = group.getDeviceId();

        // 构建分页
        PageHelper.startPage(page, count);
        if (query != null) {
            query = query.replaceAll("/", "//")
                    .replaceAll("%", "/%")
                    .replaceAll("_", "/_");
        }

        List<CameraChannel> all = channelMapper.queryListForSy(query, sortName, order, groupDeviceId, status, containMobileDevice);
        PageInfo<CameraChannel> groupPageInfo = new PageInfo<>(all);
        List<CameraChannel> list = addIconPathForCameraChannelList(groupPageInfo.getList());
        groupPageInfo.setList(list);
        return groupPageInfo;
    }

    /**
     * 为通道增加图片信息
     */
    private List<CameraChannel> addIconPathForCameraChannelList(List<CameraChannel> channels) {
        return channels;
    }

    public CameraChannel queryOne(String deviceId, String deviceCode, String geoCoordSys) {
        CommonGBChannel channel = null;
        if (deviceCode != null) {
            Device device = deviceMapper.getDeviceByDeviceId(deviceId);
            Assert.notNull(device, "设备不存在：" + deviceCode);
            Integer deviceDbId = device.getId();
            channel = channelMapper.queryByDataIdAndDeviceID(deviceDbId, deviceId);
        }else {
            channel = channelMapper.queryByDeviceId(deviceId);
        }

        if (deviceDbId != null) {
            channel.setDeviceCode(deviceCode);
        }
        if (geoCoordSys != null && channel.getGbLongitude() != null && channel.getGbLatitude() != null
         && channel.getGbLongitude() > 0 && channel.getGbLatitude() > 0) {
            if (geoCoordSys.equalsIgnoreCase("GCJ02")) {
                Double[] position = Coordtransform.WGS84ToGCJ02(channel.getGbLongitude(), channel.getGbLatitude());
                channel.setGbLongitude(position[0]);
                channel.setGbLatitude(position[1]);
            }else if (geoCoordSys.equalsIgnoreCase("BD09")) {
                Double[] gcj02Position = Coordtransform.WGS84ToGCJ02(channel.getGbLongitude(), channel.getGbLatitude());
                Double[] position = Coordtransform.GCJ02ToBD09(gcj02Position[0], gcj02Position[1]);
                channel.setGbLongitude(position[0]);
                channel.setGbLatitude(position[1]);
            }
        }
        return channel;
    }
}
