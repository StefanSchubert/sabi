/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import de.bluewhale.sabi.api.HttpHeader;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Date;

import static java.util.Collections.emptyList;

/**
 * Encodes and decodes access tokens.
 *
 * @author schubert
 */
public class TokenAuthenticationService {
    private static final String SABI_JWT_ISSUER = "SABI-server module";
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
        String jsonWebtoken = createAuthorizationTokenFor(pUserID);
        pResponse.addHeader(HttpHeader.AUTH_TOKEN, HttpHeader.TOKEN_PREFIX + jsonWebtoken);
    }

    public static String createAuthorizationTokenFor(String pUserID) {

        Date expiresAt = new Date(System.currentTimeMillis() + ACCESS_TOKEN_MAX_VALIDITY_PERIOD_IN_SECS * 1000);

        String jwtToken = JWT.create()
              //   .withSubject(pUserID)
              //  .withExpiresAt(expiresAt)
                .withIssuer(SABI_JWT_ISSUER)
                .sign(JWT_TOKEN_ALGORITHM);

        return jwtToken;
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
        try {

            // Make sure we have a valid signed token here
            /*
            JWTVerifier verifier = JWT.require(JWT_TOKEN_ALGORITHM)
                    .withIssuer(SABI_JWT_ISSUER)
                    .build();

            DecodedJWT jwt = verifier.verify(token);
            */

            DecodedJWT decoded = JWT.decode(token);

            DecodedJWT verified = JWT.require(JWT_TOKEN_ALGORITHM)
                    .withIssuer("SABI-server module")
                    .build()
                    .verify(token);

            userID = verified.getSubject();

        } catch (Exception e) {
            System.out.println("Sabi.Service: TokenAuthenticationService could not parse JWT Token!");
            System.out.println("Token was: " + token);
            e.printStackTrace();
        }
        return userID;
    }

// --------------------------- CONSTRUCTORS ---------------------------

    /**
     * Used for lazy initialization.
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

