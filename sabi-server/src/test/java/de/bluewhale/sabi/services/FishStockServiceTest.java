/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 * Part of 002-fish-stock-catalogue — T041
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.DepartureReason;
import de.bluewhale.sabi.model.FishDepartureRecordTo;
import de.bluewhale.sabi.model.FishStockEntryTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.FishCatalogueEntryEntity;
import de.bluewhale.sabi.persistence.model.FishCatalogueI18nEntity;
import de.bluewhale.sabi.persistence.model.TankFishStockEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.AquariumRepository;
import de.bluewhale.sabi.persistence.repositories.FishCatalogueEntryRepository;
import de.bluewhale.sabi.persistence.repositories.FishPhotoRepository;
import de.bluewhale.sabi.persistence.repositories.TankFishStockRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import de.bluewhale.sabi.mapper.FishCatalogueMapper;
import de.bluewhale.sabi.mapper.FishStockMapper;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

/**
 * Service-layer tests for FishStockService using Mockito.
 * Covers FR-006, FR-009, FR-024 from spec.md.
 */
@SpringBootTest
@Testcontainers
@Tag("ServiceTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class FishStockServiceTest {

    @Container
    @ServiceConnection
    static MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>(MARIADB_11_3_2);

    static TestDataFactory testDataFactory = TestDataFactory.getInstance();

    @Autowired
    private FishStockService fishStockService;

    @MockitoBean
    private TankFishStockRepository tankFishStockRepository;

    @MockitoBean
    private AquariumRepository aquariumRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private FishCatalogueEntryRepository fishCatalogueEntryRepository;

    @MockitoBean
    private FishPhotoRepository fishPhotoRepository;

    @MockitoBean
    private PhotoStorageService photoStorageService;

    @MockitoBean
    private FishStockMapper fishStockMapper;

    @MockitoBean
    private FishCatalogueMapper fishCatalogueMapper;

    // ---- Helpers ----

    private UserEntity testUser() {
        UserEntity u = testDataFactory.getNewTestUserEntity(testDataFactory.getNewTestUserTo(TESTUSER_EMAIL1));
        u.setId(1L);
        return u;
    }

    private AquariumEntity testAquarium(UserEntity owner) {
        AquariumEntity aq = new AquariumEntity();
        aq.setId(1L);
        aq.setUser(owner);
        return aq;
    }

    private TankFishStockEntity testFishEntity() {
        TankFishStockEntity entity = new TankFishStockEntity();
        entity.setId(10L);
        entity.setCommonName("Clownfish");
        entity.setAquariumId(1L);
        entity.setAddedOn(LocalDate.now().minusDays(30));
        return entity;
    }

    private FishStockEntryTo testFishTo() {
        FishStockEntryTo to = new FishStockEntryTo();
        to.setId(10L);
        to.setAquariumId(1L);
        to.setCommonName("Clownfish");
        to.setAddedOn(LocalDate.now().minusDays(30));
        return to;
    }

    // ---- Test Cases ----

    /**
     * FR-009: addFishToTank with catalogueId → scientificName is snapshot-copied.
     */
    @Test
    public void addFishToTank_withCatalogueLink_copiesScientificNameSnapshot() {
        UserEntity user = testUser();
        AquariumEntity aquarium = testAquarium(user);
        FishStockEntryTo entry = testFishTo();
        entry.setFishCatalogueId(5L);

        TankFishStockEntity entityToSave = testFishEntity();

        // Catalogue entry with EN i18n
        FishCatalogueEntryEntity catEntry = new FishCatalogueEntryEntity();
        catEntry.setId(5L);
        catEntry.setScientificName("Amphiprioninae");
        FishCatalogueI18nEntity i18n = new FishCatalogueI18nEntity();
        i18n.setLanguageCode("en");
        i18n.setReferenceUrl("https://meerwasserwiki.de/clownfish");
        List<FishCatalogueI18nEntity> i18nList = new ArrayList<>();
        i18nList.add(i18n);
        catEntry.setI18nEntries(i18nList);

        given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(user);
        given(aquariumRepository.findById(1L)).willReturn(Optional.of(aquarium));
        given(fishStockMapper.mapTo2Entity(any())).willReturn(entityToSave);
        given(fishCatalogueEntryRepository.findById(5L)).willReturn(Optional.of(catEntry));
        given(tankFishStockRepository.save(any())).willReturn(entityToSave);
        FishStockEntryTo savedTo = testFishTo();
        savedTo.setScientificName("Amphiprioninae");
        given(fishStockMapper.mapEntity2To(any())).willReturn(savedTo);

        ResultTo<FishStockEntryTo> result = fishStockService.addFishToTank(entry, TESTUSER_EMAIL1);

        assertNotNull(result);
        // scientificName was snapshot-copied from catalogue
        assertEquals("Amphiprioninae", entityToSave.getScientificName());
        assertEquals("https://meerwasserwiki.de/clownfish", entityToSave.getExternalRefUrl());
    }

    /**
     * addFishToTank without catalogueLink succeeds normally.
     */
    @Test
    public void addFishToTank_withoutCatalogueLink_succeeds() {
        UserEntity user = testUser();
        AquariumEntity aquarium = testAquarium(user);
        FishStockEntryTo entry = testFishTo();
        // no catalogue id
        TankFishStockEntity entityToSave = testFishEntity();

        given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(user);
        given(aquariumRepository.findById(1L)).willReturn(Optional.of(aquarium));
        given(fishStockMapper.mapTo2Entity(any())).willReturn(entityToSave);
        given(tankFishStockRepository.save(any())).willReturn(entityToSave);
        given(fishStockMapper.mapEntity2To(any())).willReturn(testFishTo());

        ResultTo<FishStockEntryTo> result = fishStockService.addFishToTank(entry, TESTUSER_EMAIL1);

        assertNotNull(result);
        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
    }

    /**
     * FR-024: deletePhysically when fish has departure record → returns FISH_HAS_DEPARTURE_RECORD error.
     */
    @Test
    public void deletePhysically_withDepartureRecord_returnsDepartureBlockedError() {
        UserEntity user = testUser();
        TankFishStockEntity entity = testFishEntity();

        given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(user);
        given(tankFishStockRepository.findByIdAndUserId(10L, 1L)).willReturn(Optional.of(entity));
        given(tankFishStockRepository.existsByIdAndExodusOnIsNotNull(10L)).willReturn(true);
        given(fishStockMapper.mapEntity2To(entity)).willReturn(testFishTo());

        ResultTo<FishStockEntryTo> result = fishStockService.deletePhysically(10L, TESTUSER_EMAIL1);

        assertNotNull(result);
        assertEquals(Message.CATEGORY.ERROR, result.getMessage().getType());
        assertEquals(FishStockMessageCodes.FISH_HAS_DEPARTURE_RECORD, result.getMessage().getCode());
    }

    /**
     * deletePhysically without departure record → succeeds.
     */
    @Test
    public void deletePhysically_withoutDepartureRecord_succeeds() {
        UserEntity user = testUser();
        TankFishStockEntity entity = testFishEntity();

        given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(user);
        given(tankFishStockRepository.findByIdAndUserId(10L, 1L)).willReturn(Optional.of(entity));
        given(tankFishStockRepository.existsByIdAndExodusOnIsNotNull(10L)).willReturn(false);
        given(fishPhotoRepository.findByFishId(10L)).willReturn(Optional.empty());

        ResultTo<FishStockEntryTo> result = fishStockService.deletePhysically(10L, TESTUSER_EMAIL1);

        assertNotNull(result);
        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
        assertEquals(FishStockMessageCodes.FISH_DELETED, result.getMessage().getCode());
    }

    /**
     * FR-006: recordDeparture with departureDate before addedOn → returns error, no save.
     */
    @Test
    public void recordDeparture_validatesDateNotBeforeEntryDate() {
        UserEntity user = testUser();
        TankFishStockEntity entity = testFishEntity();
        entity.setAddedOn(LocalDate.now()); // added today

        FishDepartureRecordTo record = new FishDepartureRecordTo();
        record.setDepartureDate(LocalDate.now().minusDays(5)); // before addedOn!
        record.setDepartureReason(DepartureReason.DECEASED);

        given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(user);
        given(tankFishStockRepository.findByIdAndUserId(10L, 1L)).willReturn(Optional.of(entity));
        given(fishStockMapper.mapEntity2To(entity)).willReturn(testFishTo());

        ResultTo<FishStockEntryTo> result = fishStockService.recordDeparture(10L, record, TESTUSER_EMAIL1);

        assertNotNull(result);
        assertEquals(Message.CATEGORY.ERROR, result.getMessage().getType());
    }

    /**
     * FR-006: recordDeparture with valid date → sets exodusOn and departureReason.
     */
    @Test
    public void recordDeparture_validDate_setsExodusOnAndReason() {
        UserEntity user = testUser();
        TankFishStockEntity entity = testFishEntity();
        entity.setAddedOn(LocalDate.now().minusDays(10));

        FishDepartureRecordTo record = new FishDepartureRecordTo();
        record.setDepartureDate(LocalDate.now());
        record.setDepartureReason(DepartureReason.DECEASED);

        FishStockEntryTo savedTo = testFishTo();
        savedTo.setExodusOn(LocalDate.now());
        savedTo.setDepartureReason(DepartureReason.DECEASED);

        given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(user);
        given(tankFishStockRepository.findByIdAndUserId(10L, 1L)).willReturn(Optional.of(entity));
        given(tankFishStockRepository.save(any())).willReturn(entity);
        given(fishStockMapper.mapEntity2To(entity)).willReturn(savedTo);

        ResultTo<FishStockEntryTo> result = fishStockService.recordDeparture(10L, record, TESTUSER_EMAIL1);

        assertNotNull(result);
        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
        assertEquals(FishStockMessageCodes.FISH_DEPARTURE_RECORDED, result.getMessage().getCode());
    }
}

