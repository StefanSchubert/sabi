/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.rest;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.FishStockService;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Spring MVC proxy controller for fish photos.
 *
 * <p>Serves as the webclient-side gateway for fish photo operations:
 * <ul>
 *   <li>GET  /secured/fishPhoto?fishId={id}  — proxy-loads photo bytes from backend</li>
 *   <li>POST /secured/fishPhoto?fishId={id}  — proxy-uploads photo to backend</li>
 * </ul>
 *
 * <p>Used by {@code fishStockTab.xhtml} (p:graphicImage src) and by the
 * JavaScript fetch() call in {@code fishStockEntryForm.xhtml} after AJAX save.
 * PrimeFaces mode="simple" fileUpload does not transmit file bytes in AJAX requests,
 * so the upload is handled client-side via fetch(FormData) after the AJAX save returns
 * the server-assigned fish ID.
 *
 * Part of 002-fish-stock-catalogue.
 *
 * @author Stefan Schubert
 */
@RestController
@RequestMapping("/secured/fishPhoto")
@Slf4j
public class FishPhotoController {

    @Autowired
    FishStockService fishStockService;

    @Inject
    UserSession userSession;

    /**
     * Proxies the photo bytes from the sabi-backend to the browser.
     * Used by &lt;p:graphicImage value="#{request.contextPath}/secured/fishPhoto?fishId=..."&gt;
     */
    @GetMapping
    public ResponseEntity<byte[]> getPhoto(@RequestParam Long fishId) {
        try {
            byte[] bytes = fishStockService.getPhoto(fishId, userSession.getSabiBackendToken());
            if (bytes == null || bytes.length == 0) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .header("Cache-Control", "max-age=3600")
                    .body(bytes);
        } catch (BusinessException e) {
            log.warn("Could not load photo for fish {}: {}", fishId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Proxies a photo upload from the browser to the sabi-backend.
     * Called by the JavaScript fetch() in fishStockEntryForm.xhtml after AJAX save.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadPhoto(
            @RequestParam Long fishId,
            @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            String contentType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
            fishStockService.uploadPhoto(fishId, file.getBytes(), contentType,
                    userSession.getSabiBackendToken());
            log.info("Photo uploaded via proxy for fish {} ({} bytes, {})",
                    fishId, file.getSize(), contentType);
            return ResponseEntity.noContent().build();
        } catch (BusinessException e) {
            log.error("Proxy photo upload failed for fish {}", fishId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Unexpected error uploading photo for fish {}", fishId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

