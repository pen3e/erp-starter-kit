package com.company.erp.users;

import com.company.erp.users.entity.User;
import com.company.erp.users.entity.UserStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Type-safe, composable filters for the user listing endpoint. Tenant isolation is added
 * automatically by Hibernate's {@code @TenantId} discriminator, so these specifications
 * never reference {@code tenantId}.
 */
public final class UserSpecifications {

    private UserSpecifications() {
    }

    /**
     * @param search optional case-insensitive substring matched against email / first / last name
     * @param status optional exact status filter
     */
    public static Specification<User> withFilter(String search, UserStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(root.get("firstName")), like),
                        cb.like(cb.lower(root.get("lastName")), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
