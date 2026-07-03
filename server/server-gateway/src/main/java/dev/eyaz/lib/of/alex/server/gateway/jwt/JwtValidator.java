package dev.eyaz.lib.of.alex.server.gateway.jwt;

import dev.eyaz.lib.of.alex.server.gateway.exception.exceptions.GatewayException;
import dev.eyaz.lib.of.alex.server.gateway.exception.exceptions.JwtValidationException;
import dev.eyaz.lib.of.alex.server.gateway.exception.exceptions.PublicKeyUnavailableException;
import dev.eyaz.lib.of.alex.server.gateway.key.PublicKeyProvider;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.security.interfaces.RSAPublicKey;
import java.util.List;

@Component
public class JwtValidator {

    private final PublicKeyProvider publicKeyProvider;

    public JwtValidator(PublicKeyProvider publicKeyProvider) {
        this.publicKeyProvider = publicKeyProvider;
    }

    public Mono<AuthenticatedUser> validate(String token) {
        return publicKeyProvider.getPublicKey()
                .onErrorMap(ex -> !(ex instanceof GatewayException), PublicKeyUnavailableException::new)
                .flatMap(publicKey -> parse(token, publicKey));
    }

    private Mono<AuthenticatedUser> parse(String token, RSAPublicKey publicKey) {
        try {
            Jws<io.jsonwebtoken.Claims> jws = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token);

            io.jsonwebtoken.Claims claims = jws.getPayload();

            if (!"access".equals(claims.get("type", String.class))) {
                return Mono.error(new JwtValidationException("wrong_token_type"));
            }

            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);

            AuthenticatedUser user = new AuthenticatedUser(
                    claims.getSubject(),
                    claims.get("username", String.class),
                    roles != null ? roles : List.of()
            );

            return Mono.just(user);
        } catch (ExpiredJwtException e) {
            return Mono.error(new JwtValidationException("token_expired"));
        } catch (JwtException e) {
            return Mono.error(new JwtValidationException("invalid_token"));
        }
    }
}
