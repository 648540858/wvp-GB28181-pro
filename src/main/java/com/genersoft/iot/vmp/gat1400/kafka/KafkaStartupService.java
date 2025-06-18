package com.genersoft.iot.vmp.gat1400.kafka;

import com.genersoft.iot.vmp.gat1400.backend.service.IPublishService;
import com.genersoft.iot.vmp.gat1400.framework.config.Constants;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDPublish;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDServer;
import com.genersoft.iot.vmp.gat1400.framework.service.VIIDServerService;
import com.genersoft.iot.vmp.gat1400.kafka.listener.APEMessageListener;
import com.genersoft.iot.vmp.gat1400.kafka.listener.CustomMessageListener;
import com.genersoft.iot.vmp.gat1400.kafka.listener.FaceMessageListener;
import com.genersoft.iot.vmp.gat1400.kafka.listener.LaneMessageListener;
import com.genersoft.iot.vmp.gat1400.kafka.listener.MotorVehicleMessageListener;
import com.genersoft.iot.vmp.gat1400.kafka.listener.NonMotorVehicleMessageListener;
import com.genersoft.iot.vmp.gat1400.kafka.listener.PersonMessageListener;
import com.genersoft.iot.vmp.gat1400.kafka.listener.RawMessageListener;
import com.genersoft.iot.vmp.gat1400.kafka.listener.TollgateMessageListener;
import com.genersoft.iot.vmp.gat1400.utils.DurationUtil;
import com.genersoft.iot.vmp.gat1400.utils.VIIDRandomUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.config.MethodKafkaListenerEndpoint;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;


@Order(1)
@Component
public class KafkaStartupService {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final DefaultKafkaListenerErrorHandler errorHandler = new DefaultKafkaListenerErrorHandler();
    private final StringMessageConverter converter = new StringMessageConverter();
    private final MessageHandlerMethodFactory methodFactory = new DefaultMessageHandlerMethodFactory();
    @Resource
    KafkaListenerEndpointRegistry registry;
    @Resource
    KafkaListenerContainerFactory<MessageListenerContainer> factory;
    @Resource
    IPublishService publishService;
    @Resource
    VIIDServerService viidServerService;
    @Lazy
    @Resource
    AdminClient kafkaAdminClient;

    public void register(VIIDPublish publish) {
        if (Constants.SubscribeStatus.In.equalsValue(publish.getSubscribeStatus())) {
            doRegister(publish);
        }
    }

    public void serverPublishIdle(String serverId) {
        List<VIIDPublish> publishList = publishService.findListByServerId(serverId, Constants.SubscribeStatus.In.getValue());
        publishList.forEach(this::startPublish);
    }

    public void serverPublishStop(String serverId) {
        log.info("视图库{}数据发布停止", serverId);
        List<VIIDPublish> publishList = publishService.findListByServerId(serverId, null);
        publishList.forEach(this::stopPublish);
    }

    public void startPublish(VIIDPublish publish) {
        publish = publishService.getById(publish.getSubscribeId());
        if (Objects.nonNull(publish) && Constants.SubscribeStatus.In.equalsValue(publish.getSubscribeStatus())) {
            Set<String> details = Stream.of(StringUtils.split(publish.getSubscribeDetail(), ",")).collect(Collectors.toSet());
            for (String detail : details) {
                String id = this.listenerIdBuilder(publish.getSubscribeId(), detail);
                MessageListenerContainer listenerContainer = registry.getListenerContainer(id);
                if (Objects.isNull(listenerContainer)) {
                    this.register(publish);
                    listenerContainer = registry.getListenerContainer(id);
                    if (Objects.isNull(listenerContainer))
                        continue;
                }
                boolean running = listenerContainer.isRunning();
                if (!running) {
                    listenerContainer.start();
                }
            }
        }
    }

    public void stopPublish(VIIDPublish publish) {
        Set<String> containerIds = registry.getListenerContainerIds();
        for (String containerId : containerIds) {
            if (containerId.startsWith(publish.getSubscribeId())) {
                MessageListenerContainer listenerContainer = registry.getListenerContainer(containerId);
                if (Objects.nonNull(listenerContainer) && listenerContainer.isRunning()) {
                    listenerContainer.stop();
                }
                registry.unregisterListenerContainer(containerId);
            }
        }
    }

