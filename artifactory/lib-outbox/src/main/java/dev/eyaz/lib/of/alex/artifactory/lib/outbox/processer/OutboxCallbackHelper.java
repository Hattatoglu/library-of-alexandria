package dev.eyaz.lib.of.alex.artifactory.lib.outbox.processer;

import dev.eyaz.lib.of.alex.artifactory.lib.outbox.model.OutboxStatus;
import dev.eyaz.lib.of.alex.artifactory.lib.outbox.repository.OutboxRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxCallbackHelper {

    private final OutboxRepository outboxRepository;

    public OutboxCallbackHelper(OutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public OutboxProcess help(OutboxProcess process) {
        process.setOutboxCallback(this::updateOutboxStatus);
        return process;
    }

    @Transactional
    private void updateOutboxStatus(OutboxProcess process, OutboxStatus outboxStatus) {
        outboxRepository.updateOutboxStatusById(process.getEntity().getId(), outboxStatus.getValue());
    }
}
