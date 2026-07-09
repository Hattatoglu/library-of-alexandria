package dev.eyaz.lib.of.alex.artifactory.lib.outbox.model;

public class OutboxMessage {

    private String message;
    private String service;
    private String topic;

    public OutboxMessage() {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
