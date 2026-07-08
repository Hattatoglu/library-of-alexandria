package dev.eyaz.lib.of.alex.service.catalog.domain.usecase.addbook.handler;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCaseHandler;
import dev.eyaz.lib.of.alex.service.catalog.core.exception.InsufficientRoleException;
import dev.eyaz.lib.of.alex.service.catalog.domain.usecase.addbook.port.AddBookNewBookAddedBookEventPort;
import dev.eyaz.lib.of.alex.service.catalog.domain.usecase.addbook.port.AddBookPersistenceBookPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
public class AddBookHandler implements UseCaseHandler<AddBook> {

    private static final Logger log = LoggerFactory.getLogger(AddBookHandler.class);
    Set<String> validRoles = Set.of("SUPER", "ADMIN");

    private final AddBookPersistenceBookPort addBookPersistenceBookPort;
    private final AddBookNewBookAddedBookEventPort addBookNewBookAddedBookEventPort;

    public AddBookHandler(AddBookPersistenceBookPort addBookPersistenceBookPort, AddBookNewBookAddedBookEventPort addBookNewBookAddedBookEventPort) {
        this.addBookPersistenceBookPort = addBookPersistenceBookPort;
        this.addBookNewBookAddedBookEventPort = addBookNewBookAddedBookEventPort;
    }

    @Override
    public AddBook handle(AddBook usecase) {
        log.debug("AddBookHandler initial usecase : {}", usecase.toString());
        AddBook userChecked = checkUserRole(usecase);
        AddBook bookInitiated = initiateBook(userChecked);
        AddBook bookSaved = addBook(bookInitiated);
        AddBook eventPublished = publishNewBookAddedEvent(bookSaved);
        return eventPublished;
    }

    private AddBook checkUserRole(AddBook usecase) {
        boolean authority = usecase.getRoles().stream()
                .anyMatch(validRoles::contains);
        if(authority) {
            return usecase;
        }
        throw new InsufficientRoleException("insufficient role for adding new book operation for user "
                + usecase.getUserId().toString()
                + " with role "
                + usecase.getRoles().toString());
    }

    private AddBook initiateBook(AddBook usecase) {
        usecase.setBookId(UUID.randomUUID());
        return usecase;
    }

    private AddBook addBook(AddBook usecase) {
        return addBookPersistenceBookPort.addNewBook(usecase);
    }

    private AddBook publishNewBookAddedEvent(AddBook usecase) {
        return addBookNewBookAddedBookEventPort.fireNewBookAddedEvent(usecase);
    }

}
