package dev.eyaz.lib.of.alex.artifactory.lib.kafka.model.service.catalog.event;

import java.time.LocalDateTime;
import java.util.UUID;

public class NewBookAddedEvent {

    private final UUID eventId;
    private final UUID correlationId;
    private final String isbn;
    private final String bookName;
    private final String authorName;
    private final int publishYear;
    private final boolean bc;
    private final String type;
    private final String status;
    private final String addedAt;

    public NewBookAddedEvent(UUID eventId, UUID correlationId, String isbn, String bookName, String authorName, int publishYear, boolean bc, String type, String status, String addedAt) {
        this.eventId = eventId;
        this.correlationId = correlationId;
        this.isbn = isbn;
        this.bookName = bookName;
        this.authorName = authorName;
        this.publishYear = publishYear;
        this.bc = bc;
        this.type = type;
        this.status = status;
        this.addedAt = addedAt;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getBookName() {
        return bookName;
    }

    public String getAuthorName() {
        return authorName;
    }

    public int getPublishYear() {
        return publishYear;
    }

    public boolean isBc() {
        return bc;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public String getAddedAt() {
        return addedAt;
    }
}
