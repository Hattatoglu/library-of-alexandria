package dev.eyaz.lib.of.alex.service.auth.domain.usecase.finduser.handler;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCase;
import dev.eyaz.lib.of.alex.service.auth.core.enums.Role;

import java.util.Set;
import java.util.UUID;

public class FindUser implements UseCase {

    private UUID userId;
    private String username;
    private Set<Role> roles;
    private String name;

    public FindUser() {
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
