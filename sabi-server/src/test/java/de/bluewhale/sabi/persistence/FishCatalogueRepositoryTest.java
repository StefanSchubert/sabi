/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 * Part of 002-fish-stock-catalogue — T076
 */

package de.bluewhale.sabi.persistence;

import de.bluewhale.sabi.persistence.model.FishCatalogueEntryEntity;
import de.bluewhale.sabi.persistence.repositories.FishCatalogueEntryRepository;
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
        fishCatalogueEntryRepository.save(entry("Amphiprion ocellaris", "PUBLIC", 1L));
        fishCatalogueEntryRepository.save(entry("Paracanthurus hepatus", "PUBLIC", 1L));

        List<FishCatalogueEntryEntity> results =
                fishCatalogueEntryRepository.searchByQueryAndLang("Amphiprion", "en", 99L);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(e -> e.getScientificName().contains("Amphiprion")));
    }

    /**
     * SC-009: searchByQueryAndLang returns own PENDING entries.
     */
    @Test
    public void searchByQuery_returnsOwnPendingEntries() {
        Long userId = 42L;
        fishCatalogueEntryRepository.save(entry("Naso lituratus", "PENDING", userId));

        List<FishCatalogueEntryEntity> results =
                fishCatalogueEntryRepository.searchByQueryAndLang("Naso", "en", userId);

        assertTrue(results.stream().anyMatch(e -> "Naso lituratus".equals(e.getScientificName())));
    }

    /**
     * SC-009: searchByQueryAndLang does NOT return other users PENDING entries.
     */
    @Test
    public void searchByQuery_doesNotReturnOtherUsersPendingEntries() {
        Long proposer = 55L;
        Long searcher = 77L;
        fishCatalogueEntryRepository.save(entry("Chaetodontoplus duboulayi", "PENDING", proposer));

        List<FishCatalogueEntryEntity> results =
                fishCatalogueEntryRepository.searchByQueryAndLang("Chaetodontoplus", "en", searcher);

        assertTrue(results.stream().noneMatch(e -> "Chaetodontoplus duboulayi".equals(e.getScientificName())),
                "Other user PENDING entries must not appear in search results");
    }

    /**
     * Partial match on scientific name.
     */
    @Test
    public void searchByQuery_partialMatchOnScientificName() {
        fishCatalogueEntryRepository.save(entry("Zebrasoma flavescens", "PUBLIC", 1L));

        List<FishCatalogueEntryEntity> results =
                fishCatalogueEntryRepository.searchByQueryAndLang("Zebra", "en", 99L);

        assertFalse(results.isEmpty());
    }

    /**
     * FR-012: existsByScientificNameAndStatusIn does NOT include REJECTED.
     */
    @Test
    public void uniqueConstraint_rejectedNameNotCountedAsDuplicate() {
        fishCatalogueEntryRepository.save(entry("Pomacanthus imperator", "REJECTED", 1L));

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
        fishCatalogueEntryRepository.save(entry("Centropyge bicolor", "PUBLIC", 1L));

        boolean isDuplicate = fishCatalogueEntryRepository
                .existsByScientificNameAndStatusIn("Centropyge bicolor",
                        java.util.Arrays.asList("PENDING", "PUBLIC"));

        assertTrue(isDuplicate, "PUBLIC entries should be detected as duplicates");
    }
}
