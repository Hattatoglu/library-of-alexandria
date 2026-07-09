package dev.eyaz.lib.of.alex.service.catalog.infra.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.eyaz.lib.of.alex.artifactory.lib.kafka.model.service.catalog.event.NewBookAddedEvent;
import dev.eyaz.lib.of.alex.artifactory.lib.outbox.model.OutboxMessage;
import dev.eyaz.lib.of.alex.artifactory.lib.outbox.service.OutboxService;
import dev.eyaz.lib.of.alex.service.catalog.domain.usecase.addbook.handler.AddBook;
import dev.eyaz.lib.of.alex.service.catalog.domain.usecase.addbook.port.AddBookUsecaseNewBookAddedEventOutboxPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AddBookUsecaseNewBookAddedEventOutboxPortAdapter implements
        AddBookUsecaseNewBookAddedEventOutboxPort {

    private final OutboxService outboxService;

    @Value("${service-catalog-publisher-messaging.topic.service-catalog-newbook-topic-name}")
    private String TOPIC;

    public AddBookUsecaseNewBookAddedEventOutboxPortAdapter(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @Override
    public AddBook fireNewBookAddedEvent(AddBook usecase) {

        NewBookAddedEvent event = new NewBookAddedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                usecase.getIsbn(),
                usecase.getBookName(),
                usecase.getAuthor(),
                usecase.getPublishYear(),
                usecase.isBc(),
                usecase.getBookType().getValue(),
                usecase.getStatus().getValue(),
                usecase.getCreatedAt().toString()
        );

        OutboxMessage message = new OutboxMessage();
        message.setTopic(TOPIC);
        message.setService("service-catalog");
        message.setMessage(getEventBodyPayload(event));

        outboxService.send(message);

        return usecase;
    }

    private String getEventBodyPayload(NewBookAddedEvent event) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
