package dev.eyaz.lib.of.alex.service.auth.infra.rest.api;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCaseHandler;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.createuser.handler.CreateUser;
import dev.eyaz.lib.of.alex.service.auth.infra.rest.dto.request.CreateUserRequest;
import dev.eyaz.lib.of.alex.service.auth.infra.rest.dto.response.CreateUserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.HttpExchange;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/auth")
public class RegisterController {

    private final UseCaseHandler<CreateUser> useCaseHandler;

    public RegisterController(UseCaseHandler<CreateUser> useCaseHandler) {
        this.useCaseHandler = useCaseHandler;
    }

    @PostMapping("/register")
    public ResponseEntity<CreateUserResponse> register(@Valid @RequestBody CreateUserRequest request) {
        CreateUser usecase = new CreateUser();
        usecase.setName(request.getName());
        usecase.setUsername(request.getUsername());
        usecase.setPassword(request.getPassword());
        usecase.setEmail(request.getEmail());
        usecase.setBirthday(
                LocalDate.parse(
                        request.getBirthday(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd")
                ).atStartOfDay()
        );

        CreateUser answer = useCaseHandler.handle(usecase);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
