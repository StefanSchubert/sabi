/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

/**
 * Internal value object carrying verified claims extracted from a validated OIDC ID token.
 * No PII is stored in audit logs – only the {@code sub} claim (opaque identifier).
 *
 * @author Stefan Schubert
 */
public record OidcClaims(
        /** Immutable provider-side user identifier (Google: ~21-digit numeric string). */
        String sub,
        /** Verified email address from the ID token. */
        String email,
        /** Whether the provider has verified this email address. */
        boolean emailVerified,
        /** Display name from the ID token (may be null if not granted). */
        String name,
        /** BCP-47 locale tag, e.g. "de" or "en" (may be null). */
        String locale,
        /** Provider identifier, e.g. "GOOGLE". */
        String provider,
        /** Nonce from the original authorization request (may be null). */
        String nonce
) {
}

