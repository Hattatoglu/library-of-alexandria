package dev.eyaz.lib.of.alex.service.auth.infra.security.token;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates opaque (non-JWT) refresh tokens.
 *
 * See ADR-001 (addendum): access tokens remain JWTs (RS256) so api-gateway can verify
 * them statelessly. Refresh tokens, however, are always looked up against PostgreSQL on
 * every use (see ADR-003, ADR-004) — the system never benefits from a refresh token being
 * self-describing or independently verifiable. Making the refresh token a JWT therefore
 * added parsing/signing overhead and a class of bugs (e.g. two tokens generated within the
 * same second producing an identical JWT string) without providing any real advantage.
 *
 * The token is 256 bits of cryptographically secure randomness (java.security.SecureRandom,
 * not java.util.Random and not UUID.randomUUID(), which is not designed to give a uniform
 * security guarantee for this purpose), Base64URL-encoded without padding so it is safe to
 * use directly inside a cookie value.
 */
@Component
public class OpaqueRefreshTokenGenerator implements RefreshTokenService{

    private static final int TOKEN_BYTE_LENGTH = 32; // 256 bits

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateRefreshToken() {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}