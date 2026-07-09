package dev.eyaz.lib.of.alex.artifactory.lib.outbox.publisher;

import dev.eyaz.lib.of.alex.artifactory.lib.kafka.producer.service.KafkaProducer;
import dev.eyaz.lib.of.alex.artifactory.lib.outbox.model.OutboxStatus;
import dev.eyaz.lib.of.alex.artifactory.lib.outbox.processer.OutboxProcess;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class OutboxPublisher {

    private final KafkaProducer<String, String> kafkaProducer;

    public OutboxPublisher(KafkaProducer<String, String> kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    public void handle(OutboxProcess process) {
        ProducerRecord<String, String> record = new ProducerRecord<>(process.getEntity().getTopic(), process.getEntity().getId().toString(), process.getEntity().getMessage());

        CompletableFuture<SendResult<String, String>> future = kafkaProducer.sendRecord(record);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                process.getOutboxCallback().accept(process, OutboxStatus.FAILED);
            } else {
                process.getOutboxCallback().accept(process, OutboxStatus.COMPLETED);
            }
        });


    }
}
