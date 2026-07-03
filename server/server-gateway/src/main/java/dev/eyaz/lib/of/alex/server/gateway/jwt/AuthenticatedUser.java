package dev.eyaz.lib.of.alex.server.gateway.jwt;

import java.util.List;

/**
 * Resolved identity from a valid access token, mirroring the claims set by
 * AccessTokenGenerator in service-auth: subject = userId, "username" claim,
 * "roles" claim (plural — a list, not a single role).
 */
public record AuthenticatedUser(
        String userId,
        String username,
        List<String> roles
) {
    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
