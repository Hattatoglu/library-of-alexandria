package dev.eyaz.lib.of.alex.server.gateway.acceptance.steps;

import dev.eyaz.lib.of.alex.server.gateway.acceptance.CucumberSpringContextConfig;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class GatewaySteps {

    @LocalServerPort
    private int port;

    private WebTestClient client;
    private String currentToken;
    private EntityExchangeResult<byte[]> lastResult;

    @Given("the gateway is ready")
    public void theGatewayIsReady() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Given("a valid access token for user {string} with roles {string}")
    public void aValidAccessTokenForUser(String userId, String roles) {
        currentToken = buildToken(userId, List.of(roles.split(",")), Instant.now().plusSeconds(900));
    }

    @Given("an expired access token for user {string}")
    public void anExpiredAccessTokenForUser(String userId) {
        currentToken = buildToken(userId, List.of("USER"), Instant.now().minusSeconds(60));
    }

    @Given("the rate limit bucket capacity is {int}")
    public void theRateLimitBucketCapacityIs(int capacity) {
        // Documents the expectation the scenario relies on — the actual
        // capacity is fixed in application-test.yaml (see gateway.ratelimit.bucket-capacity).
        // This step exists for readability; it does not reconfigure anything at runtime.
    }

    @When("a POST request is sent to {string}")
    public void aPostRequestIsSentTo(String path) {
        lastResult = client.post().uri(path).exchange()
                .expectBody().returnResult();
    }

    @When("a GET request is sent to {string} without a token")
    public void aGetRequestIsSentToWithoutAToken(String path) {
        lastResult = client.get().uri(path).exchange()
                .expectBody().returnResult();
    }

    @When("a GET request is sent to {string} with that token")
    public void aGetRequestIsSentToWithThatToken(String path) {
        lastResult = client.get().uri(path)
                .cookie("access_token", currentToken)
                .exchange()
                .expectBody().returnResult();
    }

    @When("{int} requests are sent in sequence to {string}")
    public void requestsAreSentInSequenceTo(int count, String path) {
        for (int i = 0; i < count; i++) {
            lastResult = client.post().uri(path).exchange()
                    .expectBody().returnResult();
        }
    }

    @Then("the response status should be {int}")
    public void theResponseStatusShouldBe(int expectedStatus) {
        assertThat(lastResult.getStatus().value()).isEqualTo(expectedStatus);
    }

    @Then("the last response status should be {int}")
    public void theLastResponseStatusShouldBe(int expectedStatus) {
        assertThat(lastResult.getStatus().value()).isEqualTo(expectedStatus);
    }

    @Then("the response body should contain {string}")
    public void theResponseBodyShouldContain(String expectedFragment) {
        String body = new String(lastResult.getResponseBodyContent());
        assertThat(body).contains(expectedFragment);
    }

    @Then("the response should contain the header {string} with value {string}")
    public void theResponseShouldContainTheHeaderWithValue(String headerName, String expectedValue) {
        assertThat(lastResult.getResponseHeaders().getFirst(headerName)).isEqualTo(expectedValue);
    }

    @Then("the last response should contain the header {string}")
    public void theLastResponseShouldContainTheHeader(String headerName) {
        assertThat(lastResult.getResponseHeaders().containsKey(headerName)).isTrue();
    }

    @Then("the response should contain the error code {string}")
    public void theResponseShouldContainTheErrorCode(String expectedErrorCode) {
        String body = new String(lastResult.getResponseBodyContent());
        assertThat(body).contains(expectedErrorCode);
    }

    private String buildToken(String userId, List<String> roles, Instant expiration) {
        return Jwts.builder()
                .subject(userId)
                .claim("username", userId)
                .claim("roles", roles)
                .claim("type", "access")
                .expiration(Date.from(expiration))
                .signWith(CucumberSpringContextConfig.signingKeyPair().getPrivate())
                .compact();
    }
}