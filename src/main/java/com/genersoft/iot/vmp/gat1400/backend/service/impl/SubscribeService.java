package com.genersoft.iot.vmp.gat1400.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import cz.data.viid.be.service.IPublishService;
import cz.data.viid.be.service.ISubscribeService;
import cz.data.viid.be.task.action.KeepaliveAction;
import cz.data.viid.fe.security.SecurityContext;
import cz.data.viid.framework.config.Constants;
import cz.data.viid.framework.domain.dto.ResponseStatusObject;
import cz.data.viid.framework.domain.dto.SubscribeObject;
import cz.data.viid.framework.domain.entity.VIIDPublish;
import cz.data.viid.framework.domain.entity.VIIDServer;
import cz.data.viid.kafka.KafkaStartupService;
import cz.data.viid.utils.DurationUtil;
import cz.data.viid.utils.StructCodec;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SubscribeService implements ISubscribeService {

    @Autowired
    IPublishService publishService;
    @Autowired
    KafkaStartupService kafkaStartupService;

    @Override
    public ResponseStatusObject upsertSubscribes(SubscribeObject subscribeObject) {
        if (Constants.SubscribeOperateType.Cancel.equalsValue(subscribeObject.getOperateType())) {
            return cancelSubscribes(subscribeObject.getSubscribeId());
        }
        String message = "操作失败";
        VIIDServer server = SecurityContext.requireVIIDServer();
        VIIDPublish publish = StructCodec.castPublish(subscribeObject);
        publish.setSubscribeStatus(Constants.SubscribeStatus.In.getValue());
        try {
            if (Constants.ServerProxyNetwork.Boundary.equalsValue(server.getProxyNetwork())) {
                //TODO: 海康1400跨网订阅bug, fixed: 跨网边界锁死回调地址host为配置的视图库host, 订阅资源指定为当前视图库
                this.fixSubscribe(publish, server);
            }
            publish.setCreateTime(new Date());
            publish.setServerId(SecurityContext.getRequestDeviceId());
            boolean save = publishService.saveOrUpdate(publish);
            if (save) {
                kafkaStartupService.stopPublish(publish);
                //todo fixed: 异步等待接口返回在调度kafka, 防止请求方未成功落库订阅
                DurationUtil.schedule(Duration.ofSeconds(3), () -> kafkaStartupService.startPublish(publish));
                return new ResponseStatusObject(publish.getSubscribeId(), null, "0", "操作成功");
            }
        } catch (Exception e) {
            //失败补偿
            this.cancelSubscribes(publish.getSubscribeId());
            message = e.getMessage();
            log.error(message, e);
        }
        return new ResponseStatusObject(publish.getSubscribeId(), null, "1", message);
    }

    public void fixSubscribe(VIIDPublish publish, VIIDServer server) {
        URI uri = URI.create(publish.getReceiveAddr());
        if (!StringUtils.equals(uri.getHost(), server.getHost())) {
            String url = String.format("%s://%s:%s%s", uri.getScheme(), server.getHost(), server.getPort(), uri.getPath());
            if (StringUtils.isNotBlank(uri.getQuery())) {
                url += "?" + uri.getQuery();
            }
            publish.setReceiveAddr(url);
        }
        publish.setResourceUri(KeepaliveAction.CURRENT_SERVER_ID);
    }

    @Override
    public ResponseStatusObject cancelSubscribes(String subscribeId) {
        try {
            VIIDPublish publish = publishService.getById(subscribeId);
            if (Objects.nonNull(publish)) {
                kafkaStartupService.stopPublish(publish);
                publishService.removeById(subscribeId);
                return new ResponseStatusObject(subscribeId, null, "0", "操作成功");
            } else {
                return new ResponseStatusObject(subscribeId, null, "1", "不存在订阅ID:" + subscribeId);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return new ResponseStatusObject(subscribeId, null, "1", "操作失败");
    }

    @Override
    public List<SubscribeObject> getSubscribes(SubscribeObject request) {
        QueryWrapper<VIIDPublish> wrapper = new QueryWrapper<>();
        String subscribeId = request.getSubscribeId();
        wrapper.lambda().eq(StringUtils.isNotBlank(subscribeId), VIIDPublish::getSubscribeId, subscribeId);
        String title = request.getTitle();
        wrapper.lambda().eq(StringUtils.isNotBlank(title), VIIDPublish::getTitle, title);
        String applicationName = request.getApplicationName();
        wrapper.lambda().eq(StringUtils.isNotBlank(applicationName), VIIDPublish::getApplicationName, applicationName);
        String applicationOrg = request.getApplicationOrg();
        wrapper.lambda().eq(StringUtils.isNotBlank(applicationOrg), VIIDPublish::getApplicationOrg, applicationOrg);
        return publishService.list(wrapper)
                .stream()
                .map(StructCodec::castSubscribe)
                .collect(Collectors.toList());
    }
}
