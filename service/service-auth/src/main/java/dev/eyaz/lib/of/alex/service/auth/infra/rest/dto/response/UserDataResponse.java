package dev.eyaz.lib.of.alex.service.auth.infra.rest.dto.response;

import java.util.List;

public record UserDataResponse (
        String name,
        String username,
        String userId,
        List<String> roles
){
}
