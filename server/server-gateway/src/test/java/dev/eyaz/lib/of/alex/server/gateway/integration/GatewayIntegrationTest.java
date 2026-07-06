package dev.eyaz.lib.of.alex.server.gateway.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.blockhound.BlockHound;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * End-to-end test of the full filter chain (JwtValidationWebFilter ->
 * RateLimitingWebFilter -> RoutingWebFilter) against a real embedded
 * server, real Redis (Testcontainers), and a WireMock stub standing in for
 * service-auth. Static config (route list, public-path list) lives in
 * application-test.yaml; only genuinely runtime-determined values (Redis
 * port, WireMock port) are injected via @DynamicPropertySource, overriding
 * the placeholder base-url values already declared in that file.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayIntegrationTest {

    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    static WireMockServer wireMockServer;
    static KeyPair serviceAuthKeyPair;

    @LocalServerPort
    private int port;

    private WebTestClient client;

    @BeforeAll
    static void installBlockHound() {
        BlockHound.install();
    }

    @BeforeAll
    static void beforeAll() throws Exception {
        REDIS.start();

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        serviceAuthKeyPair = keyGen.generateKeyPair();

        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        String pem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(((RSAPublicKey) serviceAuthKeyPair.getPublic()).getEncoded())
                + "\n-----END PUBLIC KEY-----";

        stubFor(get(urlEqualTo("/api/v1/auth/public-key"))
                .willReturn(okJson("{\"publicKey\":\"" + pem.replace("\n", "\\n") + "\",\"algorithm\":\"RS256\"}")));

        stubFor(post(urlEqualTo("/api/v1/auth/register"))
                .willReturn(okJson("{\"status\":\"registered\"}")));
    }

    @AfterAll
    static void afterAll() {
        wireMockServer.stop();
        REDIS.stop();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("gateway.auth.service-auth-base-url", () -> "http://localhost:" + wireMockServer.port());
    }

    @Test
    void publicPathIsForwardedWithoutAuthentication() {

        client().post().uri("/register")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("registered");
    }

    @Test
    void protectedPathWithoutCookieReturns401() {
        client().get().uri("/some-protected-path")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals("X-Auth-Error", "missing_token");
    }

    @Test
    void protectedPathWithValidTokenPassesAuthButHasNoRoute() {
        String token = io.jsonwebtoken.Jwts.builder()
                .subject("user-123")
                .claim("username", "test")
                .claim("roles", List.of("USER"))
                .claim("type", "access")
                .expiration(Date.from(Instant.now().plusSeconds(900)))
                .signWith(serviceAuthKeyPair.getPrivate())
                .compact();

        // No route configured for /some-protected-path in application-test.yaml,
        // so this specifically proves authentication passed (no 401) —
        // routing behavior itself is already covered by RoutingWebFilterTest.
        client().get().uri("/some-protected-path")
                .cookie("access_token", token)
                .exchange()
                .expectStatus().isNotFound(); // route_not_found, not 401
    }

    private WebTestClient client() {
        if (client == null) {
            client = WebTestClient.bindToServer()
                    .baseUrl("http://localhost:" + port)
                    .build();
        }
        return client;
    }
}