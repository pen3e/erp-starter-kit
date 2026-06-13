package com.company.erp.auth;

import com.company.erp.config.SecurityProperties;
import com.company.erp.users.entity.User;
import com.company.erp.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Brute-force / credential-stuffing defence at the account level (complements the per-IP
 * rate limiter). After {@code maxFailedLogins} consecutive failures the account is locked
 * for {@code lockoutDuration}; a successful login resets the counters.
 */
@Service
public class LoginAttemptService {

    private final UserRepository userRepository;
    private final SecurityProperties.RateLimit cfg;

    public LoginAttemptService(UserRepository userRepository, SecurityProperties properties) {
        this.userRepository = userRepository;
        this.cfg = properties.getRateLimit();
    }

    @Transactional
    public void onSuccess(User user) {
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
    }

    @Transactional
    public void onFailure(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= cfg.getMaxFailedLogins()) {
            user.setLockedUntil(Instant.now().plus(cfg.getLockoutDuration()));
            user.setFailedLoginAttempts(0); // reset the window; lockout now governs
        }
        userRepository.save(user);
    }
}
