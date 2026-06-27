package dev.eyaz.lib.of.alex.service.auth.infra.rest.api;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCaseHandler;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.createuser.handler.CreateUser;
import dev.eyaz.lib.of.alex.service.auth.infra.rest.cookie.CookieProvider;
import dev.eyaz.lib.of.alex.service.auth.infra.rest.dto.CreateUserRequest;
import dev.eyaz.lib.of.alex.service.auth.infra.rest.dto.CreateUserResponse;
import dev.eyaz.lib.of.alex.service.auth.infra.security.config.CookieProperties;
import dev.eyaz.lib.of.alex.service.auth.infra.security.config.JwtProperties;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
public class RegisterController {

    private final UseCaseHandler<CreateUser> useCaseHandler;
    private final CookieProvider cookieProvider;

    public RegisterController(UseCaseHandler<CreateUser> useCaseHandler, CookieProvider cookieProvider) {
        this.useCaseHandler = useCaseHandler;
        this.cookieProvider = cookieProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<CreateUserResponse> register(@Valid @RequestBody CreateUserRequest request,
                                                       HttpServletResponse response) {
        CreateUser usecase = new CreateUser();
        usecase.setName(request.getName());
        usecase.setUsername(request.getUsername());
        usecase.setPassword(request.getPassword());
        usecase.setEmail(request.getEmail());
        usecase.setBirthdate(request.getBirthdate());

        CreateUser answer = useCaseHandler.handle(usecase);

        cookieProvider.httpOnlyRefreshAndAccessTokenProvider(
                response,
                answer.getAccessToken(),
                answer.getRefreshToken());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateUserResponse(
                        answer.getAccessTokenExpiresAt().toString(),
                        answer.getRefreshTokenExpiresAt().toString()));
    }

}
