package com.genersoft.iot.vmp.service.redisMsg;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.Group;
import com.genersoft.iot.vmp.gb28181.bean.RedisGroupMessage;
import com.genersoft.iot.vmp.gb28181.service.IGroupService;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushService;
import com.genersoft.iot.vmp.utils.DateUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @Auther: JiangFeng
 * @Date: 2022/8/16 11:32
 * @Description: 接收redis发送的推流设备列表更新通知
 * 监听：  SUBSCRIBE VM_MSG_GROUP_LIST_CHANGE
 * 发布 PUBLISH VM_MSG_GROUP_LIST_CHANGE  '[{"groupName":"测试域修改新","topGroupGAlias":3,"messageType":"update","groupAlias":3}]'
 */
@Slf4j
@Component
public class RedisGroupChangeListener implements MessageListener {

    @Resource
    private IGroupService groupService;

    @Resource
    private IStreamPushService streamPushService;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private SipConfig sipConfig;

    private final ConcurrentLinkedQueue<Message> taskQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void onMessage(Message message, byte[] bytes) {
        log.info("[REDIS-分组信息改变] key： {}， ： {}", VideoManagerConstants.VM_MSG_GROUP_LIST_CHANGE, new String(message.getBody()));
        taskQueue.offer(message);
    }

