package dev.eyaz.lib.of.alex.service.auth.infra.rest.api;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCaseHandler;
import dev.eyaz.lib.of.alex.service.auth.core.enums.Role;
import dev.eyaz.lib.of.alex.service.auth.core.exception.InvalidCredentialsException;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.loginuser.handler.LoginUser;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.adapter.details.SecurityUserAdapter;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.adapter.details.UserRoleGrantedAuthority;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.UserAuthEntity;
import dev.eyaz.lib.of.alex.service.auth.infra.rest.cookie.CookieProvider;
import dev.eyaz.lib.of.alex.service.auth.infra.rest.dto.request.LoginUserRequest;
import dev.eyaz.lib.of.alex.service.auth.infra.rest.dto.response.LoginUserResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);
    private final AuthenticationManager authenticationManager;
    private final CookieProvider cookieProvider;

    private final UseCaseHandler<LoginUser> useCaseHandler;

    public LoginController(AuthenticationManager authenticationManager,
                           CookieProvider cookieProvider,
                           UseCaseHandler<LoginUser> useCaseHandler) {
        this.authenticationManager = authenticationManager;
        this.cookieProvider = cookieProvider;
        this.useCaseHandler = useCaseHandler;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginUserResponse> loginUser(@Valid @RequestBody LoginUserRequest request,
                                                       HttpServletResponse response) {


        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()));
        } catch (AuthenticationException ex) {
            log.warn("action=login_authentication_failed username={}", request.getUsername());
            throw new InvalidCredentialsException("Invalid username or password");
        }

        LoginUser usecase = new LoginUser();
        usecase.setUsername(request.getUsername());
        usecase.setUserId(((SecurityUserAdapter) authentication.getPrincipal()).getUserId());

        Set<Role> roles = ((SecurityUserAdapter) authentication.getPrincipal())
                .getAuthorities()
                .stream()
                .map(authority -> (UserRoleGrantedAuthority) authority)
                .map(UserRoleGrantedAuthority::getRole)
                .collect(Collectors.toSet());

        usecase.setRole(roles);

        LoginUser answer = useCaseHandler.handle(usecase);

        cookieProvider.httpOnlyRefreshAndAccessTokenProvider(
                response,
                answer.getAccessToken(),
                answer.getRefreshToken());

        return ResponseEntity.status(HttpStatus.OK)
                .body(new LoginUserResponse(
                        answer.getAccessTokenExpiresAt().toString(),
                        answer.getRefreshTokenExpiresAt().toString()
                ));
    }
}
