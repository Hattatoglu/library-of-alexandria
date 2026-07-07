Feature: API Gateway request flow
  Tests the Gateway's filter chain — JWT validation, rate limiting, and
  routing/circuit-breaking — as observed from the outside, through real
  HTTP requests, without any internal mocking of the Gateway's own filters.

  Background:
    Given the gateway is ready

  Scenario: A public path is forwarded without requiring authentication
    When a POST request is sent to "/register"
    Then the response status should be 200
    And the response body should contain "registered"

  Scenario: A protected path without an access token is rejected
    When a GET request is sent to "/some-protected-path" without a token
    Then the response status should be 401
    And the response should contain the header "X-Auth-Error" with value "missing_token"

  Scenario: A protected path with a valid access token passes authentication
    Given a valid access token for user "user-123" with roles "USER"
    When a GET request is sent to "/some-protected-path" with that token
    Then the response status should be 404
    And the response should contain the error code "route_not_found"

  Scenario: A protected path with an expired access token is rejected
    Given an expired access token for user "user-123"
    When a GET request is sent to "/some-protected-path" with that token
    Then the response status should be 401
    And the response should contain the header "X-Auth-Error" with value "token_expired"

  Scenario: Requests exceeding the rate limit are rejected
    Given the rate limit bucket capacity is 5
    When 6 requests are sent in sequence to "/register"
    Then the last response status should be 429
    And the last response should contain the header "Retry-After"