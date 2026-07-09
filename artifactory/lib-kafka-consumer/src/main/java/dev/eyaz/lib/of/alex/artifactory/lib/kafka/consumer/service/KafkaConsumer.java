package dev.eyaz.lib.of.alex.artifactory.lib.kafka.consumer.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.List;

public interface KafkaConsumer<K, V> {
    void receive(List<ConsumerRecord<K, V>> messages);
}
