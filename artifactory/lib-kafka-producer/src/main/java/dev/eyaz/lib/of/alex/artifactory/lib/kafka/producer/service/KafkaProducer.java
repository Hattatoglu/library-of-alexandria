package dev.eyaz.lib.of.alex.artifactory.lib.kafka.producer.service;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.support.SendResult;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;

public interface KafkaProducer<K extends Serializable, V extends Serializable> {
    CompletableFuture<SendResult<K, V>> sendRecord(ProducerRecord<K, V> record);
}
