/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 * Part of 006-invertebrate-tracking — T098, T099
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.persistence.model.InvertebrateCatalogueEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.InvertebrateCatalogueI18nRepository;
import de.bluewhale.sabi.persistence.repositories.InvertebrateCatalogueRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import de.bluewhale.sabi.mapper.InvertebrateCatalogueMapper;
import de.bluewhale.sabi.util.TestDataFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static de.bluewhale.sabi.util.TestContainerVersions.MARIADB_11_3_2;
import static de.bluewhale.sabi.util.TestDataFactory.TESTUSER_EMAIL1;
import static de.bluewhale.sabi.util.TestDataFactory.TESTUSER_EMAIL2;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

/**
 * Service-layer tests for InvertebrateCatalogueService.
 * Covers US4 (catalogue search / catalogue link), US5 (proposals), US6 (admin approve/reject).
 * Part of 006-invertebrate-tracking.
 */
@SpringBootTest
@Testcontainers
@Tag("ServiceTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class InvertebrateCatalogueServiceTest {

    /** Default admin email from sabi.admin.users property default. */
    private static final String ADMIN_EMAIL = "admin@sabi-project.net";

    @Container
    @ServiceConnection
    static MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>(MARIADB_11_3_2);

    static TestDataFactory testDataFactory = TestDataFactory.getInstance();

    @Autowired
    private InvertebrateCatalogueService invertebrateCatalogueService;

    @MockitoBean
    private InvertebrateCatalogueRepository invertebrateCatalogueRepository;

    @MockitoBean
    private InvertebrateCatalogueI18nRepository invertebrateCatalogueI18nRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private InvertebrateCatalogueMapper invertebrateCatalogueMapper;

    @MockitoBean
    private NotificationService notificationService;

    // ---- Helpers ----

    private UserEntity testUser1() {
        UserEntity u = testDataFactory.getNewTestUserEntity(testDataFactory.getNewTestUserTo(TESTUSER_EMAIL1));
        u.setId(1L);
        return u;
    }

    private UserEntity testUser2() {
        UserEntity u = testDataFactory.getNewTestUserEntity(testDataFactory.getNewTestUserTo(TESTUSER_EMAIL2));
        u.setId(2L);
        return u;
    }

    private InvertebrateCatalogueEntity publicEntry() {
        InvertebrateCatalogueEntity e = new InvertebrateCatalogueEntity();
        e.setId(1L);
        e.setScientificName("Lysmata amboinensis");
        e.setTaxonomicCategory("CRUSTACEAN");
        e.setStatus("PUBLIC");
        e.setI18nEntries(new ArrayList<>());
        return e;
    }

    private InvertebrateCatalogueEntity pendingEntry(Long proposerUserId) {
        InvertebrateCatalogueEntity e = new InvertebrateCatalogueEntity();
        e.setId(2L);
        e.setScientificName("Stenopus hispidus");
        e.setTaxonomicCategory("CRUSTACEAN");
        e.setStatus("PENDING");
        e.setProposerUserId(proposerUserId);
        e.setProposalDate(LocalDate.now());
        e.setI18nEntries(new ArrayList<>());
        return e;
    }

    private InvertebrateCatalogueSearchResultTo searchResultTo(InvertebrateCatalogueEntity entity) {
        InvertebrateCatalogueSearchResultTo result = new InvertebrateCatalogueSearchResultTo();
        result.setId(entity.getId());
        result.setScientificName(entity.getScientificName());
        return result;
    }

    // ---- US4: Catalogue search ----

    /**
     * US4: search returns PUBLIC entries matching query.
     */
    @Test
    public void search_publicEntry_matchesQuery() {
        InvertebrateCatalogueEntity entry = publicEntry();
        given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(testUser1());
        given(invertebrateCatalogueRepository.findAll()).willReturn(List.of(entry));
        given(invertebrateCatalogueMapper.toSearchResult(eq(entry), anyString()))
                .willReturn(searchResultTo(entry));

        List<InvertebrateCatalogueSearchResultTo> results =
                invertebrateCatalogueService.search("Lysmata", "en", TESTUSER_EMAIL1);

        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals("Lysmata amboinensis", results.get(0).getScientificName());
    }

    /**
     * US4: PENDING entry is only visible to the proposer, not to other users.
     */
    @Test
    public void search_pendingEntry_visibleOnlyToProposer() {
        UserEntity user1 = testUser1();
        UserEntity user2 = testUser2();
        InvertebrateCatalogueEntity pending = pendingEntry(user1.getId());

        // User 1 (proposer) should see the pending entry
        given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(user1);
        given(invertebrateCatalogueRepository.findAll()).willReturn(List.of(pending));
        given(invertebrateCatalogueMapper.toSearchResult(eq(pending), anyString()))
                .willReturn(searchResultTo(pending));

        List<InvertebrateCatalogueSearchResultTo> resultsAsProposer =
                invertebrateCatalogueService.search("Stenopus", "en", TESTUSER_EMAIL1);
        assertFalse(resultsAsProposer.isEmpty(), "Proposer should see their own pending entry");

        // User 2 (non-proposer) should NOT see the pending entry
        given(userRepository.getByEmail(TESTUSER_EMAIL2)).willReturn(user2);
        given(invertebrateCatalogueRepository.findAll()).willReturn(List.of(pending));

        List<InvertebrateCatalogueSearchResultTo> resultsAsOther =
                invertebrateCatalogueService.search("Stenopus", "en", TESTUSER_EMAIL2);
        assertTrue(resultsAsOther.isEmpty(), "Non-proposer should NOT see pending entry");
    }

    /**
     * US4: search query shorter than 2 characters → empty result.
     */
    @Test
    public void search_queryTooShort_returnsEmpty() {
        List<InvertebrateCatalogueSearchResultTo> results =
                invertebrateCatalogueService.search("L", "en", TESTUSER_EMAIL1);
        assertTrue(results.isEmpty());
    }

    // ---- US5: Propose catalogue entry ----

    /**
     * US5: propose a new entry → saved with PENDING status.
     */
    @Test
    public void proposeEntry_newEntry_createdAsPending() {
        UserEntity user = testUser1();
        InvertebrateCatalogueEntryTo dto = new InvertebrateCatalogueEntryTo();
        dto.setScientificName("Stenopus hispidus");
        dto.setTaxonomicCategory(InvertebrateTaxonomicCategory.CRUSTACEAN);

        InvertebrateCatalogueEntity entity = pendingEntry(user.getId());
        InvertebrateCatalogueEntryTo resultTo = new InvertebrateCatalogueEntryTo();
        resultTo.setId(2L);
        resultTo.setScientificName("Stenopus hispidus");
        resultTo.setStatus(FishCatalogueStatus.PENDING);

        given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(user);
        given(invertebrateCatalogueRepository.findByScientificNameAndStatusIn(any(), any()))
                .willReturn(new ArrayList<>());
        given(invertebrateCatalogueMapper.toEntity(any())).willReturn(entity);
        given(invertebrateCatalogueRepository.saveAndFlush(any())).willReturn(entity);
        given(invertebrateCatalogueMapper.toTo(any())).willReturn(resultTo);

        ResultTo<InvertebrateCatalogueEntryTo> result =
                invertebrateCatalogueService.proposeEntry(dto, TESTUSER_EMAIL1);

        assertNotNull(result);
        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
        assertEquals(FishCatalogueStatus.PENDING, result.getValue().getStatus());
    }

    // ---- US6: Admin approve / reject ----

    /**
     * US6: admin approves a PENDING entry → status becomes PUBLIC.
     */
    @Test
    public void approveEntry_pendingEntry_becomesPublic() {
        InvertebrateCatalogueEntity pending = pendingEntry(2L);
        InvertebrateCatalogueEntity approvedEntity = pendingEntry(2L);
        approvedEntity.setStatus("PUBLIC");

        InvertebrateCatalogueEntryTo approvedTo = new InvertebrateCatalogueEntryTo();
        approvedTo.setId(2L);
        approvedTo.setStatus(FishCatalogueStatus.PUBLIC);

        given(invertebrateCatalogueRepository.findById(2L)).willReturn(Optional.of(pending));
        given(invertebrateCatalogueRepository.save(any())).willReturn(approvedEntity);
        given(invertebrateCatalogueMapper.toTo(any())).willReturn(approvedTo);

        ResultTo<InvertebrateCatalogueEntryTo> result =
                invertebrateCatalogueService.approveEntry(2L, ADMIN_EMAIL);

        assertNotNull(result);
        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
        assertEquals(FishCatalogueStatus.PUBLIC, result.getValue().getStatus());
    }

    /**
     * US6: admin rejects a PENDING entry → status becomes REJECTED.
     */
    @Test
    public void rejectEntry_pendingEntry_becomesRejected() {
        InvertebrateCatalogueEntity pending = pendingEntry(2L);
        InvertebrateCatalogueEntity rejectedEntity = pendingEntry(2L);
        rejectedEntity.setStatus("REJECTED");

        InvertebrateCatalogueEntryTo rejectedTo = new InvertebrateCatalogueEntryTo();
        rejectedTo.setId(2L);
        rejectedTo.setStatus(FishCatalogueStatus.REJECTED);

        given(invertebrateCatalogueRepository.findById(2L)).willReturn(Optional.of(pending));
        given(invertebrateCatalogueRepository.save(any())).willReturn(rejectedEntity);
        given(invertebrateCatalogueMapper.toTo(any())).willReturn(rejectedTo);

        ResultTo<InvertebrateCatalogueEntryTo> result =
                invertebrateCatalogueService.rejectEntry(2L, ADMIN_EMAIL);

        assertNotNull(result);
        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
        assertEquals(FishCatalogueStatus.REJECTED, result.getValue().getStatus());
    }

    /**
     * US6: non-admin calling approveEntry → returns error.
     */
    @Test
    public void approveEntry_nonAdmin_returnsError() {
        ResultTo<InvertebrateCatalogueEntryTo> result =
                invertebrateCatalogueService.approveEntry(2L, TESTUSER_EMAIL1);

        assertNotNull(result);
        assertEquals(Message.CATEGORY.ERROR, result.getMessage().getType());
    }

    /**
     * US6: listPending with admin email returns pending proposals.
     */
    @Test
    public void listPending_asAdmin_returnsPendingEntries() {
        InvertebrateCatalogueEntity pending = pendingEntry(1L);

        InvertebrateCatalogueEntryTo pendingTo = new InvertebrateCatalogueEntryTo();
        pendingTo.setId(2L);
        pendingTo.setStatus(FishCatalogueStatus.PENDING);

        given(invertebrateCatalogueRepository.findByStatusOrderByProposalDateDesc("PENDING"))
                .willReturn(List.of(pending));
        given(invertebrateCatalogueMapper.toTo(eq(pending))).willReturn(pendingTo);

        List<InvertebrateCatalogueEntryTo> pendingList =
                invertebrateCatalogueService.listPending(ADMIN_EMAIL);

        assertNotNull(pendingList);
        assertEquals(1, pendingList.size());
        assertEquals(FishCatalogueStatus.PENDING, pendingList.get(0).getStatus());
    }
}
