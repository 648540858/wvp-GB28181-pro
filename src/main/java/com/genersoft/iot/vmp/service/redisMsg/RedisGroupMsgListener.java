package com.genersoft.iot.vmp.service.redisMsg;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.Group;
import com.genersoft.iot.vmp.gb28181.bean.RedisGroupMessage;
import com.genersoft.iot.vmp.gb28181.service.IGroupService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 接收redis发送的推流设备列表更新通知
 * 监听： SUBSCRIBE VM_MSG_GROUP_LIST_RESPONSE
 * 发布 PUBLISH VM_MSG_GROUP_LIST_RESPONSE '[{"groupName":"研发AAS","topGroupGAlias":"6","groupAlias":"6"},{"groupName":"测试AAS","topGroupGAlias":"5","groupAlias":"5"},{"groupName":"研发2","topGroupGAlias":"4","groupAlias":"4"},{"groupName":"啊实打实的","topGroupGAlias":"4","groupAlias":"100000009"},{"groupName":"测试域","topGroupGAlias":"3","groupAlias":"3"},{"groupName":"结构1","topGroupGAlias":"3","groupAlias":"100000000"},{"groupName":"结构1-1","topGroupGAlias":"3","parentGAlias":"100000000","groupAlias":"100000001"},{"groupName":"结构2-2","topGroupGAlias":"3","groupAlias":"100000002"},{"groupName":"结构1-2","topGroupGAlias":"3","parentGAlias":"100000001","groupAlias":"100000003"},{"groupName":"结构1-3","topGroupGAlias":"3","parentGAlias":"100000003","groupAlias":"100000004"},{"groupName":"研发1","topGroupGAlias":"3","groupAlias":"100000005"},{"groupName":"研发3","topGroupGAlias":"3","parentGAlias":"100000005","groupAlias":"100000006"},{"groupName":"测试42","topGroupGAlias":"3","parentGAlias":"100000000","groupAlias":"100000010"},{"groupName":"测试2","topGroupGAlias":"3","parentGAlias":"100000000","groupAlias":"100000011"},{"groupName":"测试3","topGroupGAlias":"3","parentGAlias":"100000000","groupAlias":"100000007"},{"groupName":"测试4","topGroupGAlias":"3","parentGAlias":"100000007","groupAlias":"100000008"}]'
 */
@Slf4j
@Component
public class RedisGroupMsgListener implements MessageListener {

    @Resource
    private IGroupService groupService;

    @Resource
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private SipConfig sipConfig;

    private final ConcurrentLinkedQueue<Message> taskQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void onMessage(Message message, byte[] bytes) {
        String serverId = redisCatchStorage.chooseOneServer(null);
        if (!userSetting.getServerId().equals(serverId)) {
            return;
        }
        log.info("[REDIS: 业务分组同步回复] key： {}， ： {}", VideoManagerConstants.VM_MSG_GROUP_LIST_RESPONSE, new String(message.getBody()));
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
            log.warn("[REDIS消息-业务分组同步回复] 处理队列时发现队列为空");
            return;
        }
        // 按照别名获取所有业务分组
        Map<String, Group> aliasGroupMap = groupService.queryGroupByAliasMap();
        Map<String, Group> aliasGroupToSave = new LinkedHashMap<>();
        for (Message msg : messageDataList) {
            try {
                log.info("[REDIS消息-业务分组同步回复] 处理数据:  {}", new String(msg.getBody()));
                List<RedisGroupMessage> groupMessages = JSON.parseArray(new String(msg.getBody()), RedisGroupMessage.class);
                log.info("[REDIS消息-业务分组同步回复] 待处理数量:  {}", groupMessages.size());
                for (RedisGroupMessage groupMessage : groupMessages) {

                    // 此处使用别名作为判断依据，别名此处常常是分组在第三方系统里的唯一ID
                    if (groupMessage.getGroupAlias() == null || ObjectUtils.isEmpty(groupMessage.getGroupName())
                            || ObjectUtils.isEmpty(groupMessage.getTopGroupGAlias())) {
                        log.info("[REDIS消息-业务分组同步回复] 消息关键字段缺失， {}", groupMessage.toString());
                        continue;
                    }
                    boolean isTop = groupMessage.getTopGroupGAlias().equals(groupMessage.getGroupAlias());
                    Group group = aliasGroupMap.get(groupMessage.getGroupAlias());
                    if (group == null) {
                        group = new Group();
                        String deviceId = buildGroupDeviceId(isTop);
                        group.setDeviceId(deviceId);
                        group.setAlias(groupMessage.getGroupAlias());
                        group.setName(groupMessage.getGroupName());
                        group.setCreateTime(DateUtil.getNow());
                    }

                    if (!isTop) {
                        if (ObjectUtils.isEmpty(groupMessage.getTopGroupGAlias())) {
                            log.info("[REDIS消息-业务分组同步回复] 消息缺失业务分组别名， {}", groupMessage.toString());
                            continue;
                        }

                        Group topGroup = aliasGroupMap.get(groupMessage.getTopGroupGAlias());
                        if (topGroup == null) {
                            topGroup = aliasGroupToSave.get(groupMessage.getTopGroupGAlias());
                        }
                        if (topGroup == null) {
                            log.info("[REDIS消息-业务分组同步回复] 业务分组信息未发送或者未首先发送， {}", groupMessage.toString());
                            continue;
                        }
                        group.setBusinessGroup(topGroup.getDeviceId());
                        if (groupMessage.getParentGAlias() != null) {
                            Group parentGroup = aliasGroupMap.get(groupMessage.getParentGAlias());
                            if (parentGroup == null) {
                                parentGroup = aliasGroupToSave.get(groupMessage.getParentGAlias());
                            }
                            if (parentGroup == null) {
                                log.info("[REDIS消息-业务分组同步回复] 虚拟组织父节点未发送或者未首先发送， {}", groupMessage.toString());
                                continue;
                            }
                            group.setParentId(null);
                            group.setParentDeviceId(parentGroup.getDeviceId());
                        } else {
                            group.setParentId(null);
                            group.setParentDeviceId(topGroup.getDeviceId());
                        }
                    } else {
                        group.setParentId(null);
                        group.setBusinessGroup(group.getDeviceId());
                        group.setParentDeviceId(null);
                    }
                    group.setUpdateTime(DateUtil.getNow());
                    aliasGroupToSave.put(group.getAlias(), group);
                }
                // 存储分组数据
                groupService.saveByAlias(aliasGroupToSave.values());

            } catch (ControllerException e) {
                log.warn("[REDIS消息-业务分组同步回复] 失败, \r\n{}", e.getMsg());
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
