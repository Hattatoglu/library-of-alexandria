package dev.eyaz.lib.of.alex.service.auth.domain.usecase.createuser.handler;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCase;
import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.Role;
import jakarta.persistence.*;

import java.util.Set;
import java.util.UUID;

public class CreateUser implements UseCase {

    private String name;
    private String username;
    private String password;
    private String email;
    private String birthdate;

    private UUID userId;
    private String role;

    private String accessToken;
    private String refreshToken;

    public CreateUser() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(String birthdate) {
        this.birthdate = birthdate;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
