package com.genersoft.iot.vmp.web.custom.service;

import com.genersoft.iot.vmp.gb28181.bean.Group;
import com.genersoft.iot.vmp.gb28181.dao.CommonGBChannelMapper;
import com.genersoft.iot.vmp.gb28181.dao.GroupMapper;
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
    private GroupMapper groupMapper;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Override
    public void run(String... args) throws Exception {
        // 启动时获取全局token

    }

    public PageInfo<CameraChannel> queryList(Integer page, Integer count, String query, String sortName, String order, String groupAlias, String topGroupAlias, Boolean status, Boolean containMobileDevice) {

        // 构建组织结构信息
        String groupDeviceId = null;
        if (topGroupAlias != null && groupAlias != null) {
            // 根据别名获取分组信息
            Group businessGroup = groupMapper.queryGroupByAlias(topGroupAlias);
            Assert.notNull(businessGroup, "域信息未找到");

            Group group = groupMapper.queryGroupByAliasAndBusinessGroup(groupAlias, businessGroup.getDeviceId());
            Assert.notNull(businessGroup, "获取组织结构失败");
            groupDeviceId = group.getDeviceId();
        }

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
}
