package dev.eyaz.lib.of.alex.service.auth.infra.security.token;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface AccessTokenService {
    String generateAccessToken(UUID userId, String username, List<String> roles, Date now, Date expiry);
    String getPublicKeyPem();
}
