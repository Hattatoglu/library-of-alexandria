package dev.eyaz.lib.of.alex.service.catalog.infra.rest.api;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCaseHandler;
import dev.eyaz.lib.of.alex.service.catalog.core.enums.BookType;
import dev.eyaz.lib.of.alex.service.catalog.domain.usecase.addbook.handler.AddBook;
import dev.eyaz.lib.of.alex.service.catalog.infra.rest.dto.response.AddBookResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog")
public class NewBookController {

    private final UseCaseHandler<AddBook> useCaseHandler;

    public NewBookController(UseCaseHandler<AddBook> useCaseHandler) {
        this.useCaseHandler = useCaseHandler;
    }

    @PostMapping("addbook")
    public ResponseEntity<AddBookResponse> addBook(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        String userRole = request.getHeader("X-User-Role");

        AddBook usecase = new AddBook();
        usecase.setUserId(UUID.randomUUID());
        usecase.setRoles(List.of("ADMIN", "USER"));
        usecase.setIsbn("dummy-isbn" + UUID.randomUUID().toString());
        usecase.setBookName("veni vidi vici");
        usecase.setAuthor("Ceaser");
        usecase.setPublishYear(40);
        usecase.setBc(true);
        usecase.setBookType(BookType.HISTORY);

        AddBook answer = useCaseHandler.handle(usecase);

        AddBookResponse response = new AddBookResponse(
                answer.getCreatedAt().toString()
        );

        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }
}
