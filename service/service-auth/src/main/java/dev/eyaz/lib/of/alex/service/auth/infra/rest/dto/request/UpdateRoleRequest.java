package dev.eyaz.lib.of.alex.service.auth.infra.rest.dto.request;

public record UpdateRoleRequest (
        String userId,
        String username,
        String role
){
}
