/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */
package de.bluewhale.sabi.webclient.rest;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.InvertebrateStockService;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Spring MVC proxy controller for invertebrate photos.
 *
 * <ul>
 *   <li>GET  /secured/invertebratePhoto?invertebrateId={id}  — proxy-loads photo bytes from backend</li>
 *   <li>POST /secured/invertebratePhoto?invertebrateId={id}  — proxy-uploads photo to backend</li>
 * </ul>
 *
 * Part of 006-invertebrate-tracking.
 *
 * @author Stefan Schubert
 */
@RestController
@RequestMapping("/secured/invertebratePhoto")
@Slf4j
public class InvertebratePhotoController {

    @Autowired
    InvertebrateStockService invertebrateStockService;

    @Inject
    UserSession userSession;

    @GetMapping
    public ResponseEntity<byte[]> getPhoto(@RequestParam Long invertebrateId) {
        try {
            byte[] bytes = invertebrateStockService.getPhoto(invertebrateId, userSession.getSabiBackendToken());
            if (bytes == null || bytes.length == 0) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .header("Cache-Control", "max-age=3600")
                    .body(bytes);
        } catch (BusinessException e) {
            log.warn("Could not load photo for invertebrate {}: {}", invertebrateId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadPhoto(
            @RequestParam Long invertebrateId,
            @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            String contentType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
            invertebrateStockService.uploadPhoto(invertebrateId, file.getBytes(), contentType,
                    userSession.getSabiBackendToken());
            log.info("Photo uploaded via proxy for invertebrate {} ({} bytes, {})",
                    invertebrateId, file.getSize(), contentType);
            return ResponseEntity.noContent().build();
        } catch (BusinessException e) {
            log.error("Proxy photo upload failed for invertebrate {}", invertebrateId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Unexpected error uploading photo for invertebrate {}", invertebrateId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
