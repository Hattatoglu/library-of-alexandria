package dev.eyaz.lib.of.alex.artifactory.lib.outbox.service;

import dev.eyaz.lib.of.alex.artifactory.lib.outbox.model.OutboxMessage;

public interface OutboxService {

    void send(OutboxMessage message);
}
