/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.security;

import de.bluewhale.sabi.model.OidcLoginRequestTo;
import de.bluewhale.sabi.model.OidcLoginResponseTo;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.utils.JwtDecoder;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

/**
 * Handles a successful Google OIDC authentication by exchanging the Google ID token
 * for a Sabi JWT and storing it in the user session.
 *
 * US-1: first-time OIDC user → backend provisions account, returns JWT
 * US-2: existing user (email match) → backend links identity, returns JWT
 * US-3: returning OIDC user (sub known) → backend returns JWT directly
 *
 * @author Stefan Schubert
 */
@Component
@Slf4j
public class SabiOidcSuccessHandler implements AuthenticationSuccessHandler {

    @Value("${sabi.backend.url}")
    private String sabiBackendUrl;

    @Autowired
    private UserSession userSession;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        String rawIdToken = oidcUser.getIdToken().getTokenValue();

        OidcLoginRequestTo requestBody = new OidcLoginRequestTo();
        requestBody.setIdToken(rawIdToken);
        requestBody.setProvider("GOOGLE");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<OidcLoginRequestTo> httpEntity = new HttpEntity<>(requestBody, headers);

        String oidcEndpoint = sabiBackendUrl + "/api/auth/oidc/google";

        try {
            ResponseEntity<OidcLoginResponseTo> backendResponse =
                    restTemplate.exchange(oidcEndpoint, HttpMethod.POST, httpEntity, OidcLoginResponseTo.class);

            if (backendResponse.getStatusCode().is2xxSuccessful() && backendResponse.getBody() != null) {
                OidcLoginResponseTo sabiResponse = backendResponse.getBody();
                // Store Sabi JWT and username in the session for subsequent API calls
                userSession.setSabiBackendToken("Bearer " + sabiResponse.getToken());
                userSession.setUserName(sabiResponse.getUsername());
                // Extract email and admin role from the Sabi JWT rather than from a separate config.
                userSession.setUserEmail(sabiResponse.getEmail());
                userSession.setAdminRole(JwtDecoder.hasAdminRole(sabiResponse.getToken()));

                log.info("OIDC_LOGIN_SUCCESS username={} provisioned={}", sabiResponse.getUsername(), sabiResponse.isProvisioned());

                // Google zeigt T&C und Datenschutz bereits im OAuth-Consent-Dialog an –
                // daher direkt zum Userportal, unabhängig ob provisioned oder nicht.
                response.sendRedirect(request.getContextPath() + "/secured/userportal.xhtml");
            } else {
                log.warn("OIDC backend returned non-2xx: {}", backendResponse.getStatusCode());
                response.sendRedirect(request.getContextPath() + "/login.xhtml?error=oidc");
            }
        } catch (HttpClientErrorException e) {
            HttpStatusCode status = e.getStatusCode();
            if (status.value() == 423) {
                log.warn("OIDC_ACCOUNT_LOCKED");
                response.sendRedirect(request.getContextPath() + "/login.xhtml?error=oidc_locked");
            } else {
                log.warn("OIDC backend client error: {} {}", status, e.getResponseBodyAsString());
                response.sendRedirect(request.getContextPath() + "/login.xhtml?error=oidc");
            }
        } catch (Exception e) {
            log.error("OIDC backend call failed: {}", e.getMessage(), e);
            response.sendRedirect(request.getContextPath() + "/login.xhtml?error=server");
        }
    }
}

