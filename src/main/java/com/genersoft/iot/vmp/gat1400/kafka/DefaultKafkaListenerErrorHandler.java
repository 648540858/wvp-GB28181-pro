package com.genersoft.iot.vmp.gat1400.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConsumerAwareListenerErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.Iterator;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultKafkaListenerErrorHandler implements ConsumerAwareListenerErrorHandler, CommonErrorHandler {

    @Override
    public Object handleError(Message<?> m, ListenerExecutionFailedException e, Consumer<?, ?> c) {
        MessageHeaders headers = m.getHeaders();
        String topic = headers.get(KafkaHeaders.RECEIVED_TOPIC, String.class);
        Integer partitionId = headers.get(KafkaHeaders.RECEIVED_PARTITION_ID, Integer.class);
        Long offset = headers.get(KafkaHeaders.OFFSET, Long.class);
        if (Objects.nonNull(partitionId) && Objects.nonNull(offset)) {
            log.error("Kafka消费者出错: {}, 回撤主题:{}分区:{}偏移量至:{}", e.getMessage(), topic, partitionId, offset);
            c.seek(new org.apache.kafka.common.TopicPartition(topic, partitionId), offset);
        }
        return null;
    }

    @Override
    public void handleBatch(Exception e, ConsumerRecords<?, ?> records, Consumer<?, ?> consumer,
                            MessageListenerContainer container, Runnable invokeListener) {
        if (!records.isEmpty()) {
            for (TopicPartition topicPartition : records.partitions()) {
                Iterator<? extends ConsumerRecord<?, ?>> iterator = records.records(topicPartition.topic()).iterator();
                if (iterator.hasNext()) {
                    ConsumerRecord<?, ?> record = iterator.next();
                    String topic = record.topic();
                    int partition = topicPartition.partition();
                    long offset = record.offset();
                    log.error("Kafka消费者出错: {}, 回撤主题:{}分区:{}偏移量至:{}", e.getMessage(), topic, partition, offset);
                    consumer.seek(new org.apache.kafka.common.TopicPartition(topic, partition), offset);
                }
            }
        }
    }
}
