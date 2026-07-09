package dev.eyaz.lib.of.alex.artifactory.lib.outbox.processer;

import dev.eyaz.lib.of.alex.artifactory.lib.outbox.model.OutboxEntity;
import dev.eyaz.lib.of.alex.artifactory.lib.outbox.publisher.OutboxPublisher;
import org.springframework.stereotype.Component;

@Component
public class OutboxProcessor {

    private final OutboxCallbackHelper outboxCallbackHelper;
    private final OutboxPublisher outboxPublisher;

    public OutboxProcessor(OutboxCallbackHelper outboxCallbackHelper,
                           OutboxPublisher outboxPublisher) {
        this.outboxCallbackHelper = outboxCallbackHelper;
        this.outboxPublisher = outboxPublisher;
    }

    public void handle(OutboxEntity entity) {
        OutboxProcess process = new OutboxProcess(entity);
        OutboxProcess answer = outboxCallbackHelper.help(process);
        outboxPublisher.handle(answer);
    }
}
