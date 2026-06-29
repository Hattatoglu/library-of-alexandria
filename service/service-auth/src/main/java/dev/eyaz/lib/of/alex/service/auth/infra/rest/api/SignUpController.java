package dev.eyaz.lib.of.alex.service.auth.infra.rest.api;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCaseHandler;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.signupuser.handler.SignUpUser;
import dev.eyaz.lib.of.alex.service.auth.infra.rest.dto.request.CreateUserRequest;
import dev.eyaz.lib.of.alex.service.auth.infra.rest.dto.response.CreateUserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/auth")
public class SignUpController {

    private final UseCaseHandler<SignUpUser> useCaseHandler;

    public SignUpController(UseCaseHandler<SignUpUser> useCaseHandler) {
        this.useCaseHandler = useCaseHandler;
    }

    @PostMapping("/register")
    public ResponseEntity<CreateUserResponse> register(@Valid @RequestBody CreateUserRequest request) {
        SignUpUser usecase = new SignUpUser();
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

        SignUpUser answer = useCaseHandler.handle(usecase);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
