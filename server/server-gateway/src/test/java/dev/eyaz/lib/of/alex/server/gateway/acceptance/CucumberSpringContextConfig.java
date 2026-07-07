package dev.eyaz.lib.of.alex.server.gateway.acceptance;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import dev.eyaz.lib.of.alex.server.gateway.filter.routing.RouteConfig;
import dev.eyaz.lib.of.alex.server.gateway.filter.routing.RoutingProperties;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Shared Spring context for every acceptance scenario in this suite.
 * Cucumber-Spring caches this context across scenarios (same as
 * @SpringBootTest context caching for regular JUnit tests) — Redis and
 * WireMock are started ONCE for the whole feature file, not per scenario.
 *
 * Container/WireMock startup happens in a static initializer rather than
 * @BeforeAll, because @CucumberContextConfiguration classes are plain POJOs
 * picked up by cucumber-spring's own lifecycle, not JUnit 5's — the
 * standard Testcontainers "singleton container" pattern is used here
 * instead of the @Testcontainers JUnit extension.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(CucumberSpringContextConfig.TestRoutingConfig.class)
public class CucumberSpringContextConfig {

    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    static final WireMockServer WIRE_MOCK = new WireMockServer(0);
    static final KeyPair SERVICE_AUTH_KEY_PAIR;

    static {
        REDIS.start();
        WIRE_MOCK.start();
        WireMock.configureFor("localhost", WIRE_MOCK.port());

        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            SERVICE_AUTH_KEY_PAIR = keyGen.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate test RSA key pair", e);
        }

        String pem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(((RSAPublicKey) SERVICE_AUTH_KEY_PAIR.getPublic()).getEncoded())
                + "\n-----END PUBLIC KEY-----";

        stubFor(get(urlEqualTo("/api/v1/auth/public-key"))
                .willReturn(okJson("{\"publicKey\":\"" + pem.replace("\n", "\\n") + "\",\"algorithm\":\"RS256\"}")));

        stubFor(post(urlEqualTo("/api/v1/auth/register"))
                .willReturn(okJson("{\"status\":\"registered\"}")));
    }

    public static KeyPair signingKeyPair() {
        return SERVICE_AUTH_KEY_PAIR;
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("gateway.auth.service-auth-base-url", () -> "http://localhost:" + WIRE_MOCK.port());
    }

    /**
     * Same rationale as GatewayIntegrationTest's TestRoutingConfig:
     * constructed directly in Java to sidestep the Binder limitation where
     * a List<record> element's fields, split across a static property
     * source and @DynamicPropertySource, do not merge correctly.
     */
    @TestConfiguration
    static class TestRoutingConfig {

        @Bean
        @Primary
        RoutingProperties testRoutingProperties() {
            String wireMockUrl = "http://localhost:" + WIRE_MOCK.port();
            return new RoutingProperties(List.of(
                    new RouteConfig("/register", "service-auth", wireMockUrl + "/api/v1/auth")
            ));
        }
    }
}