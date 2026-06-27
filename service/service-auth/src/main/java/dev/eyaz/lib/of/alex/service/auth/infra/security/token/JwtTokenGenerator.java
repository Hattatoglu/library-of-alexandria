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
import java.util.UUID;

@Component
public class JwtTokenGenerator implements JwtTokenService{

    private final JwtProperties jwtProperties;

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;
    private String publicKeyPem;

    public JwtTokenGenerator(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.privateKey  = RsaKeyLoader.loadPrivateKey(jwtProperties.privateKeyPath());
        this.publicKey   = RsaKeyLoader.loadPublicKey(jwtProperties.publicKeyPath());
        this.publicKeyPem = RsaKeyLoader.readPem(jwtProperties.publicKeyPath());
    }


    @Override
    public String generateAccessToken(UUID userId, String username, String role, Date now, Date expiry) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("roles", role)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(privateKey)
                .compact();
    }

    @Override
    public String generateRefreshToken(UUID userId) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.refreshTokenExpirationMs());

        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(privateKey)
                .compact();
    }

    @Override
    public UUID extractUserIdFromRefreshToken(String token) {
        Claims claims = parseClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    @Override
    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = parseClaims(token);
            return "refresh".equals(claims.get("type", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public LocalDateTime getRefreshTokenExpiry() {
        return LocalDateTime.now().plusSeconds(
                jwtProperties.refreshTokenExpirationMs() / 1000
        );
    }

    @Override
    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
