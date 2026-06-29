package dev.eyaz.lib.of.alex.service.auth.infra.postgres.adapter.details;

import dev.eyaz.lib.of.alex.service.auth.infra.postgres.model.UserAuthEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

public class SecurityUserAdapter implements UserDetails {

    private final UserAuthEntity entity;

    public SecurityUserAdapter(UserAuthEntity entity) {
        this.entity = entity;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return entity.getRoles()
                .stream()
                .map(UserRoleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword()  { return entity.getPassword(); }

    @Override
    public String getUsername()  { return entity.getUsername(); }

    @Override
    public boolean isAccountNonExpired()     { return entity.isAccountNonExpired(); }

    @Override
    public boolean isAccountNonLocked()      { return entity.isAccountNonLocked(); }

    @Override
    public boolean isCredentialsNonExpired() { return entity.isCredentialsNonExpired(); }

    @Override
    public boolean isEnabled()               { return entity.isEnabled(); }
}
