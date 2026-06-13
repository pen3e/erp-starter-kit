package com.company.erp.common.util;

import java.util.regex.Pattern;

/**
 * Masks secret-bearing fields before they can reach logs or the audit trail. Belt-and-braces:
 * audit snapshots are built to exclude secrets in the first place, but this guards against a
 * field being added later and forgotten.
 */
public final class SensitiveDataMasker {

    private static final String MASK = "***REDACTED***";

    // Matches "key":"value" or key=value for sensitive keys (case-insensitive).
    private static final Pattern JSON_SECRET = Pattern.compile(
            "(?i)(\"(?:password|passwordHash|currentPassword|newPassword|token|refreshToken|accessToken|secret|authorization)\"\\s*:\\s*)\"[^\"]*\"");

    private SensitiveDataMasker() {
    }

    public static String mask(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return JSON_SECRET.matcher(value).replaceAll("$1\"" + MASK + "\"");
    }
}
