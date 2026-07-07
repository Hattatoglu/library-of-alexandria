package dev.eyaz.lib.of.alex.service.auth.infra.rest.dto.response;

import java.util.Set;

public record UpdateRoleResponse (
        String username,
        Set<String> roles
){
}
