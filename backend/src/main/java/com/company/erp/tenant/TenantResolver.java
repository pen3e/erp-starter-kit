package com.company.erp.tenant;

import com.company.erp.config.TenantProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Resolves the tenant for an incoming request before authentication.
 *
 * <p>Precedence for un-authenticated requests is the {@code X-Tenant-ID} header, falling
 * back to the configured default tenant (single-tenant deployments never send the header).
 * For authenticated requests the {@code JwtAuthenticationFilter} overrides this with the
 * authoritative tenant claim from the verified token, so a client cannot read another
 * tenant's data by spoofing the header.
 */
@Component
public class TenantResolver {

    /** Conservative tenant-id format; rejects anything that isn't a clean slug. */
    private static final Pattern VALID_TENANT = Pattern.compile("^[a-z0-9][a-z0-9_-]{0,62}$");

    private final TenantProperties properties;

    public TenantResolver(TenantProperties properties) {
        this.properties = properties;
    }

    public String resolve(HttpServletRequest request) {
        String header = request.getHeader(properties.getHeaderName());
        if (StringUtils.hasText(header)) {
            String candidate = header.trim().toLowerCase(Locale.ROOT);
            if (VALID_TENANT.matcher(candidate).matches()) {
                return candidate;
            }
            // Malformed header -> do not honour it; fall through to default.
        }
        return properties.getDefaultTenant();
    }
}
