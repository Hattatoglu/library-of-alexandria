Feature: Authentication Flows
  Tests the authentication flows of the Library of Alexandria service-auth service.
  Users can sign up, log in, refresh their tokens, and log out.

  Background:
    Given the system is ready

  Scenario: A new user signs up successfully
    When a sign-up request is sent with username "testuser"
    Then the response status should be 201
    And the user should be persisted in the database

  Scenario: Signing up with an already registered username
    Given a user is already registered with username "testuser"
    When a sign-up request is sent with username "testuser"
    Then the response status should be 409
    And the response should contain the error type "user-already-exists"

  Scenario: A registered user logs in successfully
    Given a user is already registered with username "testuser"
    When a login request is sent with username "testuser" and password "Secret123!"
    Then the response status should be 200
    And the response should contain an HttpOnly access_token cookie
    And the response should contain an HttpOnly refresh_token cookie

  Scenario: Logging in with an incorrect password
    Given a user is already registered with username "testuser"
    When a login request is sent with username "testuser" and password "WrongPass!"
    Then the response status should be 401
    And the response should contain the error type "invalid-credentials"

  Scenario: Logging in with a non-existent username
    When a login request is sent with username "ghost" and password "anypass"
    Then the response status should be 401

  Scenario: Access token is refreshed with a valid refresh token
    Given a user with username "testuser" is logged in
    When a request is sent to the refresh endpoint
    Then the response status should be 200
    And the response should contain a new HttpOnly access_token cookie
    And the response should contain a new HttpOnly refresh_token cookie
    And the old refresh token should no longer be valid

  Scenario: Sending a refresh request without a refresh token
    When a request is sent to the refresh endpoint without a cookie
    Then the response status should be 400

  Scenario: A logged-in user logs out successfully
    Given a user with username "testuser" is logged in
    When a logout request is sent
    Then the response status should be 204
    And the cookies should be cleared

  Scenario: The public key is retrieved successfully
    When a request is sent to the public-key endpoint
    Then the response status should be 200
    And the response should contain an RSA public key
    And the algorithm should be "RSA256"

  Scenario: A registered user is found by username
    Given a user is already registered with username "testuser"
    When a request is sent to the searchuser endpoint with username "testuser"
    Then the response status should be 200
    And the response should contain the correct user details

  Scenario: A non-existent user is searched
    When a request is sent to the searchuser endpoint with username "ghost"
    Then the response status should be 404
    And the response should contain the error type "user-not-found"

  Scenario: A new role is assigned to a user
    Given a user is already registered with username "testuser"
    When the role "ADMIN" is assigned to user "testuser"
    Then the response status should be 200
    And the user's roles should contain "ADMIN"