    public void delayPublish(VIIDPublish publish) {
        this.stopPublish(publish);
        DurationUtil.schedule(Duration.ofSeconds(60), () -> this.startPublish(publish));
    }

    public String description(VIIDPublish publish) {
        String subscribeId = publish.getSubscribeId();
        String subscribeDetail = publish.getSubscribeDetail();
        Set<String> details = Stream.of(StringUtils.split(subscribeDetail, ","))
                .collect(Collectors.toSet());
        StringBuilder sb = new StringBuilder();
        for (String detail : details) {
            if (sb.length() > 0) {
                sb.append(";");
            }
            String id = this.listenerIdBuilder(subscribeId, detail);
            MessageListenerContainer listenerContainer = registry.getListenerContainer(id);
            if (Objects.nonNull(listenerContainer)) {
                boolean running = listenerContainer.isRunning();
                sb.append("订阅类型[")
                        .append(
                                Optional.ofNullable(Constants.SubscribeDetail.match(detail))
                                        .map(Constants.SubscribeDetail::getDescribe)
                                        .orElse("未知类型" + detail)
                        )
                        .append("]").append(running ? "正常运行" : "暂停中");
            } else {
                sb.append("订阅类型[")
                        .append(
                                Optional.ofNullable(Constants.SubscribeDetail.match(detail))
                                        .map(Constants.SubscribeDetail::getDescribe)
                                        .orElse("未知类型" + detail)
                        )
                        .append("]未注册");
            }
        }
        return sb.toString();
    }