    @Scheduled(fixedDelay = 100)
    public void executeTaskQueue() {
        if (taskQueue.isEmpty()) {
            return;
        }
        List<Message> messageDataList = new ArrayList<>();
        int size = taskQueue.size();
        for (int i = 0; i < size; i++) {
            Message msg = taskQueue.poll();
            if (msg != null) {
                messageDataList.add(msg);
            }
        }
        if (messageDataList.isEmpty()) {
            return;
        }
        for (Message msg : messageDataList) {
            try {
                List<RedisGroupMessage> groupMessages = JSON.parseArray(new String(msg.getBody()), RedisGroupMessage.class);
                for (int i = 0; i < groupMessages.size(); i++) {
                    RedisGroupMessage groupMessage = groupMessages.get(i);
                    log.info("[REDIS消息-分组信息更新] {}", groupMessage.toString());
                    switch (groupMessage.getMessageType()){
                        case "add":
                            if (!userSetting.isUseAliasForGroupSync()) {
                                if (groupMessage.getGroupGbId() == null) {
                                    log.info("[REDIS消息-分组信息新增] 分组编号未设置，{}", groupMessage.toString());
                                    continue;
                                }
                                Group group = groupService.queryGroupByDeviceId(groupMessage.getGroupGbId());
                                if (group != null) {
                                    log.info("[REDIS消息-分组信息新增] 失败 {}，编号已经存在", groupMessage.getGroupGbId());
                                    continue;
                                }
                                if (ObjectUtils.isEmpty(groupMessage.getGroupName())
                                        || ObjectUtils.isEmpty(groupMessage.getTopGroupGbId()) ){
                                    log.info("[REDIS消息-分组信息新增] 消息关键字段缺失， {}", groupMessage.toString());
                                    continue;
                                }
                                group = new Group();
                                group.setDeviceId(groupMessage.getGroupGbId());
                                group.setAlias(groupMessage.getGroupAlias());
                                group.setParentDeviceId(groupMessage.getParentGroupGbId());
                                group.setBusinessGroup(groupMessage.getTopGroupGbId());
                                group.setCreateTime(DateUtil.getNow());
                                group.setUpdateTime(DateUtil.getNow());
                                groupService.add(group);

                            }else {
                                // 此处使用别名作为判断依据，别名此处常常是分组在第三方系统里的唯一ID
                                if (groupMessage.getGroupAlias() == null || ObjectUtils.isEmpty(groupMessage.getGroupName())
                                        || ObjectUtils.isEmpty(groupMessage.getTopGroupGAlias())) {
                                    log.info("[REDIS消息-分组信息新增] 消息关键字段缺失， {}", groupMessage.toString());
                                    continue;
                                }
                                Group group = groupService.queryGroupByAlias(groupMessage.getGroupAlias());
                                if (group != null) {
                                    log.info("[REDIS消息-分组信息新增] 失败 {}，别名已经存在", groupMessage.getGroupGbId());
                                    continue;
                                }
                                group = new Group();
                                boolean isTop = groupMessage.getTopGroupGAlias().equals(groupMessage.getGroupAlias());
                                String deviceId = buildGroupDeviceId(isTop);
                                group.setDeviceId(deviceId);
                                group.setAlias(groupMessage.getGroupAlias());
                                group.setName(groupMessage.getGroupName());
                                if (!isTop) {
                                    if (ObjectUtils.isEmpty(groupMessage.getTopGroupGAlias()) ) {
                                        log.info("[REDIS消息-分组信息新增] 消息缺失业务分组别名或者父节点别名， {}", groupMessage.toString());
                                        continue;
                                    }

                                    Group topGroup = groupService.queryGroupByAlias(groupMessage.getTopGroupGAlias());
                                    if (topGroup == null) {
                                        log.info("[REDIS消息-分组信息新增] 业务分组信息未入库， {}", groupMessage.toString());
                                        continue;
                                    }
                                    group.setBusinessGroup(topGroup.getDeviceId());
                                    group.setParentId(topGroup.getId());
                                }
                                if (groupMessage.getParentGAlias() != null) {
                                    Group parentGroup = groupService.queryGroupByAlias(groupMessage.getParentGAlias());
                                    if (parentGroup == null) {
                                        log.info("[REDIS消息-分组信息新增] 虚拟组织父节点信息未入库， {}", groupMessage.toString());
                                        continue;
                                    }
                                    group.setParentId(parentGroup.getId());
                                    group.setParentDeviceId(parentGroup.getDeviceId());
                                }
                                group.setCreateTime(DateUtil.getNow());
                                group.setUpdateTime(DateUtil.getNow());
                                groupService.add(group);
                            }

                            break;
                        case "update":
                            if (!userSetting.isUseAliasForGroupSync()) {
                                if (groupMessage.getGroupGbId() == null) {
                                    log.info("[REDIS消息-分组信息更新] 分组编号未设置，{}", groupMessage.toString());
                                    continue;
                                }
                                Group group = groupService.queryGroupByDeviceId(groupMessage.getGroupGbId());
                                if (group == null) {
                                    log.info("[REDIS消息-分组信息更新] 失败 {}，编号不存在", groupMessage.getGroupGbId());
                                    continue;
                                }
                                group.setDeviceId(groupMessage.getGroupGbId());
                                group.setAlias(groupMessage.getGroupAlias());
                                group.setParentDeviceId(groupMessage.getParentGroupGbId());
                                group.setBusinessGroup(groupMessage.getTopGroupGbId());
                                group.setUpdateTime(DateUtil.getNow());
                                groupService.update(group);
                            }else {
                                // 此处使用别名作为判断依据，别名此处常常是分组在第三方系统里的唯一ID
                                if (groupMessage.getGroupAlias() == null) {
                                    log.info("[REDIS消息-分组信息更新] 消息关键字段缺失， {}", groupMessage.toString());
                                    continue;
                                }
                                Group group = groupService.queryGroupByAlias(groupMessage.getGroupAlias());
                                if (group == null ) {
                                    log.info("[REDIS消息-分组信息更新] 失败 {}，别名不存在", groupMessage.getGroupAlias());
                                    continue;
                                }
                                group.setName(groupMessage.getGroupName());
                                group.setUpdateTime(DateUtil.getNow());
                                if (groupMessage.getParentGAlias() != null) {
                                    Group parentGroup = groupService.queryGroupByAlias(groupMessage.getParentGAlias());
                                    if (parentGroup == null) {
                                        log.info("[REDIS消息-分组信息更新] 虚拟组织父节点信息未入库， {}", groupMessage.toString());
                                        continue;
                                    }
                                    group.setParentId(parentGroup.getId());
                                    group.setParentDeviceId(parentGroup.getDeviceId());
                                }else {
                                    Group businessGroup = groupService.queryGroupByDeviceId(group.getBusinessGroup());
                                    if (businessGroup == null ) {
                                        log.info("[REDIS消息-分组信息更新] 失败 {}，业务分组不存在", groupMessage.getGroupAlias());
                                        continue;
                                    }
                                    group.setParentId(businessGroup.getId());
                                    group.setParentDeviceId(null);
                                }
                                groupService.update(group);
                            }
                            break;
                        case "delete":
                            if (!userSetting.isUseAliasForGroupSync()) {
                                if (groupMessage.getGroupGbId() == null) {
                                    log.info("[REDIS消息-分组信息删除] 分组编号未设置，{}", groupMessage.toString());
                                    continue;
                                }
                                Group group = groupService.queryGroupByDeviceId(groupMessage.getGroupGbId());
                                if (group == null) {
                                    log.info("[REDIS消息-分组信息删除] 失败 {}，编号不存在", groupMessage.getGroupGbId());
                                    continue;
                                }
                                groupService.delete(group.getId());
                            }else {
                                // 此处使用别名作为判断依据，别名此处常常是分组在第三方系统里的唯一ID
                                if (groupMessage.getGroupAlias() == null) {
                                    log.info("[REDIS消息-分组信息删除] 消息关键字段缺失， {}", groupMessage.toString());
                                    continue;
                                }
                                Group group = groupService.queryGroupByAlias(groupMessage.getGroupAlias());
                                if (group == null) {
                                    log.info("[REDIS消息-分组信息删除] 失败 {}，别名不存在", groupMessage.getGroupAlias());
                                    continue;
                                }
                                groupService.delete(group.getId());
                            }
                            break;
                        default:
                            log.info("[REDIS消息-分组信息改变] 未识别的消息类型 {}，目前支持的消息类型为 add、update、delete", groupMessage.getMessageType());
                    }
                }

            } catch (Exception e) {
                log.warn("[REDIS消息-业务分组同步回复] 发现未处理的异常, \r\n{}", new String(msg.getBody()));
                log.error("[REDIS消息-业务分组同步回复] 异常内容： ", e);
            }
        }

    }

    /**
     * 生成分组国标编号
     */
    private String buildGroupDeviceId(boolean isTop) {
        try {
            String deviceTemplate = userSetting.getGroupSyncDeviceTemplate();
            if (ObjectUtils.isEmpty(deviceTemplate) || !deviceTemplate.contains("%s")) {
                String domain = sipConfig.getDomain();
                if (domain.length() != 10) {
                    domain = sipConfig.getId().substring(0, 10);
                }
                deviceTemplate = domain + "%s0%s";
            }
            String codeType = "216";
            if (isTop) {
                codeType = "215";
            }
            return String.format(deviceTemplate, codeType, RandomStringUtils.secureStrong().next(6, false, true));
        }catch (Exception e) {
            log.error("[REDIS消息-业务分组同步回复] 构建新的分组编号失败", e);
            return null;
        }
    }
}
