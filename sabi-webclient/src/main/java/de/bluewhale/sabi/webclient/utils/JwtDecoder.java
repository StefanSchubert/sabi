/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.utils;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Lightweight JWT payload decoder for the webclient.
 * Extracts claims from the JWT payload (middle section) using standard Base64URL decoding
 * and Jackson JSON parsing — no additional JWT library required.
 *
 * @author Stefan Schubert
 */
@Slf4j
public final class JwtDecoder {

    private JwtDecoder() {
    }

    /**
     * Extracts the {@code sub} (subject) claim from the JWT.
     * For Sabi tokens the subject is always the user's email address.
     *
     * @param jwt raw JWT string (with or without "Bearer " prefix).
     * @return the subject string, or {@code null} if the token is malformed.
     */
    public static String extractSubject(String jwt) {
        Map<String, Object> claims = decodePayload(jwt);
        if (claims == null) {
            return null;
        }
        Object sub = claims.get("sub");
        return sub != null ? sub.toString() : null;
    }

    /**
     * Returns {@code true} if the JWT payload contains the {@code roles} claim with value {@code "ADMIN"}.
     *
     * @param jwt raw JWT string (with or without "Bearer " prefix).
     * @return {@code true} if the ADMIN role is present in the token.
     */
    public static boolean hasAdminRole(String jwt) {
        Map<String, Object> claims = decodePayload(jwt);
        if (claims == null) {
            return false;
        }
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List<?> roles) {
            return roles.stream().anyMatch(r -> "ADMIN".equals(r));
        }
        return false;
    }

    /**
     * Decodes the Base64URL-encoded payload section of a JWT and parses it as a JSON map.
     *
     * @param jwt raw JWT string (with or without "Bearer " prefix).
     * @return the claims map, or {@code null} on any error.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> decodePayload(String jwt) {
        if (jwt == null) {
            return null;
        }
        String token = jwt.startsWith("Bearer ") ? jwt.substring(7) : jwt;
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            return null;
        }
        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(addPadding(parts[1]));
            String payloadJson = new String(payloadBytes, StandardCharsets.UTF_8);
            // Use standard Jackson (tools.jackson) ObjectMapper to parse the JSON
            tools.jackson.databind.json.JsonMapper mapper = new tools.jackson.databind.json.JsonMapper();
            return mapper.readValue(payloadJson, Map.class);
        } catch (Exception e) {
            log.warn("Failed to decode JWT payload: {}", e.getMessage());
            return null;
        }
    }

    /** Base64URL strings are unpadded — add '=' padding if required before decoding. */
    private static String addPadding(String base64url) {
        int padding = (4 - base64url.length() % 4) % 4;
        return base64url + "=".repeat(padding);
    }
}
