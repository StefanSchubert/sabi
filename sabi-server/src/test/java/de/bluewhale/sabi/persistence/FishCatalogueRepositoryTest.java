/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 * Part of 002-fish-stock-catalogue — T076
 */

package de.bluewhale.sabi.persistence;

import de.bluewhale.sabi.persistence.model.FishCatalogueEntryEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.FishCatalogueEntryRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static de.bluewhale.sabi.util.TestContainerVersions.MARIADB_11_3_2;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository tests for FishCatalogueEntryRepository.
 * Covers SC-009 visibility rules, FR-012 REJECTED duplicate constraint.
 * T076: 6 test cases.
 */
@SpringBootTest
@Testcontainers
@Transactional
@DirtiesContext
@Tag("IntegrationTest")
public class FishCatalogueRepositoryTest {

    @Container
    @ServiceConnection
    static MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>(MARIADB_11_3_2);

    @Autowired
    private FishCatalogueEntryRepository fishCatalogueEntryRepository;

    @Autowired
    private UserRepository userRepository;

    // Dynamic user IDs — populated in @BeforeEach (FK constraint on fish_catalogue.proposer_user_id)
    private Long userId1;
    private Long userId2;

    @BeforeEach
    public void setUp() {
        userId1 = createUser("cataloguetest1@test.de", "catuser1");
        userId2 = createUser("cataloguetest2@test.de", "catuser2");
    }

    private Long createUser(String email, String username) {
        UserEntity u = new UserEntity();
        u.setEmail(email);
        u.setUsername(username);
        u.setPassword("hashed");
        u.setValidateToken("tok-" + username);
        u.setValidated(true);
        u.setLanguage("en");
        u.setCountry("DE");
        return userRepository.save(u).getId();
    }

    // ---- Helpers ----

    private FishCatalogueEntryEntity entry(String scientificName, String status, Long proposerId) {
        FishCatalogueEntryEntity e = new FishCatalogueEntryEntity();
        e.setScientificName(scientificName);
        e.setStatus(status);
        e.setProposerUserId(proposerId);
        e.setProposalDate(LocalDate.now());
        e.setI18nEntries(new ArrayList<>());
        return e;
    }

    // ---- Test Cases ----

    /**
     * SC-009: searchByQueryAndLang returns PUBLIC entries.
     */
    @Test
    public void searchByQuery_returnsPublicEntries() {
        fishCatalogueEntryRepository.saveAndFlush(entry("Amphiprion ocellaris", "PUBLIC", userId1));
        fishCatalogueEntryRepository.saveAndFlush(entry("Paracanthurus hepatus", "PUBLIC", userId1));

        List<FishCatalogueEntryEntity> results =
                fishCatalogueEntryRepository.searchByQueryAndLang("Amphiprion", "en", userId2);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(e -> e.getScientificName().contains("Amphiprion")));
    }

    /**
     * SC-009: searchByQueryAndLang returns own PENDING entries.
     */
    @Test
    public void searchByQuery_returnsOwnPendingEntries() {
        fishCatalogueEntryRepository.saveAndFlush(entry("Naso lituratus", "PENDING", userId1));

        List<FishCatalogueEntryEntity> results =
                fishCatalogueEntryRepository.searchByQueryAndLang("Naso", "en", userId1);

        assertTrue(results.stream().anyMatch(e -> "Naso lituratus".equals(e.getScientificName())));
    }

    /**
     * SC-009: searchByQueryAndLang does NOT return other users PENDING entries.
     */
    @Test
    public void searchByQuery_doesNotReturnOtherUsersPendingEntries() {
        // userId1 proposes an entry; userId2 searches — should NOT see it
        fishCatalogueEntryRepository.saveAndFlush(entry("Chaetodontoplus duboulayi", "PENDING", userId1));

        List<FishCatalogueEntryEntity> results =
                fishCatalogueEntryRepository.searchByQueryAndLang("Chaetodontoplus", "en", userId2);

        assertTrue(results.stream().noneMatch(e -> "Chaetodontoplus duboulayi".equals(e.getScientificName())),
                "Other user PENDING entries must not appear in search results");
    }

    /**
     * Partial match on scientific name.
     */
    @Test
    public void searchByQuery_partialMatchOnScientificName() {
        fishCatalogueEntryRepository.saveAndFlush(entry("Zebrasoma flavescens", "PUBLIC", userId1));

        List<FishCatalogueEntryEntity> results =
                fishCatalogueEntryRepository.searchByQueryAndLang("Zebra", "en", userId2);

        assertFalse(results.isEmpty());
    }

    /**
     * FR-012: existsByScientificNameAndStatusIn does NOT include REJECTED.
     */
    @Test
    public void uniqueConstraint_rejectedNameNotCountedAsDuplicate() {
        fishCatalogueEntryRepository.saveAndFlush(entry("Pomacanthus imperator", "REJECTED", userId1));

        boolean isDuplicate = fishCatalogueEntryRepository
                .existsByScientificNameAndStatusIn("Pomacanthus imperator",
                        java.util.Arrays.asList("PENDING", "PUBLIC"));

        assertFalse(isDuplicate, "REJECTED entries should not count as duplicates for new proposals");
    }

    /**
     * existsByScientificNameAndStatusIn returns true for PUBLIC entry (duplicate detection).
     */
    @Test
    public void uniqueConstraint_publicNameCountedAsDuplicate() {
        fishCatalogueEntryRepository.saveAndFlush(entry("Centropyge bicolor", "PUBLIC", userId1));

        boolean isDuplicate = fishCatalogueEntryRepository
                .existsByScientificNameAndStatusIn("Centropyge bicolor",
                        java.util.Arrays.asList("PENDING", "PUBLIC"));

        assertTrue(isDuplicate, "PUBLIC entries should be detected as duplicates");
    }
}
