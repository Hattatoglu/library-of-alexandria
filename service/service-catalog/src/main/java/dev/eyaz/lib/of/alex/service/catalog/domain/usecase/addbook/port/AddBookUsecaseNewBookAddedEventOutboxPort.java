package dev.eyaz.lib.of.alex.service.catalog.domain.usecase.addbook.port;

import dev.eyaz.lib.of.alex.service.catalog.domain.usecase.addbook.handler.AddBook;

public interface AddBookUsecaseNewBookAddedEventOutboxPort {
    AddBook fireNewBookAddedEvent(AddBook usecase);
}
