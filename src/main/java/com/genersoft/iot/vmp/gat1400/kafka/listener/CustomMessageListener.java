package com.genersoft.iot.vmp.gat1400.kafka.listener;

import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDServer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;


public interface CustomMessageListener {

    default void configure(VIIDServer setting) {}

    void consumer(List<ConsumerRecord<String, String>> records, Acknowledgment ack);

    default void scheduler() {}
}
