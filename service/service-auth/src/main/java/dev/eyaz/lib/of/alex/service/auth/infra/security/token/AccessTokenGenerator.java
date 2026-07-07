package dev.eyaz.lib.of.alex.service.auth.infra.security.token;

import dev.eyaz.lib.of.alex.service.auth.infra.security.config.JwtProperties;
import dev.eyaz.lib.of.alex.service.auth.infra.security.key.RsaKeyLoader;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class AccessTokenGenerator implements AccessTokenService {

    private final JwtProperties jwtProperties;

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;
    private String publicKeyPem;

    public AccessTokenGenerator(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.privateKey  = RsaKeyLoader.loadPrivateKey(jwtProperties.privateKeyPath());
        this.publicKey   = RsaKeyLoader.loadPublicKey(jwtProperties.publicKeyPath());
        this.publicKeyPem = RsaKeyLoader.readPem(jwtProperties.publicKeyPath());
    }

    @Override
    public String generateAccessToken(UUID userId, String username, List<String> roles, Date now, Date expiry) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("roles", roles)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(privateKey)
                .compact();
    }




    @Override
    public String getPublicKeyPem() {
        return publicKeyPem;
    }

}
