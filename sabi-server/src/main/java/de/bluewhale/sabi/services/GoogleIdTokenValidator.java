/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Validates a raw Google ID token (JWT) against Google's public JWK Set.
 * Extracted as a Spring component so it can be mocked in tests.
 *
 * @author Stefan Schubert
 */
@Component
public class GoogleIdTokenValidator {

    private static final String GOOGLE_JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final String GOOGLE_ISSUER   = "https://accounts.google.com";

    @Value("${google.oidc.client-id}")
    private String expectedAudience;

    /**
     * Decodes and validates the raw ID token.
     *
     * @param rawIdToken raw Google ID token string
     * @return the verified {@link Jwt}
     * @throws org.springframework.security.oauth2.jwt.JwtException on any validation failure
     */
    public Jwt validate(String rawIdToken) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(GOOGLE_JWKS_URI)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(GOOGLE_ISSUER));
        Jwt jwt = decoder.decode(rawIdToken);

        // Validate audience claim
        Object aud = jwt.getClaim("aud");
        boolean audMatch = false;
        if (aud instanceof String s) {
            audMatch = expectedAudience.equals(s);
        } else if (aud instanceof Iterable<?> list) {
            for (Object entry : list) {
                if (expectedAudience.equals(entry)) { audMatch = true; break; }
            }
        }
        if (!audMatch) {
            throw new org.springframework.security.oauth2.jwt.JwtException("aud claim mismatch – token not intended for this application");
        }
        return jwt;
    }
}

