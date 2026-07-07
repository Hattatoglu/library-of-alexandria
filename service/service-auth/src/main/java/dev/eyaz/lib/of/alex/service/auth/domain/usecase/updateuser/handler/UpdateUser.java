package dev.eyaz.lib.of.alex.service.auth.domain.usecase.updateuser.handler;

import dev.eyaz.lib.of.alex.artifactory.lib.domain.usecase.UseCase;
import dev.eyaz.lib.of.alex.service.auth.core.enums.Role;

import java.util.Set;
import java.util.UUID;

public class UpdateUser implements UseCase {

    private UUID userId;
    private String username;
    private Set<Role> role;
    private Role newRole;

    public UpdateUser() {
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

    public Set<Role> getRole() {
        return role;
    }

    public void setRole(Set<Role> role) {
        this.role = role;
    }

    public Role getNewRole() {
        return newRole;
    }

    public void setNewRole(Role newRole) {
        this.newRole = newRole;
    }
}
