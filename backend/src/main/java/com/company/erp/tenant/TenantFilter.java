package com.company.erp.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Establishes the tenant for the request thread and guarantees it is cleared afterwards.
 *
 * <p>Runs near the start of the chain (before security) so that the tenant is set for the
 * default/edge case. {@code JwtAuthenticationFilter} may subsequently overwrite the value
 * with the verified token claim. The {@code finally} block prevents tenant leakage onto a
 * pooled request thread.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantFilter extends OncePerRequestFilter {

    private final TenantResolver tenantResolver;

    public TenantFilter(TenantResolver tenantResolver) {
        this.tenantResolver = tenantResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            TenantContext.set(tenantResolver.resolve(request));
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
