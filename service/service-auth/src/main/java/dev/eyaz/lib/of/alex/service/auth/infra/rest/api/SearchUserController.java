package dev.eyaz.lib.of.alex.service.auth.infra.rest.api;

import dev.eyaz.lib.of.alex.service.auth.domain.usecase.finduser.handler.FindUser;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.finduser.handler.FindUserHandler;
import dev.eyaz.lib.of.alex.service.auth.infra.rest.dto.response.UserDataResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class SearchUserController {

    private final FindUserHandler findUserHandler;

    public SearchUserController(FindUserHandler findUserHandler) {
        this.findUserHandler = findUserHandler;
    }

    @GetMapping("/searchuser")
    public ResponseEntity<UserDataResponse> findUser(@RequestParam String username) {
        FindUser usecase = new FindUser();
        usecase.setUsername(username);

        FindUser answer = findUserHandler.handle(usecase);

        return ResponseEntity.status(HttpStatus.OK).body(
                new UserDataResponse(
                        answer.getName(),
                        answer.getUsername(),
                        answer.getUserId().toString(),
                        answer.getRoles().stream()
                                .map(role -> String.valueOf(role.getValue()))
                                .toList())
        );

    }
}
