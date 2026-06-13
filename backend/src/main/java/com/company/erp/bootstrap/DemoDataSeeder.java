package com.company.erp.bootstrap;

import com.company.erp.config.BootstrapProperties;
import com.company.erp.common.util.InputSanitizer;
import com.company.erp.permissions.repository.PermissionRepository;
import com.company.erp.roles.entity.Role;
import com.company.erp.roles.repository.RoleRepository;
import com.company.erp.tenant.entity.Tenant;
import com.company.erp.tenant.entity.TenantStatus;
import com.company.erp.tenant.repository.TenantRepository;
import com.company.erp.users.entity.User;
import com.company.erp.users.entity.UserStatus;
import com.company.erp.users.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;

/**
 * Idempotent transactional seeding steps for the optional demo data.
 *
 * <p>Kept as a separate bean so the {@code @Transactional} boundaries are reached through a
 * Spring proxy, and so {@link #ensureTenantData} runs after the caller has bound the demo
 * tenant to {@code TenantContext} — Hibernate resolves the {@code @TenantId} discriminator
 * when the transaction's session opens (at method entry), so the context must already be set.
 */
@Service
public class DemoDataSeeder {

    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(TenantRepository tenantRepository, RoleRepository roleRepository,
                          UserRepository userRepository, PermissionRepository permissionRepository,
                          PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Creates the demo tenant (global entity) if it does not already exist. */
    @Transactional
    public void ensureTenant(BootstrapProperties props) {
        if (tenantRepository.existsBySlug(props.getTenantSlug())) {
            return;
        }
        Tenant tenant = new Tenant();
        tenant.setSlug(props.getTenantSlug());
        tenant.setName(props.getTenantName());
        tenant.setStatus(TenantStatus.ACTIVE);
        tenantRepository.save(tenant);
    }

    /**
     * Creates the ADMIN role (all permissions) and the admin user within the demo tenant.
     * {@code TenantContext} must be bound to the demo tenant before this is invoked.
     *
     * @return {@code true} if the admin user was created, {@code false} if it already existed
     */
    @Transactional
    public boolean ensureTenantData(BootstrapProperties props) {
        String email = InputSanitizer.normalizeEmail(props.getAdminEmail());
        if (userRepository.existsByEmail(email)) {
            return false;
        }

        Role admin = roleRepository.findByName(props.getAdminRoleName()).orElseGet(() -> {
            Role role = new Role();
            role.setName(props.getAdminRoleName());
            role.setDescription("Full administrative access (seeded)");
            role.setSystemRole(true);
            role.setPermissions(new HashSet<>(permissionRepository.findAll()));
            return roleRepository.save(role);
        });

        User user = new User();
        user.setFirstName(props.getAdminFirstName());
        user.setLastName(props.getAdminLastName());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(props.getAdminPassword()));
        user.setPasswordChangedAt(Instant.now());
        user.setStatus(UserStatus.ACTIVE);
        user.addRole(admin);
        userRepository.save(user);
        return true;
    }
}
