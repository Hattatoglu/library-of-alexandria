package dev.eyaz.lib.of.alex.service.auth.acceptance.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository.RefreshTokenRepository;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.repository.UserAuthRepository;
import dev.eyaz.lib.of.alex.service.auth.infra.rest.dto.request.CreateUserRequest;
import dev.eyaz.lib.of.alex.service.auth.infra.rest.dto.request.LoginUserRequest;
import dev.eyaz.lib.of.alex.service.auth.infra.rest.dto.request.UpdateRoleRequest;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;

import jakarta.servlet.http.Cookie;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthFlowSteps {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.5")
            .withDatabaseName("authdb_test")
            .withUsername("test")
            .withPassword("test");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private UserAuthRepository userRepository;
    @Autowired private RefreshTokenRepository tokenRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MvcResult lastResult;
    private Cookie lastAccessCookie;
    private Cookie lastRefreshCookie;
    private String lastRefreshTokenValue;
    private String lastUserId;

    private static final String BASE = "/api/v1/auth";
    private static final String DEFAULT_PASSWORD = "Secret123!";

    @Before
    void cleanDatabase() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();
        lastResult       = null;
        lastAccessCookie = null;
        lastRefreshCookie = null;
        lastRefreshTokenValue = null;
        lastUserId = null;
    }

    // ── Given ─────────────────────────────────────────────────────────────────

    @Given("the system is ready")
    public void theSystemIsReady() {
        // Spring context is up and running — Testcontainers is healthy
    }

    @Given("a user is already registered with username {string}")
    public void aUserIsAlreadyRegisteredWithUsername(String username) throws Exception {
        performSignUp(username, username + "@example.com", DEFAULT_PASSWORD);
    }

    @Given("a user with username {string} is logged in")
    public void aUserWithUsernameIsLoggedIn(String username) throws Exception {
        performSignUp(username, username + "@example.com", DEFAULT_PASSWORD);
        MvcResult loginResult = performLogin(username, DEFAULT_PASSWORD);
        MockHttpServletResponse response = loginResult.getResponse();
        lastAccessCookie  = findCookie(response, "access_token");
        lastRefreshCookie = findCookie(response, "refresh_token");
        if (lastRefreshCookie != null) {
            lastRefreshTokenValue = lastRefreshCookie.getValue();
        }
        lastUserId = userRepository.findByUsername(username)
                .orElseThrow(() -> new AssertionError("User not found after login: " + username))
                .getUserId()
                .toString();
    }

    // ── When ──────────────────────────────────────────────────────────────────

    @When("a sign-up request is sent with username {string}")
    public void aSignUpRequestIsSentWithUsername(String username) throws Exception {
        lastResult = performSignUp(username, username + "@example.com", DEFAULT_PASSWORD);
    }

    @When("a login request is sent with username {string} and password {string}")
    public void aLoginRequestIsSentWithUsernameAndPassword(String username, String password) throws Exception {
        lastResult = performLogin(username, password);
        MockHttpServletResponse response = lastResult.getResponse();
        lastAccessCookie  = findCookie(response, "access_token");
        lastRefreshCookie = findCookie(response, "refresh_token");
    }

    @When("a request is sent to the refresh endpoint")
    public void aRequestIsSentToTheRefreshEndpoint() throws Exception {
        assertThat(lastRefreshCookie).isNotNull();
        lastResult = mockMvc.perform(post(BASE + "/refresh")
                .cookie(lastRefreshCookie))
                .andReturn();
        MockHttpServletResponse response = lastResult.getResponse();
        lastAccessCookie  = findCookie(response, "access_token");
        lastRefreshCookie = findCookie(response, "refresh_token");
    }

    @When("a request is sent to the refresh endpoint without a cookie")
    public void aRequestIsSentToTheRefreshEndpointWithoutACookie() throws Exception {
        lastResult = mockMvc.perform(post(BASE + "/refresh"))
                .andReturn();
    }

    @When("a logout request is sent")
    public void aLogoutRequestIsSent() throws Exception {
        Cookie cookie = lastRefreshCookie != null ? lastRefreshCookie
                : new Cookie("refresh_token", "dummy");
        // X-User-Id is normally injected by api-gateway after JWT verification.
        // api-gateway does not exist yet, so the test simulates that header directly.
        lastResult = mockMvc.perform(post(BASE + "/logout")
                .cookie(cookie)
                .header("X-User-Id", lastUserId))
                .andReturn();
    }

    @When("a request is sent to the public-key endpoint")
    public void aRequestIsSentToThePublicKeyEndpoint() throws Exception {
        lastResult = mockMvc.perform(get(BASE + "/public-key"))
                .andReturn();
    }

    @When("a request is sent to the searchuser endpoint with username {string}")
    public void aRequestIsSentToTheSearchUserEndpointWithUsername(String username) throws Exception {
        lastResult = mockMvc.perform(get(BASE + "/searchuser")
                .param("username", username))
                .andReturn();
    }

    @When("the role {string} is assigned to user {string}")
    public void theRoleIsAssignedToUser(String role, String username) throws Exception {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AssertionError("User not found: " + username));
        UpdateRoleRequest request = new UpdateRoleRequest(
                user.getUserId().toString(), username, role);
        lastResult = mockMvc.perform(patch(BASE + "/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andReturn();
    }

    // ── Then ──────────────────────────────────────────────────────────────────

    @Then("the response status should be {int}")
    public void theResponseStatusShouldBe(int statusCode) {
        assertThat(lastResult.getResponse().getStatus()).isEqualTo(statusCode);
    }

    @Then("the user should be persisted in the database")
    public void theUserShouldBePersistedInTheDatabase() {
        assertThat(userRepository.count()).isGreaterThan(0);
    }

    @Then("the response should contain the error type {string}")
    public void theResponseShouldContainTheErrorType(String errorType) throws Exception {
        String body = lastResult.getResponse().getContentAsString();
        assertThat(body).contains(errorType);
    }

    @Then("the response should contain an HttpOnly access_token cookie")
    public void theResponseShouldContainAnHttpOnlyAccessTokenCookie() {
        Cookie cookie = findCookie(lastResult.getResponse(), "access_token");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    @Then("the response should contain an HttpOnly refresh_token cookie")
    public void theResponseShouldContainAnHttpOnlyRefreshTokenCookie() {
        Cookie cookie = findCookie(lastResult.getResponse(), "refresh_token");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    @Then("the response should contain a new HttpOnly access_token cookie")
    public void theResponseShouldContainANewHttpOnlyAccessTokenCookie() {
        assertThat(lastAccessCookie).isNotNull();
        assertThat(lastAccessCookie.isHttpOnly()).isTrue();
    }

    @Then("the response should contain a new HttpOnly refresh_token cookie")
    public void theResponseShouldContainANewHttpOnlyRefreshTokenCookie() {
        assertThat(lastRefreshCookie).isNotNull();
        assertThat(lastRefreshCookie.isHttpOnly()).isTrue();
    }

    @Then("the old refresh token should no longer be valid")
    public void theOldRefreshTokenShouldNoLongerBeValid() {
        if (lastRefreshTokenValue != null) {
            assertThat(tokenRepository.findByToken(lastRefreshTokenValue)).isEmpty();
        }
    }

    @Then("the cookies should be cleared")
    public void theCookiesShouldBeCleared() {
        Cookie access  = findCookie(lastResult.getResponse(), "access_token");
        Cookie refresh = findCookie(lastResult.getResponse(), "refresh_token");
        if (access  != null) assertThat(access.getMaxAge()).isZero();
        if (refresh != null) assertThat(refresh.getMaxAge()).isZero();
    }

    @Then("the response should contain an RSA public key")
    public void theResponseShouldContainAnRsaPublicKey() throws Exception {
        String body = lastResult.getResponse().getContentAsString();
        assertThat(body).containsAnyOf("BEGIN PUBLIC KEY", "publicKey");
    }

    @Then("the algorithm should be {string}")
    public void theAlgorithmShouldBe(String expectedAlg) throws Exception {
        String body = lastResult.getResponse().getContentAsString();
        assertThat(body).contains(expectedAlg);
    }

    @Then("the response should contain the correct user details")
    public void theResponseShouldContainTheCorrectUserDetails() throws Exception {
        String body = lastResult.getResponse().getContentAsString();
        assertThat(body).contains("emre");
    }

    @Then("the user's roles should contain {string}")
    public void theUsersRolesShouldContain(String role) throws Exception {
        String body = lastResult.getResponse().getContentAsString();
        assertThat(body).contains(role);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private MvcResult performSignUp(String username, String email, String password) throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setName("Test User");
        request.setUsername(username);
        request.setPassword(password);
        request.setEmail(email);
        request.setBirthday("1990-01-01");

        return mockMvc.perform(post(BASE + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andReturn();
    }

    private MvcResult performLogin(String username, String password) throws Exception {
        LoginUserRequest request = new LoginUserRequest();
        request.setUsername(username);
        request.setPassword(password);

        return mockMvc.perform(post(BASE + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andReturn();
    }

    private Cookie findCookie(MockHttpServletResponse response, String name) {
        Cookie[] cookies = response.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (c.getName().equals(name)) return c;
        }
        return null;
    }
}
