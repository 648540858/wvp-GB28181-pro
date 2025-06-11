package com.genersoft.iot.vmp.gat1400.kafka.listener;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.Acknowledgment;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import cz.data.viid.be.service.IPublishService;
import cz.data.viid.be.socket.WebSocketEndpoint;
import cz.data.viid.be.task.action.KeepaliveAction;
import cz.data.viid.framework.S3StorageService;
import cz.data.viid.framework.SpringContextHolder;
import cz.data.viid.framework.config.Constants;
import cz.data.viid.framework.domain.dto.ResponseStatusListObject;
import cz.data.viid.framework.domain.dto.ResponseStatusObject;
import cz.data.viid.framework.domain.dto.SubImageList;
import cz.data.viid.framework.domain.entity.VIIDPublish;
import cz.data.viid.framework.domain.entity.VIIDServer;
import cz.data.viid.framework.domain.vo.SubscribeNotificationRequest;
import cz.data.viid.framework.domain.vo.VIIDResponseStatusObject;
import cz.data.viid.kafka.KafkaStartupService;
import cz.data.viid.rpc.SubscribeClient;

public abstract class AbstractMessageListener<T> implements CustomMessageListener {
    private final Logger log = LoggerFactory.getLogger(getClass());
    protected VIIDPublish publish;
    protected VIIDServer server;
    protected SubscribeClient subscribeClient;
    protected KeepaliveAction keepaliveAction;
    protected S3StorageService storageService;
    private final String type;
    private final long startTime;
    private final long endTime;


    public AbstractMessageListener(VIIDPublish publish, String type) {
        this.publish = publish;
        this.startTime = Optional.ofNullable(publish.getBeginTime())
                .map(Date::getTime)
                .map(time -> time - (1000 * 60 * 60 * 8))
                .orElse(0L);
        this.endTime = Optional.ofNullable(publish.getEndTime())
                .map(Date::getTime)
                .orElse(0L);
        this.type = type;
    }

    @Override
    public void configure(VIIDServer setting) {
        this.subscribeClient = SpringContextHolder.getBean(SubscribeClient.class);
        this.keepaliveAction = SpringContextHolder.getBean(KeepaliveAction.class);
        this.server = keepaliveAction.get(this.publish.getServerId()).originVIIDServer();
        this.storageService = SpringContextHolder.getBean(S3StorageService.class);
    }

    @Override
    public void consumer(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
        log.info("接收到{}订阅的{}数据", publish.getServerId(), publish.getSubscribeDetail());
        if (checkConsumeCondition()) {
            List<T> collect = records.stream()
                    .map(ConsumerRecord::value)
                    .map(this::messageConverter)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            try {
                log.info("推送数据给: {}, 数据量: {}", publish.getServerId(), collect.size());
                this.notificationRequest(collect);
                ack.acknowledge();
            } catch (Exception e) {
                SpringContextHolder.getBean(KafkaStartupService.class).delayPublish(this.publish);
                log.warn("视图库{}订阅消费出现错误,休眠一分钟", publish.getServerId());
                log.error(e.getMessage(), e);
            }
        } else {
            SpringContextHolder.getBean(KafkaStartupService.class).stopPublish(this.publish);
        }
    }

    public abstract T messageConverter(String value);

    public abstract SubscribeNotificationRequest packHandler(List<T> partition);

    protected void notificationRequest(List<T> collect) throws IOException {
        if (Constants.VIID_SERVER.TRANSMISSION.WEBSOCKET.equals(server.getTransmission())) {
            String serverId = publish.getServerId();
            JSONObject payload = new JSONObject();
            payload.put("type", this.type);
            payload.put("data", collect);
            WebSocketEndpoint.sendMessageToServerId(serverId, payload.toJSONString());
        } else if (Constants.VIID_SERVER.TRANSMISSION.HTTP.equals(server.getTransmission())) {
            SubscribeNotificationRequest request = this.packHandler(collect);
            VIIDResponseStatusObject response = subscribeClient.subscribeNotifications(URI.create(publish.getReceiveAddr()), request);
            this.resolverResponse(response);
        }
    }

    protected boolean checkConsumeCondition() {
        long currentTimestamp = System.currentTimeMillis();
        if (currentTimestamp > startTime) {
            if (currentTimestamp < endTime) {
                //检查是否在线
                return this.keepaliveAction.online(publish.getServerId());
            } else {
                //订阅已过期
                SpringContextHolder.getBean(IPublishService.class)
                        .updateSubscribe(publish.getSubscribeId(), Constants.SubscribeStatus.Expire);
                return false;
            }
        } else {
            //还未到达订阅开始时间
            log.warn("视图库[{}]订阅任务[{}]还未到达开始时间", publish.getServerId(), publish.getTitle());
            return false;
        }
    }

    protected void resolverResponse(VIIDResponseStatusObject response) {
        Optional.ofNullable(response)
                .map(VIIDResponseStatusObject::getResponseStatusListObject)
                .map(ResponseStatusListObject::getResponseStatusObject)
                .filter(CollectionUtils::isNotEmpty)
                .ifPresent(list -> {
                    for (ResponseStatusObject statusObject : list) {
                        if (!"0".equals(statusObject.getStatusCode())) {
                            log.warn("订阅通知远程调用错误: {}", statusObject);
                        }
                    }
                });
    }

    protected T wrap(T container, Function<T, SubImageList> function) {
        SubImageList subImageList = function.apply(container);
        if (Constants.ImageDeclare.Base64.equalsValue(publish.getResultImageDeclare())) {
            //base64图片推送
            storageService.subImageListRestore(subImageList);
        } else if (Constants.ImageDeclare.Url.equalsValue(publish.getResultImageDeclare())){
            //URL图片推送
            storageService.subImageListStorage(null, subImageList);
        } else {
            //默认情况下使用base64
            storageService.subImageListRestore(subImageList);
        }
        return container;
    }
}
