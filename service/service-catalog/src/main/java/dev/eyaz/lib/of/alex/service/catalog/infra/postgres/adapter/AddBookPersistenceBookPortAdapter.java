package dev.eyaz.lib.of.alex.service.catalog.infra.postgres.adapter;

import dev.eyaz.lib.of.alex.service.catalog.domain.usecase.addbook.handler.AddBook;
import dev.eyaz.lib.of.alex.service.catalog.domain.usecase.addbook.port.AddBookPersistenceBookPort;
import dev.eyaz.lib.of.alex.service.catalog.infra.postgres.model.BookEntity;
import dev.eyaz.lib.of.alex.service.catalog.infra.postgres.repository.BookRepository;
import org.springframework.stereotype.Component;

@Component
public class AddBookPersistenceBookPortAdapter implements AddBookPersistenceBookPort {

    private final BookRepository bookRepository;

    public AddBookPersistenceBookPortAdapter(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public AddBook addNewBook(AddBook usecase) {

        BookEntity entity = new BookEntity();
        entity.setBookId(usecase.getBookId());
        entity.setBookName(usecase.getBookName());
        entity.setAuthor(usecase.getAuthor());
        entity.setPublishYear(usecase.getPublishYear());
        entity.setType(usecase.getBookType());
        entity.setBc(usecase.isBc());
        entity.setStatus(usecase.getStatus());

        BookEntity answer = bookRepository.save(entity);
        usecase.setCreatedAt(answer.getCreatedAt());
        usecase.setUpdatedAt(answer.getUpdatedAt());

        return usecase;
    }
}
