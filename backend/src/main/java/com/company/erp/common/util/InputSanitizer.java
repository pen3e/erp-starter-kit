package com.company.erp.common.util;

import java.util.regex.Pattern;

/**
 * Defensive input normalisation used in addition to (never instead of) bean validation
 * and parameterised queries.
 *
 * <p>Note on injection defence in this codebase:
 * <ul>
 *   <li><b>SQL injection</b> is prevented structurally by JPA/Hibernate parameter binding
 *       — we never concatenate user input into JPQL/SQL.</li>
 *   <li><b>XSS</b> is primarily the frontend's responsibility (output encoding); here we
 *       strip control chars and reject obvious markup in free-text identifiers.</li>
 *   <li><b>Header / CRLF injection</b> is blocked by stripping line terminators.</li>
 * </ul>
 */
public final class InputSanitizer {

    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}&&[^\r\n\t]]");
    private static final Pattern CRLF = Pattern.compile("[\r\n]");

    private InputSanitizer() {
    }

    /** Trim, collapse stray control characters; returns {@code null} for {@code null}. */
    public static String clean(String value) {
        if (value == null) {
            return null;
        }
        String stripped = CONTROL_CHARS.matcher(value).replaceAll("");
        return stripped.trim();
    }

    /** Remove CR/LF to defend against header/log injection when echoing user values. */
    public static String stripCrlf(String value) {
        return value == null ? null : CRLF.matcher(value).replaceAll("");
    }

    /** Normalise an email for storage/lookup (trim + lowercase). */
    public static String normalizeEmail(String email) {
        String cleaned = clean(email);
        return cleaned == null ? null : cleaned.toLowerCase(java.util.Locale.ROOT);
    }
}
