package dev.eyaz.lib.of.alex.artifactory.lib.outbox.model;

import java.util.stream.Stream;

public enum OutboxStatus {
    STARTED("STARTED"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED");

    private final String value;

    OutboxStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static OutboxStatus of(String value) {
        return Stream.of(OutboxStatus.values())
                .filter(outboxStatus -> outboxStatus.value.equals(value))
                .findFirst()
                .orElseThrow();
    }
}
