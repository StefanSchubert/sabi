/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */
package de.bluewhale.sabi.persistence.repositories;
import de.bluewhale.sabi.persistence.model.OidcProviderLinkEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
/**
 * Spring Data JPA repository for OidcProviderLinkEntity.
 *
 * @author Stefan Schubert
 */
public interface OidcProviderLinkRepository extends JpaRepository<OidcProviderLinkEntity, Long> {
    Optional<OidcProviderLinkEntity> findByProviderAndProviderSubject(String provider, String providerSubject);
    Optional<OidcProviderLinkEntity> findByUserAndProvider(UserEntity user, String provider);
}
