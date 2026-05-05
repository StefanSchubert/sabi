/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import de.bluewhale.sabi.api.HttpHeader;
import de.bluewhale.sabi.configs.AppConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Date;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Encodes and decodes access tokens.
 *
 * @author schubert
 */
@Slf4j
public class TokenAuthenticationService {
    private static final String SABI_JWT_ISSUER = "SABI-server module";
    /** JWT claim name used to encode assigned roles (e.g. {@code ["ADMIN"]}). */
    public static final String CLAIM_ROLES = "roles";
    // ------------------------------ FIELDS ------------------------------


    // Will be lazy initialized with @Value("${accessToken.SECRET}") through constructor
    private static String SECRET;

    // Will be lazy initialized with @Value("${accessToken.TTL}") through constructor
    private static long ACCESS_TOKEN_MAX_VALIDITY_PERIOD_IN_SECS;

    // Will be lazy initialized through constructor;
    static Algorithm JWT_TOKEN_ALGORITHM;

// -------------------------- STATIC METHODS --------------------------

    /**
     * Will be used by login or register, to provide the token belonging to the user.
     *
     * @param pResponse
     * @param pUserID   For sabi it's users email address.
     */
    static void addAuthentication(@NotNull HttpServletResponse pResponse, String pUserID) {
        addAuthentication(pResponse, pUserID, false);
    }

    /**
     * Will be used by login or register, to provide the token belonging to the user.
     * Includes the ADMIN role in the JWT claim if {@code isAdmin} is {@code true}.
     *
     * @param pResponse
     * @param pUserID   For sabi it's users email address.
     * @param isAdmin   Whether to embed the ADMIN role in the token.
     */
    static void addAuthentication(@NotNull HttpServletResponse pResponse, String pUserID, boolean isAdmin) {
        String jsonWebtoken = createAuthorizationTokenFor(pUserID, isAdmin);
        pResponse.addHeader(HttpHeader.AUTH_TOKEN, HttpHeader.TOKEN_PREFIX + jsonWebtoken);
    }

    /**
     * Creates a JWT without any roles (backward-compatible overload).
     */
    public static String createAuthorizationTokenFor(String pUserID) {
        return createAuthorizationTokenFor(pUserID, false);
    }

    /**
     * Creates a JWT for the given user.  When {@code isAdmin} is {@code true} the claim
     * {@value CLAIM_ROLES} is set to {@code ["ADMIN"]} so that the frontend and backend
     * can derive the admin role from the token rather than from a separate config.
     *
     * @param pUserID For sabi it's users email address.
     * @param isAdmin Whether to embed the ADMIN role in the token.
     */
    public static String createAuthorizationTokenFor(String pUserID, boolean isAdmin) {

        Date expiresAt = new Date(System.currentTimeMillis() + ACCESS_TOKEN_MAX_VALIDITY_PERIOD_IN_SECS * 1000);

        var builder = JWT.create()
                .withSubject(pUserID)
                .withExpiresAt(expiresAt)
                .withIssuer(SABI_JWT_ISSUER);

        if (isAdmin) {
            builder = builder.withClaim(CLAIM_ROLES, List.of("ADMIN"));
        }

        return builder.sign(JWT_TOKEN_ALGORITHM);
    }

    /**
     * Returns {@code true} if the given email is contained in the admin-users property string
     * (comma-separated list of emails, case-insensitive).
     * Centralizes the admin determination logic used by {@link JWTLoginFilter} and other callers.
     *
     * @param email              the user's email address to check.
     * @param adminUsersProperty comma-separated list of admin email addresses.
     * @return {@code true} if the email matches an admin entry.
     */
    public static boolean isAdminEmail(String email, String adminUsersProperty) {
        if (adminUsersProperty == null || adminUsersProperty.isBlank() || email == null || email.isBlank()) {
            return false;
        }
        return java.util.Arrays.stream(adminUsersProperty.split(","))
                .anyMatch(a -> a.trim().equalsIgnoreCase(email));
    }

    /**
     * Returns {@code true} if the given JWT contains the ADMIN role in the {@value CLAIM_ROLES} claim.
     *
     * @param token raw JWT string (with or without "Bearer " prefix).
     * @return {@code true} if the token is valid and contains the ADMIN role.
     */
    public static boolean extractAdminFromToken(String token) {
        if (token == null) {
            return false;
        }
        if (token.startsWith(HttpHeader.TOKEN_PREFIX)) {
            token = token.substring(7);
        }
        try {
            DecodedJWT verified = JWT.require(JWT_TOKEN_ALGORITHM)
                    .withIssuer(SABI_JWT_ISSUER)
                    .build()
                    .verify(token);
            List<String> roles = verified.getClaim(CLAIM_ROLES).asList(String.class);
            return roles != null && roles.contains("ADMIN");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Take the request and checks if we find a user in there and if so
     * we return the authenticated Authentication Object for that user.
     *
     * @param pRequest
     * @return null if the request did not contain a valid JWT token.
     */
    static Authentication getAuthentication(HttpServletRequest pRequest) {
        String token = pRequest.getHeader(HttpHeader.AUTH_TOKEN);
        if (token != null) {
            // parse the token.
            String user = extractUserFromToken(token);
            return user != null ?
                    new UsernamePasswordAuthenticationToken(user, null, emptyList()) :
                    null;
        }
        return null;
    }

    /**
     * provides the user encoded with the token
     *
     * @param token
     * @return users identified by his email or null in case the token was not valid.
     */
    public static String extractUserFromToken(String token) {
        String userID = null;

        if (token != null && token.startsWith(HttpHeader.TOKEN_PREFIX)) {
            token = token.substring(7);
        }

        try {

            // Make sure we have a valid signed token here
            DecodedJWT decoded = JWT.decode(token);

            DecodedJWT verified = JWT.require(JWT_TOKEN_ALGORITHM)
                    .withIssuer("SABI-server module")
                    .build()
                    .verify(token);

            userID = verified.getSubject();

            log.debug("TokenAuth discovered UserID: {}", userID);

        } catch (Exception e) {
            log.error("Error parsing JWT token", e);
            // System.out.println("Sabi.Service: TokenAuthenticationService could not parse JWT Token!");
            // System.out.println("Token was: " + token);
            // e.printStackTrace();
        }
        return userID;
    }

// --------------------------- CONSTRUCTORS ---------------------------

    /**
     * Notice I'm using encrypt and decrypt functions in a static way
     * which is required by the JWTAuthorizationFilter. You might think that JWT_TOKEN_ALGORITHM won't
     * be initialized then by using in combination with this constructor.
     * The lazy initalization is being done by {@link AppConfig#encryptionService()}
     * which happens during bootstrapping before using the Service within JWTAuthorizationFilter
     *
     * @param pSECRET
     * @param pTOKEN_TTL_IN_SECS
     */
    public TokenAuthenticationService(String pSECRET, String pTOKEN_TTL_IN_SECS) {
        SECRET = pSECRET;
        ACCESS_TOKEN_MAX_VALIDITY_PERIOD_IN_SECS = Long.parseLong(pTOKEN_TTL_IN_SECS);
        if (JWT_TOKEN_ALGORITHM == null) {
            JWT_TOKEN_ALGORITHM = Algorithm.HMAC512(SECRET);
        }
    }
}

