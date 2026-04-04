/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence;

import de.bluewhale.sabi.persistence.model.OidcProviderLinkEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.OidcProviderLinkRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import de.bluewhale.sabi.util.TestContainerVersions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Persistence-Layer Tests for OidcProviderLinkRepository.
 * Tests: findByProviderAndProviderSubject, Cascade-Delete, Unique-Constraint.
 *
 * @author Stefan Schubert
 */
@SpringBootTest
@Testcontainers
@Tag("IntegrationTest")
@Transactional
@DirtiesContext
public class OidcProviderLinkRepositoryTest implements TestContainerVersions {

    @Container
    @ServiceConnection
    static MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>(MARIADB_11_3_2);

    @Autowired
    OidcProviderLinkRepository oidcProviderLinkRepository;

    @Autowired
    UserRepository userRepository;

    @AfterAll
    static void cleanup() {
        mariaDBContainer.stop();
    }

    private UserEntity createTestUser(String email) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setUsername(email.split("@")[0]);
        user.setPassword("");
        user.setValidateToken("");
        user.setValidated(true);
        user.setOidcManaged(true);
        user.setLanguage("de");
        user.setCountry("DE");
        return userRepository.saveAndFlush(user);
    }

    private OidcProviderLinkEntity createLink(UserEntity user, String provider, String sub) {
        OidcProviderLinkEntity link = new OidcProviderLinkEntity();
        link.setUser(user);
        link.setProvider(provider);
        link.setProviderSubject(sub);
        link.setLinkedAt(LocalDateTime.now());
        OidcProviderLinkEntity saved = oidcProviderLinkRepository.saveAndFlush(link);
        // Bidirektionale Beziehung in Sync halten: EclipseLink benötigt die befüllte
        // Collection für CascadeType.REMOVE (Lazy-Collection wäre sonst leer beim Delete).
        user.getOidcProviderLinks().add(saved);
        return saved;
    }

    @Test
    void connectionEstablished() {
        assertThat(mariaDBContainer.isRunning()).isTrue();
    }

    @Test
    @Rollback
    void testFindByProviderAndProviderSubject_findsExistingLink() {
        // Given
        UserEntity user = createTestUser("oidc_test1@example.com");
        createLink(user, "GOOGLE", "google-sub-12345");

        // When
        Optional<OidcProviderLinkEntity> found =
                oidcProviderLinkRepository.findByProviderAndProviderSubject("GOOGLE", "google-sub-12345");

        // Then
        assertTrue(found.isPresent());
        assertEquals("google-sub-12345", found.get().getProviderSubject());
        assertEquals(user.getId(), found.get().getUser().getId());
    }

    @Test
    @Rollback
    void testFindByProviderAndProviderSubject_returnsEmptyForUnknownSub() {
        Optional<OidcProviderLinkEntity> found =
                oidcProviderLinkRepository.findByProviderAndProviderSubject("GOOGLE", "nonexistent-sub");
        assertTrue(found.isEmpty());
    }

    @Test
    @Rollback
    void testCascadeDelete_linkDeletedWhenUserDeleted() {
        // Given
        UserEntity user = createTestUser("oidc_cascade@example.com");
        OidcProviderLinkEntity link = createLink(user, "GOOGLE", "sub-for-cascade");
        Long linkId = link.getId();

        // When
        userRepository.delete(user);
        userRepository.flush();

        // Then
        Optional<OidcProviderLinkEntity> found = oidcProviderLinkRepository.findById(linkId);
        assertTrue(found.isEmpty(), "Link should have been cascade-deleted with user");
    }

    @Test
    @Rollback
    void testUniqueConstraint_duplicateProviderSubjectThrowsException() {
        // Given
        UserEntity user1 = createTestUser("oidc_unique1@example.com");
        UserEntity user2 = createTestUser("oidc_unique2@example.com");
        createLink(user1, "GOOGLE", "same-sub-value");

        // When / Then – same provider+sub for a different user must fail
        // Note: EclipseLink wraps constraint violations as JpaSystemException (not DataIntegrityViolationException
        // like Hibernate does). Both are subclasses of DataAccessException → use the common parent.
        assertThrows(DataAccessException.class, () -> {
            createLink(user2, "GOOGLE", "same-sub-value");
        });
    }
}

