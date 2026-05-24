/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 */
package de.bluewhale.sabi.webclient.rest;

import de.bluewhale.sabi.exception.BusinessException;
import de.bluewhale.sabi.webclient.CDIBeans.UserSession;
import de.bluewhale.sabi.webclient.apigateway.CoralStockService;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Spring MVC proxy controller for coral photos.
 *
 * <ul>
 *   <li>GET  /secured/coralPhoto?coralId={id}  — proxy-loads photo bytes from backend</li>
 *   <li>POST /secured/coralPhoto?coralId={id}  — proxy-uploads photo to backend</li>
 * </ul>
 *
 * Part of 005-coral-stock.
 *
 * @author Stefan Schubert
 */
@RestController
@RequestMapping("/secured/coralPhoto")
@Slf4j
public class CoralPhotoController {

    @Autowired
    CoralStockService coralStockService;

    @Inject
    UserSession userSession;

    @GetMapping
    public ResponseEntity<byte[]> getPhoto(@RequestParam Long coralId) {
        try {
            byte[] bytes = coralStockService.getPhoto(coralId, userSession.getSabiBackendToken());
            if (bytes == null || bytes.length == 0) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .header("Cache-Control", "max-age=3600")
                    .body(bytes);
        } catch (BusinessException e) {
            log.warn("Could not load photo for coral {}: {}", coralId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadPhoto(
            @RequestParam Long coralId,
            @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            String contentType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
            coralStockService.uploadPhoto(coralId, file.getBytes(), contentType,
                    userSession.getSabiBackendToken());
            log.info("Photo uploaded via proxy for coral {} ({} bytes, {})",
                    coralId, file.getSize(), contentType);
            return ResponseEntity.noContent().build();
        } catch (BusinessException e) {
            log.error("Proxy photo upload failed for coral {}", coralId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Unexpected error uploading photo for coral {}", coralId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

