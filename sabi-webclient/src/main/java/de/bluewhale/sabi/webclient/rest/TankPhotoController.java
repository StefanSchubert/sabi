/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.rest;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.TankService;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Spring MVC proxy controller for aquarium photos.
 *
 * <p>Serves as the webclient-side gateway for aquarium photo operations:
 * <ul>
 *   <li>GET  /secured/tankPhoto?tankId={id}  — proxy-loads photo bytes from backend</li>
 *   <li>POST /secured/tankPhoto?tankId={id}  — proxy-uploads photo to backend</li>
 * </ul>
 *
 * @author Stefan Schubert
 */
@RestController
@RequestMapping("/secured/tankPhoto")
@Slf4j
public class TankPhotoController {

    @Autowired
    TankService tankService;

    @Inject
    UserSession userSession;

    /**
     * Proxies the photo bytes from the sabi-backend to the browser.
     * Used by &lt;p:graphicImage value="#{request.contextPath}/secured/tankPhoto?tankId=..."&gt;
     */
    @GetMapping
    public ResponseEntity<byte[]> getPhoto(@RequestParam Long tankId) {
        try {
            byte[] bytes = tankService.getPhoto(tankId, userSession.getSabiBackendToken());
            if (bytes == null || bytes.length == 0) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .header("Cache-Control", "max-age=3600")
                    .body(bytes);
        } catch (BusinessException e) {
            log.warn("Could not load photo for tank {}: {}", tankId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Proxies a photo upload from the browser to the sabi-backend.
     * Called by the JavaScript fetch() in tankEditor.xhtml after AJAX save.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadPhoto(
            @RequestParam Long tankId,
            @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            String contentType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
            tankService.uploadPhoto(tankId, file.getBytes(), contentType,
                    userSession.getSabiBackendToken());
            log.info("Photo uploaded via proxy for tank {} ({} bytes, {})",
                    tankId, file.getSize(), contentType);
            return ResponseEntity.noContent().build();
        } catch (BusinessException e) {
            log.error("Proxy photo upload failed for tank {}", tankId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Unexpected error uploading photo for tank {}", tankId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

