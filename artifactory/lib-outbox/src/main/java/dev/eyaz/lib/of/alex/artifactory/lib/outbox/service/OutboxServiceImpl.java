package dev.eyaz.lib.of.alex.artifactory.lib.outbox.service;

import dev.eyaz.lib.of.alex.artifactory.lib.outbox.model.OutboxEntity;
import dev.eyaz.lib.of.alex.artifactory.lib.outbox.model.OutboxMessage;
import dev.eyaz.lib.of.alex.artifactory.lib.outbox.model.OutboxStatus;
import dev.eyaz.lib.of.alex.artifactory.lib.outbox.repository.OutboxRepository;
import org.springframework.stereotype.Component;

@Component
public class OutboxServiceImpl implements OutboxService{

    private final OutboxRepository outboxRepository;

    public OutboxServiceImpl(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Override
    public void send(OutboxMessage message) {
        OutboxEntity entity = new OutboxEntity();
        entity.setOutboxStatus(OutboxStatus.STARTED.getValue());
        entity.setTopic(message.getTopic());
        entity.setMessage(message.getMessage());
        entity.setService(message.getService());

        outboxRepository.save(entity);
    }
}