    public Map<String, PusherMetric> metric(VIIDPublish subscribe) {
        Set<Pattern> patterns = VIIDRandomUtil.getSubscribeDetailPatterns(subscribe.getResourceUri(), subscribe.getSubscribeDetail());
        Map<String, PusherMetric> metrics = new HashMap<>();
        try {
            String groupId = subscribe.getSubscribeId();
            //获取订阅消费者所有topic偏移量
            Map<TopicPartition, OffsetAndMetadata> partitionMap = kafkaAdminClient.listConsumerGroupOffsets(groupId)
                    .partitionsToOffsetAndMetadata()
                    .get();
            Map<TopicPartition, OffsetSpec> offsetSpecMap = new HashMap<>();
            if (partitionMap.isEmpty()) {
                //不存在消费记录,直接统计所有topic总和
                List<String> topics = kafkaAdminClient.listTopics()
                        .listings()
                        .get()
                        .stream()
                        .map(TopicListing::name)
                        .filter(name -> patterns.stream().map(pattern -> pattern.matcher(name)).anyMatch(Matcher::find))
                        .collect(Collectors.toList());
                if (!topics.isEmpty()) {
                    Map<String, TopicDescription> topicDescriptionMap = kafkaAdminClient.describeTopics(topics)
                            .allTopicNames()
                            .get();
                    for (Map.Entry<String, TopicDescription> entry : topicDescriptionMap.entrySet()) {
                        String topic = entry.getKey();
                        TopicDescription description = entry.getValue();
                        for (TopicPartitionInfo partition : description.partitions()) {
                            offsetSpecMap.put(new TopicPartition(topic, partition.partition()), OffsetSpec.latest());
                            PusherMetric metric = metrics.computeIfAbsent(topic, PusherMetric::create);
                            metric.setSubscribeId(subscribe.getSubscribeId());
                        }
                    }
                }
            } else {
                //存在消费记录
                for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : partitionMap.entrySet()) {
                    TopicPartition p = entry.getKey();
                    OffsetAndMetadata value = entry.getValue();
                    //过滤非当前消费topic
                    boolean anyMatch = patterns.stream().map(pattern -> pattern.matcher(p.topic())).anyMatch(Matcher::find);
                    if (anyMatch) {
                        offsetSpecMap.put(p, OffsetSpec.latest());
                        PusherMetric metric = metrics.computeIfAbsent(p.topic(), PusherMetric::create);
                        metric.setSubscribeId(subscribe.getSubscribeId());
                        metric.setCurOffset(metric.getCurOffset() + value.offset());
                    }
                }
            }
            if (!offsetSpecMap.isEmpty()) {
                kafkaAdminClient.listOffsets(offsetSpecMap)
                        .all()
                        .get()
                        .forEach((k, v) -> {
                            PusherMetric metric = metrics.get(k.topic());
                            if (metric != null) {
                                metric.setMaxOffset(v.offset());
                            }
                        });
            }
        } catch (Exception e) {
            log.warn("获取上级订阅推送情况错误: {}", e.getMessage(), e);
        }
        return metrics;
    }

    private synchronized void doRegister(VIIDPublish publish) {
        VIIDServer setting = viidServerService.getCurrentServer();
        Set<Constants.SubscribeDetail> details = Stream.of(StringUtils.split(publish.getSubscribeDetail(), ","))
                .map(Constants.SubscribeDetail::match)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (Constants.SubscribeDetail detail : details) {
            String endpointId = this.listenerIdBuilder(publish.getSubscribeId(), detail.getValue());
            Set<String> containerIds = registry.getListenerContainerIds();
            if (containerIds.contains(endpointId))
                continue;
            MethodKafkaListenerEndpoint<String, String> endpoint = new MethodKafkaListenerEndpoint<>();
            CustomMessageListener messageListener;
            String prefix;
            switch (detail) {
                case DEVICE:
                    messageListener = new APEMessageListener(publish);
                    prefix = Constants.DEFAULT_TOPIC_PREFIX.APE_DEVICE;
                    break;
                case TOLLGATE:
                    messageListener = new TollgateMessageListener(publish);
                    prefix = Constants.DEFAULT_TOPIC_PREFIX.TOLLGATE_DEVICE;
                    break;
                case Lanes:
                    messageListener = new LaneMessageListener(publish);
                    prefix = Constants.DEFAULT_TOPIC_PREFIX.LANE;
                    break;
                case FACE_INFO:
                    messageListener = new FaceMessageListener(publish);
                    prefix = Constants.DEFAULT_TOPIC_PREFIX.FACE_RECORD;
                    break;
                case PERSON_INFO:
                    messageListener = new PersonMessageListener(publish);
                    prefix = Constants.DEFAULT_TOPIC_PREFIX.PERSON_RECORD;
                    break;
                case PLATE_INFO:
                    messageListener = new MotorVehicleMessageListener(publish);
                    prefix = Constants.DEFAULT_TOPIC_PREFIX.MOTOR_VEHICLE;
                    break;
                case PLATE_MIRCO_INFO:
                    messageListener = new NonMotorVehicleMessageListener(publish);
                    prefix = Constants.DEFAULT_TOPIC_PREFIX.NON_MOTOR_VEHICLE;
                    break;
                case RAW:
                    messageListener = new RawMessageListener(publish);
                    prefix = Constants.DEFAULT_TOPIC_PREFIX.RAW;
                    break;
                default:
                    throw new RuntimeException("没有指定resourceClass");
            }
            messageListener.configure(setting);
            try {
                messageListener.scheduler();
            } catch (Exception e) {
                log.warn("初始化调度错误: {}", e.getMessage());
            }
            Set<String> resourceUri = Arrays.stream(
                    StringUtils.split(publish.getResourceUri(), ",")
            ).map(String::trim).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
            if (resourceUri.contains(setting.getServerId())) {
                endpoint.setTopicPattern(Pattern.compile("^" + prefix + ".*"));
            } else {
                endpoint.setTopics(resourceUri.stream()
                        .map(prefix::concat)
                        .toArray(String[]::new));
            }
            endpoint.setBean(messageListener);
            Method method = ReflectionUtils.findMethod(messageListener.getClass(), "consumer", List.class, Acknowledgment.class);
            if (Objects.isNull(method))
                throw new RuntimeException("消费者没有实现消费方法");
            endpoint.setMethod(method);
            endpoint.setId(endpointId);
            endpoint.setGroupId(publish.getSubscribeId());
            endpoint.setMessagingConverter(converter);
            endpoint.setErrorHandler(errorHandler);
            endpoint.setMessageHandlerMethodFactory(methodFactory);
            Properties properties = new Properties();
            if (Objects.nonNull(publish.getReportInterval())) {
                properties.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, publish.getReportInterval() * 1000);
            }
            endpoint.setConsumerProperties(properties);
            registry.registerListenerContainer(endpoint, factory);
            log.info("视图库{}注册Kafka消费端点,消费类型{}", publish.getServerId(), detail.getValue());
        }
    }

    public String listenerIdBuilder(String subscribeId, String detail) {
        return StringUtils.join(subscribeId, "::", detail);
    }
}
