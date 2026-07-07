package dev.eyaz.lib.of.alex.service.auth.infra.rest.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest (
        @NotNull(message = "Target userId is required")
        String userId,
        @NotNull(message = "Username is required")
        String username,
        @NotNull(message = "Role is required")
        String role
){
}
