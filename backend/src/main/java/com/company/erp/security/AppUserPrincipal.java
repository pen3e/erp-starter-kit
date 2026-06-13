package com.company.erp.security;

import com.company.erp.users.entity.User;
import com.company.erp.users.entity.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spring Security principal backed by an ERP {@link User}. Authorities are the user's
 * fine-grained <em>permission</em> names (e.g. {@code USER_READ}), gathered across all
 * assigned roles — this is what {@code @PreAuthorize("hasAuthority(...)")} checks.
 */
@Getter
public class AppUserPrincipal implements UserDetails {

    private final UUID userId;
    private final String tenantId;
    private final String email;
    private final String passwordHash;
    private final UserStatus status;
    private final boolean locked;
    private final List<GrantedAuthority> authorities;

    public AppUserPrincipal(User user) {
        this.userId = user.getId();
        this.tenantId = user.getTenantId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.status = user.getStatus();
        this.locked = user.isLocked();
        this.authorities = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(p -> (GrantedAuthority) new SimpleGrantedAuthority(p.getName()))
                .distinct()
                .toList();
    }

    /** Lightweight principal rebuilt from JWT claims (no DB access on the hot path). */
    public AppUserPrincipal(UUID userId, String email, String tenantId, List<String> authorities) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.email = email;
        this.passwordHash = null;
        this.status = UserStatus.ACTIVE;
        this.locked = false;
        this.authorities = authorities.stream()
                .map(a -> (GrantedAuthority) new SimpleGrantedAuthority(a))
                .toList();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
