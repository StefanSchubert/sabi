/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Filesystem-based implementation of {@link PhotoStorageService}.
 * Validates magic bytes, enforces max size, stores under {storage-dir}/{userId}/{fishId}.{ext}.
 * Part of 002-fish-stock-catalogue.
 *
 * @author Stefan Schubert
 */
@Slf4j
public class PhotoStorageServiceImpl implements PhotoStorageService {

    /** Configurable storage root, default /tmp/sabi-fish-photos for dev. */
    private String storageDir = "/tmp/sabi-fish-photos";

    /** Maximum allowed file size in bytes (default 5 MB). */
    private long maxSizeBytes = 5_242_880L;

    // ---- Magic-byte signatures ----
    private static final byte[] MAGIC_JPEG  = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] MAGIC_PNG   = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] MAGIC_WEBP1 = {0x52, 0x49, 0x46, 0x46}; // "RIFF"
    private static final byte[] MAGIC_WEBP2 = {0x57, 0x45, 0x42, 0x50}; // "WEBP" at offset 8
    private static final byte[] MAGIC_GIF   = {0x47, 0x49, 0x46, 0x38}; // "GIF8"

    @Override
    public String store(Long userId, Long fishId, byte[] bytes, String contentType) {
        if (bytes.length > maxSizeBytes) {
            throw new FishPhotoTooLargeException(
                    "Photo exceeds max size of " + maxSizeBytes + " bytes; got " + bytes.length);
        }
        validateMagicBytes(bytes, contentType);

        String ext = resolveExtension(contentType);
        Path userDir = Paths.get(storageDir, String.valueOf(userId));
        try {
            Files.createDirectories(userDir);
            Path target = userDir.resolve(fishId + "." + ext);
            Files.write(target, bytes);
            // Return relative path: {userId}/{fishId}.{ext}
            return userId + "/" + fishId + "." + ext;
        } catch (IOException e) {
            log.error("Failed to store photo for fish {} / user {}", fishId, userId, e);
            throw new UncheckedIOException("Failed to store photo", e);
        }
    }

    @Override
    public byte[] load(String relativePath) {
        Path file = Paths.get(storageDir, relativePath);
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            log.error("Failed to load photo from {}", file, e);
            throw new UncheckedIOException("Failed to load photo", e);
        }
    }

    @Override
    public void delete(String relativePath) {
        Path file = Paths.get(storageDir, relativePath);
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Failed to delete photo at {}", file, e);
        }
    }

    @Override
    public String resolveExtension(String contentType) {
        if (contentType == null) return "jpg";
        return switch (contentType.toLowerCase()) {
            case "image/jpeg" -> "jpg";
            case "image/png"  -> "png";
            case "image/webp" -> "webp";
            case "image/gif"  -> "gif";
            default           -> "jpg";
        };
    }

    // ---- Properties setters for @ConfigurationProperties ----

    public void setStorageDir(String storageDir) {
        this.storageDir = storageDir;
    }

    public String getStorageDir() {
        return storageDir;
    }

    public void setMaxSizeBytes(long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    // ---- Internal helpers ----

    private void validateMagicBytes(byte[] bytes, String contentType) {
        if (bytes.length < 12) {
            throw new FishPhotoInvalidFormatException("File too small to be a valid image.");
        }
        if (startsWith(bytes, MAGIC_JPEG)) return;
        if (startsWith(bytes, MAGIC_PNG)) return;
        if (startsWith(bytes, MAGIC_GIF)) return;
        // WebP: "RIFF" at offset 0 and "WEBP" at offset 8
        if (startsWith(bytes, MAGIC_WEBP1) && bytes.length >= 12) {
            byte[] offset8 = {bytes[8], bytes[9], bytes[10], bytes[11]};
            if (startsWith(offset8, MAGIC_WEBP2)) return;
        }
        throw new FishPhotoInvalidFormatException(
                "Unsupported image format (magic bytes mismatch for content-type: " + contentType + ")");
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }
}

