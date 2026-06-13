package com.company.erp.users;

import com.company.erp.audit.AuditAction;
import com.company.erp.audit.AuditService;
import com.company.erp.common.exception.BusinessRuleException;
import com.company.erp.common.exception.DuplicateResourceException;
import com.company.erp.common.exception.ResourceNotFoundException;
import com.company.erp.common.util.InputSanitizer;
import com.company.erp.roles.entity.Role;
import com.company.erp.roles.repository.RoleRepository;
import com.company.erp.security.SecurityUtils;
import com.company.erp.users.dto.CreateUserRequest;
import com.company.erp.users.dto.UpdateUserRequest;
import com.company.erp.users.dto.UserResponse;
import com.company.erp.users.entity.User;
import com.company.erp.users.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Tenant-scoped user administration. Every query/write is implicitly constrained to the
 * current tenant by Hibernate's {@code @TenantId} discriminator. Read mapping to DTOs is
 * done inside the transaction because {@code spring.jpa.open-in-view=false} closes the
 * persistence context before the controller serializes the response.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder, AuditService audit) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(String search, UserStatus status, Pageable pageable) {
        return userRepository.findAll(UserSpecifications.withFilter(search, status), pageable)
                .map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public User get(UUID id) {
        return userRepository.findWithRolesById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional
    public User create(CreateUserRequest request) {
        String email = InputSanitizer.normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("A user with this email already exists");
        }
        User user = new User();
        user.setFirstName(InputSanitizer.clean(request.firstName()));
        user.setLastName(InputSanitizer.clean(request.lastName()));
        user.setEmail(email);
        user.setPhone(InputSanitizer.clean(request.phone()));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPasswordChangedAt(Instant.now());
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(resolveRoles(request.roleIds()));
        User saved = userRepository.save(user);

        audit.entityChange(AuditAction.CREATE, SecurityUtils.currentUserId(), SecurityUtils.currentUserEmail(),
                "User", saved.getId().toString(), null, saved.getEmail());
        return saved;
    }

    @Transactional
    public User update(UUID id, UpdateUserRequest request) {
        User user = get(id);
        String before = describe(user);

        if (request.firstName() != null) {
            user.setFirstName(InputSanitizer.clean(request.firstName()));
        }
        if (request.lastName() != null) {
            user.setLastName(InputSanitizer.clean(request.lastName()));
        }
        if (request.phone() != null) {
            user.setPhone(InputSanitizer.clean(request.phone()));
        }
        if (request.status() != null) {
            user.setStatus(request.status());
        }
        if (request.roleIds() != null) {
            user.setRoles(resolveRoles(request.roleIds()));
        }
        User saved = userRepository.save(user);

        audit.entityChange(AuditAction.UPDATE, SecurityUtils.currentUserId(), SecurityUtils.currentUserEmail(),
                "User", saved.getId().toString(), before, describe(saved));
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        User user = get(id);
        if (id.equals(SecurityUtils.currentUserId())) {
            throw new BusinessRuleException("You cannot delete your own account");
        }
        userRepository.delete(user);
        audit.entityChange(AuditAction.DELETE, SecurityUtils.currentUserId(), SecurityUtils.currentUserEmail(),
                "User", id.toString(), user.getEmail(), null);
    }

    private String describe(User user) {
        return user.getEmail() + " status=" + user.getStatus() + " roles=" + user.getRoles().size();
    }

    private Set<Role> resolveRoles(Set<UUID> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Role> found = roleRepository.findAllById(roleIds);
        if (found.size() != roleIds.size()) {
            throw new BusinessRuleException("One or more role ids are invalid");
        }
        return new HashSet<>(found);
    }
}
