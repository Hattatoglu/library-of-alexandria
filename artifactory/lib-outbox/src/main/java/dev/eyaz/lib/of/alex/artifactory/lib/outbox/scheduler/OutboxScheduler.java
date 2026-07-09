package dev.eyaz.lib.of.alex.artifactory.lib.outbox.scheduler;

import dev.eyaz.lib.of.alex.artifactory.lib.outbox.model.OutboxEntity;
import dev.eyaz.lib.of.alex.artifactory.lib.outbox.model.OutboxStatus;
import dev.eyaz.lib.of.alex.artifactory.lib.outbox.processer.OutboxProcessor;
import dev.eyaz.lib.of.alex.artifactory.lib.outbox.repository.OutboxRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class OutboxScheduler {

    private final OutboxRepository outboxRepository;
    private final OutboxProcessor outboxProcessor;

    public OutboxScheduler(OutboxRepository outboxRepository, OutboxProcessor outboxProcessor) {
        this.outboxRepository = outboxRepository;
        this.outboxProcessor = outboxProcessor;
    }

    @Transactional
    @Scheduled(fixedDelayString = "5000",
            initialDelayString = "5000")
    public void outboxScheduler() {
        Optional<List<OutboxEntity>> optionalPaymentOutboxEntityList =
                outboxRepository.findByOutboxStatus(OutboxStatus.STARTED.getValue());

        if (optionalPaymentOutboxEntityList.isPresent() &&
                !optionalPaymentOutboxEntityList.get().isEmpty()) {
            optionalPaymentOutboxEntityList.get().forEach(outboxEntity -> {
                outboxProcessor.handle(outboxEntity);
            });
        }
    }
}
