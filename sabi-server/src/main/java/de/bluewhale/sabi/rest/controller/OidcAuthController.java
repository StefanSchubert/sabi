/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.model.OidcLoginRequestTo;
import de.bluewhale.sabi.model.OidcLoginResponseTo;
import de.bluewhale.sabi.persistence.model.OidcProviderLinkEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.OidcProviderLinkRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import de.bluewhale.sabi.security.TokenAuthenticationService;
import de.bluewhale.sabi.services.GoogleIdTokenValidator;
import de.bluewhale.sabi.services.OidcClaims;
import de.bluewhale.sabi.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * REST controller for the OIDC login endpoint.
 * Accepts a Google ID token, validates it, and returns a Sabi JWT.
 *
 * US-1: First-time OIDC user → auto-provisions new Sabi account.
 * US-2: Existing Sabi user (email match) → links Google identity.
 * US-3: Returning OIDC user (sub match) → direct login.
 *
 * @author Stefan Schubert
 */
@RestController
@RequestMapping(value = "api/auth/oidc")
@Tag(name = "OIDC Authentication", description = "OpenID Connect login endpoints")
@Slf4j
public class OidcAuthController {

    @Autowired
    private GoogleIdTokenValidator googleIdTokenValidator;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OidcProviderLinkRepository oidcProviderLinkRepository;

    /** Comma-separated list of admin emails — used to embed the ADMIN role in the issued JWT. */
    @Value("${sabi.admin.users:admin@sabi-project.net}")
    private String adminUsers;

    @Operation(summary = "Exchange Google ID token for Sabi JWT",
            description = "Validates the Google ID token, looks up or provisions the Sabi user, and returns a Sabi JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – Sabi JWT returned."),
            @ApiResponse(responseCode = "400", description = "Bad Request – missing or malformed body."),
            @ApiResponse(responseCode = "401", description = "Unauthorized – invalid or expired ID token, or email not verified."),
            @ApiResponse(responseCode = "409", description = "Conflict – email already linked to a different Google account."),
            @ApiResponse(responseCode = "423", description = "Locked – Sabi account is locked."),
            @ApiResponse(responseCode = "500", description = "Internal Server Error.")
    })
    @PostMapping(value = "/google",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> processOidcLogin(@Valid @RequestBody OidcLoginRequestTo request) {

        // --- Step 1: Validate Google ID token (signature, issuer, exp, aud) ---
        Jwt jwt;
        try {
            jwt = googleIdTokenValidator.validate(request.getIdToken());
        } catch (JwtException e) {
            log.warn("OIDC_INVALID_TOKEN reason={}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "INVALID_ID_TOKEN"));
        }

        // --- Step 2: email_verified must be true ---
        Boolean emailVerified = jwt.getClaim("email_verified");
        if (emailVerified == null || !emailVerified) {
            log.warn("OIDC_EMAIL_NOT_VERIFIED");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "EMAIL_NOT_VERIFIED"));
        }

        // --- Step 3: Map claims to internal value object ---
        String provider = request.getProvider().toUpperCase();
        OidcClaims claims = new OidcClaims(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                emailVerified,
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("locale"),
                provider,
                jwt.getClaimAsString("nonce")
        );

        UserEntity sabiUser;
        boolean provisioned = false;

        // --- Step 4 (US-3): Returning OIDC user – sub already known ---
        Optional<UserEntity> existingBySub = userService.findUserBySub(provider, claims.sub());
        if (existingBySub.isPresent()) {
            sabiUser = existingBySub.get();
            log.info("OIDC_LOGIN_RETURNING sub_hash={} provider={}", claims.sub().hashCode(), provider);

        } else {
            // --- Step 4b (US-2): Email-match: existing Sabi user ---
            UserEntity existingByEmail = userRepository.getByEmail(claims.email());
            if (existingByEmail != null) {
                // Check for IDENTITY_CONFLICT: already linked to a different sub
                Optional<OidcProviderLinkEntity> existingLink =
                        oidcProviderLinkRepository.findByUserAndProvider(existingByEmail, provider);
                if (existingLink.isPresent()
                        && !existingLink.get().getProviderSubject().equals(claims.sub())) {
                    log.warn("OIDC_IDENTITY_CONFLICT user_id={} provider={}", existingByEmail.getId(), provider);
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("error", "IDENTITY_CONFLICT"));
                }
                // Link the existing account (first OIDC login for this user)
                userService.linkOidcIdentity(existingByEmail, claims);
                sabiUser = existingByEmail;
                log.info("OIDC_LINK_CREATED user_id={} provider={}", existingByEmail.getId(), provider);

            } else {
                // --- Step 5 (US-1): New user → provision ---
                sabiUser = userService.provisionOidcUser(claims);
                provisioned = true;
                log.info("OIDC_USER_PROVISIONED sub_hash={} provider={}", claims.sub().hashCode(), provider);
            }
        }

        // --- Step 6: Account status check ---
        if (!sabiUser.isValidated()) {
            log.warn("OIDC_ACCOUNT_LOCKED user_id={}", sabiUser.getId());
            return ResponseEntity.status(HttpStatus.LOCKED)
                    .body(Map.of("error", "ACCOUNT_LOCKED"));
        }

        // --- Step 7: Issue Sabi JWT ---
        boolean isAdmin = TokenAuthenticationService.isAdminEmail(sabiUser.getEmail(), adminUsers);
        String sabiJwt = TokenAuthenticationService.createAuthorizationTokenFor(sabiUser.getEmail(), isAdmin);

        OidcLoginResponseTo response = new OidcLoginResponseTo();
        response.setToken(sabiJwt);
        response.setEmail(sabiUser.getEmail());
        response.setUsername(sabiUser.getUsername());
        response.setProvisioned(provisioned);

        log.info("OIDC_LOGIN_SUCCESS sub_hash={} provider={} provisioned={}", claims.sub().hashCode(), provider, provisioned);
        return ResponseEntity.ok(response);
    }
}

