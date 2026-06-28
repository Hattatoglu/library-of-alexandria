package dev.eyaz.lib.of.alex.service.auth.infra.rest.api;

import dev.eyaz.lib.of.alex.service.auth.core.enums.UserRole;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.updateuser.handler.UpdateUser;
import dev.eyaz.lib.of.alex.service.auth.domain.usecase.updateuser.handler.UpdateUserHandler;
import dev.eyaz.lib.of.alex.service.auth.infra.rest.dto.request.UpdateRoleRequest;
import dev.eyaz.lib.of.alex.service.auth.infra.rest.dto.response.UpdateRoleResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
public class UpdateRoleController {

    private final UpdateUserHandler updateUserHandler;

    public UpdateRoleController(UpdateUserHandler updateUserHandler) {
        this.updateUserHandler = updateUserHandler;
    }

    @PatchMapping("/roles")
    public ResponseEntity<UpdateRoleResponse> updateRole(@RequestBody UpdateRoleRequest request) {
        UpdateUser usecase = new UpdateUser();
        usecase.setUsername(request.username());
        usecase.setUserId(UUID.fromString(request.userId()));
        usecase.setNewRole(UserRole.fromValue(request.role()));

        UpdateUser answer = updateUserHandler.handle(usecase);

        return ResponseEntity.status(HttpStatus.OK)
                .body(new UpdateRoleResponse(
                        answer.getUsername(),
                        answer.getRole().stream()
                                .map(UserRole::getValue)
                                .collect(Collectors.toSet())
                ));
    }
}
