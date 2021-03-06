/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.security;

import de.bluewhale.sabi.api.HttpHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

import static java.util.Collections.emptyList;

/**
 * Encodes and decodes access tokens.
 *
 * @author schubert
 */
public class TokenAuthenticationService {
// ------------------------------ FIELDS ------------------------------


    // Will be lazy initialized with @Value("${accessToken.SECRET}") through constructor
    static private String SECRET;

    // Will be lazy initialized with @Value("${accessToken.TTL}") through constructor
    static  private long ACCESS_TOKEN_MAX_VALIDITY_PERIOD_IN_SECS;

// -------------------------- STATIC METHODS --------------------------

    /**
     * Will be used by login or register, to provide the token belonging to the user.
     * @param pResponse
     * @param pUserID For sabi it's users email address.
     */
    static void addAuthentication(HttpServletResponse pResponse, String pUserID) {
        String JWT = createAuthorizationTokenFor(pUserID);
        pResponse.addHeader(HttpHeader.AUTH_TOKEN, HttpHeader.TOKEN_PREFIX + JWT);
    }

    public static String createAuthorizationTokenFor(String pUserID) {
        return Jwts.builder()
                    .setSubject(pUserID)
                    .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_MAX_VALIDITY_PERIOD_IN_SECS*1000))
                    .signWith(SignatureAlgorithm.HS512, SECRET)
                    .compact();
    }

    /**
     * Take the request and checks if we find a user in there and if so
     * we return the authenticated Authentication Object for that user.
     * @param pRequest
     * @return null if the request did not contained a valid JWT token.
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
     * @param token
     * @return users identified by his email or null in case the token was not valid.
     */
    public static String extractUserFromToken(String token) {
        String user = null;
        try {
            user = Jwts.parser()
                    .setSigningKey(SECRET)
                    .parseClaimsJws(token.replace(HttpHeader.TOKEN_PREFIX, ""))
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            System.out.println("Sabi.Service: TokenAuthenticationService could not parse JWT Token!");
            System.out.println("Token was: "+token);
            e.printStackTrace();
        }
        return user;
    }

// --------------------------- CONSTRUCTORS ---------------------------

    /**
     * Used for lazy initialization.
     * @param pSECRET
     * @param pTOKEN_TTL_IN_SECS
     */
    public TokenAuthenticationService(String pSECRET, String pTOKEN_TTL_IN_SECS) {
        TokenAuthenticationService.SECRET = pSECRET;
        TokenAuthenticationService.ACCESS_TOKEN_MAX_VALIDITY_PERIOD_IN_SECS = Long.valueOf(pTOKEN_TTL_IN_SECS);
    }
}

