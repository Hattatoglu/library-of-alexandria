package dev.eyaz.lib.of.alex.artifactory.lib.outbox.model;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "outbox")
public class OutboxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "outboxStatus")
    private String outboxStatus;
    @Column(name = "message", length = 1000)
    private String message;
    @Column(name = "topic")
    private String topic;
    @Column(name = "service")
    private String service;

    public OutboxEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getOutboxStatus() {
        return outboxStatus;
    }

    public void setOutboxStatus(String outboxStatus) {
        this.outboxStatus = outboxStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }
}
