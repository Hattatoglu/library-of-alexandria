package dev.eyaz.lib.of.alex.artifactory.lib.outbox.processer;

import dev.eyaz.lib.of.alex.artifactory.lib.outbox.model.OutboxEntity;
import dev.eyaz.lib.of.alex.artifactory.lib.outbox.model.OutboxStatus;

import java.util.function.BiConsumer;

public class OutboxProcess {
    private final OutboxEntity entity;
    private BiConsumer<OutboxProcess, OutboxStatus> outboxCallback;

    public OutboxProcess(OutboxEntity entity) {
        this.entity = entity;
    }

    public OutboxEntity getEntity() {
        return entity;
    }

    public BiConsumer<OutboxProcess, OutboxStatus> getOutboxCallback() {
        return outboxCallback;
    }

    public void setOutboxCallback(BiConsumer<OutboxProcess, OutboxStatus> outboxCallback) {
        this.outboxCallback = outboxCallback;
    }
}
