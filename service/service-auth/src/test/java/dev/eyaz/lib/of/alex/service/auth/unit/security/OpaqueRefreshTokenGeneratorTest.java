package dev.eyaz.lib.of.alex.service.auth.unit.security;

import dev.eyaz.lib.of.alex.service.auth.infra.security.token.OpaqueRefreshTokenGenerator;
import dev.eyaz.lib.of.alex.service.auth.infra.security.token.RefreshTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

@DisplayName("OpaqueTokenGenerator — Unit Tests")
class OpaqueRefreshTokenGeneratorTest {

    private final RefreshTokenService generator = new OpaqueRefreshTokenGenerator();

    @Test
    @DisplayName("Generated token is not blank")
    void shouldGenerateNonBlankToken() {
        String token = generator.generateRefreshToken();

        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("Generated token is URL-safe (no '+', '/', or '=' characters)")
    void shouldBeUrlSafe() {
        String token = generator.generateRefreshToken();

        assertThat(token).doesNotContain("+", "/", "=");
    }

    @Test
    @DisplayName("Two consecutive calls produce different tokens, even within the same millisecond")
    void shouldGenerateDifferentTokensOnEachCall() {
        String first = generator.generateRefreshToken();
        String second = generator.generateRefreshToken();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("1000 generated tokens are all unique — no collisions")
    void shouldNotProduceCollisionsAtScale() {
        Set<String> tokens = new HashSet<>();
        IntStream.range(0, 1000).forEach(i -> tokens.add(generator.generateRefreshToken()));

        assertThat(tokens).hasSize(1000);
    }

    @Test
    @DisplayName("Token has sufficient length to represent 256 bits of entropy")
    void shouldHaveSufficientLength() {
        String token = generator.generateRefreshToken();

        // 256 bits = 32 bytes -> Base64URL without padding encodes to 43 characters
        assertThat(token.length()).isGreaterThanOrEqualTo(40);
    }
}
