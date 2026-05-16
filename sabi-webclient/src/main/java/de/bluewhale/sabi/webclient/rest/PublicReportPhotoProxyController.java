/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Webclient-side proxy for public HouseReef report photo endpoints.
 *
 * <p>The browser fetches image URLs relative to the webclient origin (e.g.
 * {@code /api/public/report/{token}/photo}). In production nginx routes
 * {@code /api/**} directly to the sabi-backend; locally the webclient
 * serves as a thin proxy so the same URLs work without nginx.</p>
 *
 * <p>Security: these endpoints are declared {@code permitAll()} in
 * {@link de.bluewhale.sabi.webclient.config.WebSecurityConfig}.
 * Access is still token-gated: the backend validates that the share token
 * exists, is not expired, and belongs to the requested resource before
 * returning any bytes.</p>
 *
 * <ul>
 *   <li>GET /api/public/report/{token}/photo         – aquarium photo</li>
 *   <li>GET /api/public/report/{token}/fish/{fishId}/photo – fish photo</li>
 * </ul>
 *
 * @author Stefan Schubert
 */
@RestController
@RequestMapping("/api/public/report")
@Slf4j
public class PublicReportPhotoProxyController {

    @Value("${sabi.backend.url}")
    private String sabiBackendUrl;

    // ---- Aquarium photo ------------------------------------------------

    /**
     * Proxies the tank photo from the sabi-backend to the browser.
     * URL pattern matches the one embedded in {@code houseReefReport.xhtml}.
     */
    @GetMapping(value = "/{token}/photo")
    public ResponseEntity<byte[]> getAquariumPhoto(@PathVariable("token") String token) {
        String backendUrl = sabiBackendUrl + "/api/public/report/" + token + "/photo";
        log.debug("Proxy: GET aquarium photo for token {}", token);
        return fetchImageFromBackend(backendUrl);
    }

    // ---- Fish photo ----------------------------------------------------

    /**
     * Proxies a fish photo from the sabi-backend to the browser.
     * URL pattern matches the one embedded in {@code houseReefReport.xhtml}.
     */
    @GetMapping(value = "/{token}/fish/{fishId}/photo")
    public ResponseEntity<byte[]> getFishPhoto(
            @PathVariable("token") String token,
            @PathVariable("fishId") Long fishId) {
        String backendUrl = sabiBackendUrl + "/api/public/report/" + token + "/fish/" + fishId + "/photo";
        log.debug("Proxy: GET fish photo for token {}, fishId {}", token, fishId);
        return fetchImageFromBackend(backendUrl);
    }

    // ---- Internal helper -----------------------------------------------

    private ResponseEntity<byte[]> fetchImageFromBackend(String url) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(java.util.List.of(MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG, MediaType.ALL));
            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<byte[]> backendResponse = restTemplate.exchange(
                    url, HttpMethod.GET, request, byte[].class);

            if (backendResponse.getStatusCode() == HttpStatus.NOT_FOUND
                    || backendResponse.getBody() == null
                    || backendResponse.getBody().length == 0) {
                return ResponseEntity.notFound().build();
            }

            // Determine content type from backend response (default: JPEG)
            MediaType contentType = backendResponse.getHeaders().getContentType();
            if (contentType == null) {
                contentType = MediaType.IMAGE_JPEG;
            }

            return ResponseEntity.ok()
                    .contentType(contentType)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                    .body(backendResponse.getBody());

        } catch (HttpClientErrorException.NotFound e) {
            log.debug("Backend returned 404 for {}", url);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.warn("Failed to proxy photo from {}: {}", url, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }
}

