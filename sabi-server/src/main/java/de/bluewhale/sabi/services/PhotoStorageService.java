/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

/**
 * Service for storing and retrieving fish photos on the filesystem.
 * Part of 002-fish-stock-catalogue.
 *
 * @author Stefan Schubert
 */
public interface PhotoStorageService {

    /**
     * Stores a photo for a fish entry.
     *
     * @param userId      owner of the fish
     * @param fishId      ID of the fish entry
     * @param bytes       raw image bytes
     * @param contentType MIME type (image/jpeg, image/png, image/webp, image/gif)
     * @return relative path to the stored file (for persistence in FishPhotoEntity)
     * @throws FishPhotoInvalidFormatException if magic bytes do not match supported formats
     * @throws FishPhotoTooLargeException      if bytes exceed configured max-size-bytes
     */
    String store(Long userId, Long fishId, byte[] bytes, String contentType);

    /**
     * Loads the raw bytes of a stored photo.
     *
     * @param relativePath as stored by {@link #store}
     * @return raw image bytes
     */
    byte[] load(String relativePath);

    /**
     * Deletes a stored photo.
     *
     * @param relativePath as stored by {@link #store}
     */
    void delete(String relativePath);

    /**
     * Resolves the file extension from a MIME type.
     *
     * @param contentType e.g. image/jpeg
     * @return file extension without dot, e.g. "jpg"
     */
    String resolveExtension(String contentType);

    // --- Checked exceptions used as RuntimeExceptions for cleaner service API ---

    class FishPhotoInvalidFormatException extends RuntimeException {
        public FishPhotoInvalidFormatException(String message) {
            super(message);
        }
    }

    class FishPhotoTooLargeException extends RuntimeException {
        public FishPhotoTooLargeException(String message) {
            super(message);
        }
    }
}

