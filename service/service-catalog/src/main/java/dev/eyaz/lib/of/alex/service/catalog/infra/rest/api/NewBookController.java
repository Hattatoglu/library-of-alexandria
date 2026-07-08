package dev.eyaz.lib.of.alex.service.catalog.infra.rest.api;

import dev.eyaz.lib.of.alex.service.catalog.infra.rest.dto.response.AddBookResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/catalog")
public class NewBookController {

    @PostMapping("addbook")
    public ResponseEntity<AddBookResponse> addBook(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        String userRole = request.getHeader("X-User-Role");

        return null;
    }
}
