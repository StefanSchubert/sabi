/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.configs;

import de.bluewhale.sabi.services.PhotoStorageService;
import de.bluewhale.sabi.services.PhotoStorageServiceImpl;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers two named {@link PhotoStorageService} beans with separate configuration prefixes:
 * <ul>
 *   <li>{@code fishPhotoStorage}    — bound to {@code sabi.fish.photo.*}</li>
 *   <li>{@code aquariumPhotoStorage} — bound to {@code sabi.aquarium.photo.*}</li>
 * </ul>
 *
 * Inject via {@code @Qualifier("fishPhotoStorage")} or {@code @Qualifier("aquariumPhotoStorage")}.
 *
 * @author Stefan Schubert
 */
@Configuration
public class PhotoStorageConfig {

    @Bean("fishPhotoStorage")
    @ConfigurationProperties(prefix = "sabi.fish.photo")
    public PhotoStorageService fishPhotoStorage() {
        return new PhotoStorageServiceImpl();
    }

    @Bean("aquariumPhotoStorage")
    @ConfigurationProperties(prefix = "sabi.aquarium.photo")
    public PhotoStorageService aquariumPhotoStorage() {
        return new PhotoStorageServiceImpl();
    }
}

