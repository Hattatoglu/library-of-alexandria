package dev.eyaz.lib.of.alex.server.gateway.key;

import dev.eyaz.lib.of.alex.server.gateway.actuator.GatewayMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;

@Component
public class PublicKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(PublicKeyProvider.class);

    private final WebClient serviceAuthWebClient;
    private final GatewayMetrics gatewayMetrics;
    private final Mono<RSAPublicKey> cachedPublicKey;

    public PublicKeyProvider(WebClient serviceAuthWebClient,
                             GatewayAuthProperties properties,
                             GatewayMetrics gatewayMetrics) {
        this.serviceAuthWebClient = serviceAuthWebClient;
        this.gatewayMetrics = gatewayMetrics;
        Duration ttl = properties.publicKeyCacheTtl();
        this.cachedPublicKey = fetchPublicKey()
                .doOnError(e -> {
                    gatewayMetrics.recordPublicKeyFetchFailure();
                    log.error("Failed to fetch/parse public key from ServiceAuth", e);
                })
                .cache(
                        value -> ttl,
                        error -> Duration.ZERO,
                        () -> Duration.ZERO
                );
    }

    public Mono<RSAPublicKey> getPublicKey() {
        return cachedPublicKey;
    }

    private Mono<RSAPublicKey> fetchPublicKey() {
        return serviceAuthWebClient.get()
                .uri("/api/v1/auth/public-key")
                .retrieve()
                .bodyToMono(PublicKeyResponse.class)
                .map(response -> parsePemPublicKey(response.publicKey()));
    }

    private RSAPublicKey parsePemPublicKey(String pem) {
        try {
            String cleaned = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(cleaned);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse RSA public key from ServiceAuth", e);
        }
    }
}

