/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 * Part of 002-fish-stock-catalogue — T077
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.mapper.FishCatalogueMapper;
import de.bluewhale.sabi.model.FishCatalogueSearchResultTo;
import de.bluewhale.sabi.persistence.model.FishCatalogueEntryEntity;
import de.bluewhale.sabi.persistence.model.FishCatalogueI18nEntity;
import de.bluewhale.sabi.persistence.repositories.FishCatalogueEntryRepository;
import de.bluewhale.sabi.persistence.repositories.FishCatalogueI18nRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.util.TestDataFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static de.bluewhale.sabi.util.TestContainerVersions.MARIADB_11_3_2;
import static de.bluewhale.sabi.util.TestDataFactory.TESTUSER_EMAIL1;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

/**
 * Performance test SC-003: Catalogue search must return results in <= 1000ms for 500 entries.
 * Part of 002-fish-stock-catalogue T077.
 */
@SpringBootTest
@Testcontainers
@Tag("performance")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class FishCataloguePerformanceTest {

    @Container
    @ServiceConnection
    static MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>(MARIADB_11_3_2);

    static TestDataFactory testDataFactory = TestDataFactory.getInstance();

    @Autowired
    private FishCatalogueService fishCatalogueService;

    @MockitoBean
    private FishCatalogueEntryRepository fishCatalogueEntryRepository;

    @MockitoBean
    private FishCatalogueI18nRepository fishCatalogueI18nRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private FishCatalogueMapper fishCatalogueMapper;

    /**
     * SC-003: Catalogue search with 500-entry result set must complete in <= 1000ms.
     */
    @Test
    public void catalogueSearch_500entries_completesInUnder1Second() {
        // Build 500 mock catalogue entities
        List<FishCatalogueEntryEntity> entries = new ArrayList<>(500);
        for (int i = 0; i < 500; i++) {
            FishCatalogueEntryEntity e = new FishCatalogueEntryEntity();
            e.setId((long) i);
            e.setScientificName("Species " + i);
            e.setStatus("PUBLIC");
            e.setI18nEntries(new ArrayList<>());
            entries.add(e);
        }

        UserEntity user = testDataFactory.getNewTestUserEntity(
                testDataFactory.getNewTestUserTo(TESTUSER_EMAIL1));
        user.setId(1L);

        given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(user);
        given(fishCatalogueEntryRepository.searchByQueryAndLang(
                eq("sp"), anyString(), anyLong())).willReturn(entries);

        // Mock mapper: lightweight 1:1 mapping
        given(fishCatalogueMapper.mapEntity2SearchResult(any(), anyString()))
                .willAnswer(inv -> {
                    FishCatalogueEntryEntity entity = inv.getArgument(0);
                    FishCatalogueSearchResultTo result = new FishCatalogueSearchResultTo();
                    result.setId(entity.getId());
                    result.setScientificName(entity.getScientificName());
                    result.setCommonName(entity.getScientificName());
                    return result;
                });

        long start = System.currentTimeMillis();
        List<FishCatalogueSearchResultTo> results = fishCatalogueService.search("sp", "en", TESTUSER_EMAIL1);
        long durationMs = System.currentTimeMillis() - start;

        assertNotNull(results);
        assertEquals(500, results.size(), "Should return all 500 entries");
        assertTrue(durationMs < 1000,
                "SC-003 FAILED: Catalogue search took " + durationMs + "ms (must be < 1000ms)");
    }
}
