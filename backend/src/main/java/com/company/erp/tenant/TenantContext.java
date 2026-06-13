package com.company.erp.tenant;

/**
 * Holds the tenant identifier for the duration of a single request, on the request thread.
 *
 * <p>Populated by {@link TenantFilter} early in the filter chain and read by Hibernate's
 * {@link TenantIdentifierResolver}. Always cleared in a {@code finally} block to avoid
 * leaking a tenant id into a pooled thread serving the next request.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(String tenantId) {
        CURRENT.set(tenantId);
    }

    /** @return the current tenant id, or {@code null} if none has been resolved yet. */
    public static String get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
