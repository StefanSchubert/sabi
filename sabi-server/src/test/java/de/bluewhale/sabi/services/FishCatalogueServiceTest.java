/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 * Part of 002-fish-stock-catalogue — T059 + T071
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.mapper.FishCatalogueMapper;
import de.bluewhale.sabi.model.FishCatalogueEntryTo;
import de.bluewhale.sabi.model.FishCatalogueI18nTo;
import de.bluewhale.sabi.model.FishCatalogueStatus;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.persistence.model.FishCatalogueEntryEntity;
import de.bluewhale.sabi.persistence.model.FishCatalogueI18nEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.FishCatalogueEntryRepository;
import de.bluewhale.sabi.persistence.repositories.FishCatalogueI18nRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
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
import java.util.Optional;

import static de.bluewhale.sabi.util.TestContainerVersions.MARIADB_11_3_2;
import static de.bluewhale.sabi.util.TestDataFactory.TESTUSER_EMAIL1;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

/**
 * Service-layer tests for FishCatalogueService.
 * T059: proposeEntry (FR-015, FR-012). T071: updateEntry (FR-019, SC-009).
 */
@SpringBootTest
@Testcontainers
@Tag("ServiceTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class FishCatalogueServiceTest {

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

    // ---- Helpers ----

    private UserEntity testUser() {
        UserEntity u = testDataFactory.getNewTestUserEntity(testDataFactory.getNewTestUserTo(TESTUSER_EMAIL1));
        u.setId(1L);
        return u;
    }

    private FishCatalogueEntryTo buildEntryTo(String scientificName) {
        FishCatalogueEntryTo to = new FishCatalogueEntryTo();
        to.setScientificName(scientificName);
        to.setStatus(FishCatalogueStatus.PENDING);
        List<FishCatalogueI18nTo> i18n = new ArrayList<>();
        FishCatalogueI18nTo en = new FishCatalogueI18nTo();
        en.setLanguageCode("en");
        en.setCommonName("Clownfish");
        i18n.add(en);
        to.setI18nEntries(i18n);
        return to;
    }

    private FishCatalogueEntryEntity buildEntity(Long id, String scientificName, String status, Long proposerId) {
        FishCatalogueEntryEntity e = new FishCatalogueEntryEntity();
        e.setId(id);
        e.setScientificName(scientificName);
        e.setStatus(status);
        e.setProposerUserId(proposerId);
        e.setProposalDate(LocalDate.now());
        e.setI18nEntries(new ArrayList<>());
        return e;
    }

    // ---- T059: proposeEntry ----

    /** FR-015: proposeEntry with duplicate scientific name -> warning but entry IS saved. */
    @Test
    public void proposeEntry_duplicateScientificName_returnsWarningButProceed() {
        UserEntity user = testUser();
        FishCatalogueEntryTo entryTo = buildEntryTo("Amphiprioninae");
        FishCatalogueEntryEntity savedEntity = buildEntity(99L, "Amphiprioninae", "PENDING", 1L);
        FishCatalogueEntryEntity otherEntity = buildEntity(10L, "Amphiprioninae", "PUBLIC", 2L);

        given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(user);
        given(fishCatalogueMapper.mapTo2Entity(any())).willReturn(savedEntity);
        // Service calls saveAndFlush(), not save()
        given(fishCatalogueEntryRepository.saveAndFlush(any())).willReturn(savedEntity);
        FishCatalogueEntryTo savedTo = buildEntryTo("Amphiprioninae");
        savedTo.setId(99L);
        given(fishCatalogueMapper.mapEntity2To(savedEntity)).willReturn(savedTo);
        given(fishCatalogueEntryRepository.existsByScientificNameAndStatusIn(
                eq("Amphiprioninae"), anyList())).willReturn(true);
        given(fishCatalogueEntryRepository.searchByQueryAndLang(
                eq("Amphiprioninae"), anyString(), anyLong()))
                .willReturn(List.of(otherEntity));
        given(fishCatalogueEntryRepository.findById(99L)).willReturn(Optional.of(savedEntity));

        ResultTo<FishCatalogueEntryTo> result = fishCatalogueService.proposeEntry(entryTo, TESTUSER_EMAIL1);

        assertNotNull(result);
        assertNotNull(result.getValue()); // entry WAS saved
        assertEquals(Message.CATEGORY.WARNING, result.getMessage().getType());
        assertEquals(FishCatalogueMessageCodes.CATALOGUE_DUPLICATE_WARNING, result.getMessage().getCode());
    }

    /** FR-012: proposeEntry where name only exists as REJECTED -> NO warning. */
    @Test
    public void proposeEntry_rejectedDuplicateName_noWarning() {
        UserEntity user = testUser();
        FishCatalogueEntryTo entryTo = buildEntryTo("Amphiprioninae");
        FishCatalogueEntryEntity savedEntity = buildEntity(100L, "Amphiprioninae", "PENDING", 1L);

        given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(user);
        given(fishCatalogueMapper.mapTo2Entity(any())).willReturn(savedEntity);
        given(fishCatalogueEntryRepository.saveAndFlush(any())).willReturn(savedEntity);
        FishCatalogueEntryTo savedTo = buildEntryTo("Amphiprioninae");
        savedTo.setId(100L);
        given(fishCatalogueMapper.mapEntity2To(savedEntity)).willReturn(savedTo);
        // PENDING/PUBLIC check returns false -> only REJECTED exists
        given(fishCatalogueEntryRepository.existsByScientificNameAndStatusIn(
                eq("Amphiprioninae"), anyList())).willReturn(false);
        given(fishCatalogueEntryRepository.findById(100L)).willReturn(Optional.of(savedEntity));

        ResultTo<FishCatalogueEntryTo> result = fishCatalogueService.proposeEntry(entryTo, TESTUSER_EMAIL1);

        assertNotNull(result);
        assertNotNull(result.getValue());
        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
    }

    /** proposeEntry sets status PENDING and proposerId. */
    @Test
    public void proposeEntry_setsStatusPendingAndProposerId() {
        UserEntity user = testUser();
        FishCatalogueEntryTo entryTo = buildEntryTo("Naso lituratus");
        FishCatalogueEntryEntity entityToSave = buildEntity(null, "Naso lituratus", "PENDING", null);

        given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(user);
        given(fishCatalogueMapper.mapTo2Entity(any())).willReturn(entityToSave);
        given(fishCatalogueEntryRepository.saveAndFlush(any())).willAnswer(inv -> {
            FishCatalogueEntryEntity e = inv.getArgument(0);
            e.setId(77L);
            return e;
        });
        FishCatalogueEntryTo savedTo = buildEntryTo("Naso lituratus");
        savedTo.setId(77L);
        given(fishCatalogueMapper.mapEntity2To(any())).willReturn(savedTo);
        given(fishCatalogueEntryRepository.existsByScientificNameAndStatusIn(anyString(), anyList())).willReturn(false);
        given(fishCatalogueEntryRepository.findById(77L)).willReturn(Optional.of(entityToSave));

        ResultTo<FishCatalogueEntryTo> result = fishCatalogueService.proposeEntry(entryTo, TESTUSER_EMAIL1);

        assertNotNull(result);
        assertEquals("PENDING", entityToSave.getStatus());
        assertEquals(1L, entityToSave.getProposerUserId());
    }

    // ---- T071: updateEntry ----

    /** FR-019: updateEntry on REJECTED -> CATALOGUE_REJECTED_READ_ONLY error. */
    @Test
    public void updateEntry_rejectedStatus_throwsCatalogueRejectedReadOnly() {
        UserEntity user = testUser();
        FishCatalogueEntryTo entryTo = buildEntryTo("Amphiprioninae");
        entryTo.setId(5L);
        FishCatalogueEntryEntity entity = buildEntity(5L, "Amphiprioninae", "REJECTED", 1L);

        given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(user);
        given(fishCatalogueEntryRepository.findById(5L)).willReturn(Optional.of(entity));

        ResultTo<FishCatalogueEntryTo> result = fishCatalogueService.updateEntry(entryTo, TESTUSER_EMAIL1);

        assertNotNull(result);
        assertEquals(Message.CATEGORY.ERROR, result.getMessage().getType());
        // Service returns FishCatalogueMessageCodes, not FishCatalogueExceptionCodes
        assertEquals(FishCatalogueMessageCodes.CATALOGUE_REJECTED_READ_ONLY, result.getMessage().getCode());
    }

    /** PENDING by creator -> succeeds. */
    @Test
    public void updateEntry_pendingByCreator_succeeds() {
        UserEntity user = testUser();
        FishCatalogueEntryTo entryTo = buildEntryTo("Amphiprioninae");
        entryTo.setId(6L);
        FishCatalogueEntryEntity entity = buildEntity(6L, "Amphiprioninae", "PENDING", 1L);

        given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(user);
        given(fishCatalogueEntryRepository.findById(6L)).willReturn(Optional.of(entity));
        given(fishCatalogueEntryRepository.save(any())).willReturn(entity);
        // mergeI18nEntries calls mapper for new i18n entries (entity has empty list, entryTo has 1 EN entry)
        FishCatalogueI18nEntity i18nEntity = new FishCatalogueI18nEntity();
        i18nEntity.setCatalogueId(6L);
        given(fishCatalogueMapper.mapI18nTo2Entity(any())).willReturn(i18nEntity);
        FishCatalogueEntryTo savedTo = buildEntryTo("Amphiprioninae");
        savedTo.setId(6L);
        given(fishCatalogueMapper.mapEntity2To(any())).willReturn(savedTo);
        given(fishCatalogueEntryRepository.existsByScientificNameAndStatusIn(anyString(), anyList())).willReturn(false);

        ResultTo<FishCatalogueEntryTo> result = fishCatalogueService.updateEntry(entryTo, TESTUSER_EMAIL1);

        assertNotNull(result);
        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
        assertEquals(FishCatalogueMessageCodes.CATALOGUE_ENTRY_UPDATED, result.getMessage().getCode());
    }

    /** PUBLIC by non-creator non-admin -> CATALOGUE_ENTRY_NOT_YOURS error. */
    @Test
    public void updateEntry_publicByNonCreatorNonAdmin_throwsNotYours() {
        UserEntity user = testUser(); // id=1
        FishCatalogueEntryTo entryTo = buildEntryTo("Paracanthurus hepatus");
        entryTo.setId(7L);
        // proposer is user 99, not user 1
        FishCatalogueEntryEntity entity = buildEntity(7L, "Paracanthurus hepatus", "PUBLIC", 99L);

        given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(user);
        given(fishCatalogueEntryRepository.findById(7L)).willReturn(Optional.of(entity));

        ResultTo<FishCatalogueEntryTo> result = fishCatalogueService.updateEntry(entryTo, TESTUSER_EMAIL1);

        assertNotNull(result);
        assertEquals(Message.CATEGORY.ERROR, result.getMessage().getType());
        // Service returns FishCatalogueMessageCodes, not FishCatalogueExceptionCodes
        assertEquals(FishCatalogueMessageCodes.CATALOGUE_ENTRY_NOT_YOURS, result.getMessage().getCode());
    }

    /** FR-015: updateEntry with changed scientificName -> re-triggers duplicate warning. */
    @Test
    public void updateEntry_changeScientificName_retriggeresDuplicateWarning() {
        UserEntity user = testUser();
        FishCatalogueEntryTo entryTo = buildEntryTo("Amphiprioninae");
        entryTo.setId(8L);
        FishCatalogueEntryEntity entity = buildEntity(8L, "OldName", "PENDING", 1L);
        FishCatalogueEntryEntity otherEntity = buildEntity(3L, "Amphiprioninae", "PUBLIC", 2L);

        given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(user);
        given(fishCatalogueEntryRepository.findById(8L)).willReturn(Optional.of(entity));
        given(fishCatalogueEntryRepository.save(any())).willReturn(entity);
        // mergeI18nEntries: entity has empty i18n list, entryTo has EN entry -> mapper is called
        FishCatalogueI18nEntity i18nEntity = new FishCatalogueI18nEntity();
        i18nEntity.setCatalogueId(8L);
        given(fishCatalogueMapper.mapI18nTo2Entity(any())).willReturn(i18nEntity);
        FishCatalogueEntryTo savedTo = buildEntryTo("Amphiprioninae");
        savedTo.setId(8L);
        given(fishCatalogueMapper.mapEntity2To(any())).willReturn(savedTo);
        given(fishCatalogueEntryRepository.existsByScientificNameAndStatusIn(
                eq("Amphiprioninae"), anyList())).willReturn(true);
        given(fishCatalogueEntryRepository.searchByQueryAndLang(
                eq("Amphiprioninae"), anyString(), anyLong()))
                .willReturn(List.of(otherEntity));

        ResultTo<FishCatalogueEntryTo> result = fishCatalogueService.updateEntry(entryTo, TESTUSER_EMAIL1);

        assertNotNull(result);
        assertEquals(Message.CATEGORY.WARNING, result.getMessage().getType());
        assertEquals(FishCatalogueMessageCodes.CATALOGUE_DUPLICATE_WARNING, result.getMessage().getCode());
    }
}
