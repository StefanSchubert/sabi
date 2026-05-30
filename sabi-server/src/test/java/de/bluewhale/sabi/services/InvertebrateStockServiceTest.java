/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 * Part of 006-invertebrate-tracking — T095
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.*;
import de.bluewhale.sabi.persistence.model.*;
import de.bluewhale.sabi.persistence.repositories.*;
import de.bluewhale.sabi.mapper.InvertebrateStockMapper;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static de.bluewhale.sabi.util.TestContainerVersions.MARIADB_11_3_2;
import static de.bluewhale.sabi.util.TestDataFactory.TESTUSER_EMAIL1;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

/**
 * Service-layer tests for InvertebrateStockService using Mockito.
 * Covers US1 (basic CRUD), US2 (functional classifications), US3 (departure).
 * Part of 006-invertebrate-tracking.
 */
@SpringBootTest
@Testcontainers
@Tag("ServiceTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class InvertebrateStockServiceTest {

    @Container
    @ServiceConnection
    static MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>(MARIADB_11_3_2);

    static TestDataFactory testDataFactory = TestDataFactory.getInstance();

    @Autowired
    private InvertebrateStockService invertebrateStockService;

    @MockitoBean
    private TankInvertebrateStockRepository invertebrateStockRepository;

    @MockitoBean
    private InvertebrateWaterSensitivityRepository waterSensitivityRepository;

    @MockitoBean
    private InvertebratePhotoRepository invertebratePhotoRepository;

    @MockitoBean
    private AquariumRepository aquariumRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private InvertebrateStockMapper invertebrateStockMapper;

    // All four PhotoStorageService beans must be mocked to prevent ambiguity
    @MockitoBean(name = "invertebratePhotoStorage")
    private PhotoStorageService invertebratePhotoStorage;

    @MockitoBean(name = "fishPhotoStorage")
    private PhotoStorageService fishPhotoStorage;

    @MockitoBean(name = "aquariumPhotoStorage")
    private PhotoStorageService aquariumPhotoStorage;

    @MockitoBean(name = "coralPhotoStorage")
    private PhotoStorageService coralPhotoStorage;

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
        aq.setWaterType(WaterType.SEA_WATER);
        return aq;
    }

    private TankInvertebrateStockEntity testEntity() {
        TankInvertebrateStockEntity e = new TankInvertebrateStockEntity();
        e.setId(10L);
        e.setSpeciesName("Fire Shrimp");
        e.setAquariumId(1L);
        e.setAddedOn(LocalDate.now().minusDays(20));
        e.setTaxonomicCategory("CRUSTACEAN");
        return e;
    }

    private InvertebrateStockEntryTo testEntryTo() {
        InvertebrateStockEntryTo to = new InvertebrateStockEntryTo();
        to.setId(10L);
        to.setAquariumId(1L);
        to.setSpeciesName("Fire Shrimp");
        to.setTaxonomicCategory(InvertebrateTaxonomicCategory.CRUSTACEAN);
        to.setAddedOn(LocalDate.now().minusDays(20));
        return to;
    }

    // ---- US1: Basic CRUD tests ----

    /**
     * US1: create an invertebrate entry → persisted and returned.
     */
    @Test
    public void create_validEntry_succeeds() {
        UserEntity user = testUser();
        AquariumEntity aquarium = testAquarium(user);
        InvertebrateStockEntryTo entry = testEntryTo();
        entry.setId(null);

        TankInvertebrateStockEntity savedEntity = testEntity();

        given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(user);
        given(aquariumRepository.findById(1L)).willReturn(Optional.of(aquarium));
        given(invertebrateStockMapper.toEntity(any())).willReturn(savedEntity);
        given(invertebrateStockRepository.saveAndFlush(any())).willReturn(savedEntity);
        given(invertebrateStockMapper.toTo(any())).willReturn(testEntryTo());
        given(waterSensitivityRepository.saveAll(any())).willReturn(Collections.emptyList());
        given(invertebratePhotoRepository.findByInvertebrateStockId(anyLong())).willReturn(Optional.empty());

        ResultTo<InvertebrateStockEntryTo> result = invertebrateStockService.create(entry, TESTUSER_EMAIL1);

        assertNotNull(result);
        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
        assertNotNull(result.getValue());
        assertEquals("Fire Shrimp", result.getValue().getSpeciesName());
    }

    /**
     * US1: create with unknown user → error returned.
     */
    @Test
    public void create_unknownUser_returnsError() {
        given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(null);

        InvertebrateStockEntryTo entry = testEntryTo();
        ResultTo<InvertebrateStockEntryTo> result = invertebrateStockService.create(entry, TESTUSER_EMAIL1);

        assertNotNull(result);
        assertEquals(Message.CATEGORY.ERROR, result.getMessage().getType());
    }

    /**
     * US1: listForAquarium returns entries for correct user.
     */
    @Test
    public void listForAquarium_returnsOwnedEntries() {
        UserEntity user = testUser();
        TankInvertebrateStockEntity entity = testEntity();

        given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(user);
        given(invertebrateStockRepository.findAllByAquariumIdAndUserId(1L, 1L))
                .willReturn(List.of(entity));
        given(invertebrateStockMapper.toTo(entity)).willReturn(testEntryTo());
        given(invertebratePhotoRepository.findByInvertebrateStockId(10L)).willReturn(Optional.empty());

        List<InvertebrateStockEntryTo> result = invertebrateStockService.listForAquarium(1L, TESTUSER_EMAIL1);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Fire Shrimp", result.get(0).getSpeciesName());
    }

    /**
     * US1: delete with departure record present → INVERT_HAS_DEPARTURE_RECORD error.
     */
    @Test
    public void delete_withDepartureRecord_returnsError() {
        UserEntity user = testUser();
        TankInvertebrateStockEntity entityWithDeparture = testEntity();
        entityWithDeparture.setDepartedOn(LocalDate.now().minusDays(5));

        given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(user);
        given(invertebrateStockRepository.findByIdAndUserId(10L, 1L))
                .willReturn(Optional.of(entityWithDeparture));

        ResultTo<InvertebrateStockEntryTo> result = invertebrateStockService.delete(10L, TESTUSER_EMAIL1);

        assertNotNull(result);
        assertEquals(Message.CATEGORY.ERROR, result.getMessage().getType());
        assertEquals(InvertebrateStockMessageCodes.INVERT_HAS_DEPARTURE_RECORD, result.getMessage().getCode());
    }

    /**
     * US1: delete without departure record → succeeds.
     */
    @Test
    public void delete_withoutDepartureRecord_succeeds() {
        UserEntity user = testUser();
        TankInvertebrateStockEntity entity = testEntity(); // no departedOn

        given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(user);
        given(invertebrateStockRepository.findByIdAndUserId(10L, 1L))
                .willReturn(Optional.of(entity));

        ResultTo<InvertebrateStockEntryTo> result = invertebrateStockService.delete(10L, TESTUSER_EMAIL1);

        assertNotNull(result);
        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
    }

    /**
     * US1: ownership check — wrong user cannot access entry.
     */
    @Test
    public void delete_nonOwner_returnsOwnershipError() {
        UserEntity user = testUser();

        given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(user);
        given(invertebrateStockRepository.findByIdAndUserId(10L, 1L))
                .willReturn(Optional.empty()); // not found for this user = ownership rejected

        ResultTo<InvertebrateStockEntryTo> result = invertebrateStockService.delete(10L, TESTUSER_EMAIL1);

        assertNotNull(result);
        assertEquals(Message.CATEGORY.ERROR, result.getMessage().getType());
        assertEquals(InvertebrateStockMessageCodes.INVERT_NOT_OWNER, result.getMessage().getCode());
    }

    // ---- US2: Functional classifications ----

    /**
     * US2: create with all four classification dimensions → entity fields populated.
     */
    @Test
    public void create_withFullClassifications_persistsAllFields() {
        UserEntity user = testUser();
        AquariumEntity aquarium = testAquarium(user);
        InvertebrateStockEntryTo entry = testEntryTo();
        entry.setMobility(InvertebrateMobility.MOBILE);
        entry.setEcologicalRole(InvertebrateEcologicalRole.CLEANUP_CREW);
        entry.setActivityPattern(InvertebrateActivityPattern.NOCTURNAL);
        entry.setWaterSensitivityUnitIds(Arrays.asList(1, 3));

        TankInvertebrateStockEntity savedEntity = testEntity();
        savedEntity.setMobility("MOBILE");
        savedEntity.setEcologicalRole("CLEANUP_CREW");
        savedEntity.setActivityPattern("NOCTURNAL");

        InvertebrateStockEntryTo resultTo = testEntryTo();
        resultTo.setMobility(InvertebrateMobility.MOBILE);
        resultTo.setEcologicalRole(InvertebrateEcologicalRole.CLEANUP_CREW);
        resultTo.setActivityPattern(InvertebrateActivityPattern.NOCTURNAL);
        resultTo.setWaterSensitivityUnitIds(Arrays.asList(1, 3));

        given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(user);
        given(aquariumRepository.findById(1L)).willReturn(Optional.of(aquarium));
        given(invertebrateStockMapper.toEntity(any())).willReturn(savedEntity);
        given(invertebrateStockRepository.saveAndFlush(any())).willReturn(savedEntity);
        given(invertebrateStockMapper.toTo(any())).willReturn(resultTo);
        given(waterSensitivityRepository.saveAll(any())).willReturn(Collections.emptyList());
        given(invertebratePhotoRepository.findByInvertebrateStockId(anyLong())).willReturn(Optional.empty());

        ResultTo<InvertebrateStockEntryTo> result = invertebrateStockService.create(entry, TESTUSER_EMAIL1);

        assertNotNull(result);
        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
        assertEquals(InvertebrateMobility.MOBILE, result.getValue().getMobility());
        assertEquals(InvertebrateEcologicalRole.CLEANUP_CREW, result.getValue().getEcologicalRole());
        assertEquals(InvertebrateActivityPattern.NOCTURNAL, result.getValue().getActivityPattern());
        assertEquals(2, result.getValue().getWaterSensitivityUnitIds().size());
    }

    // ---- US3: Departure recording ----

    /**
     * US3: recordDeparture with valid date → succeeds.
     */
    @Test
    public void recordDeparture_validDate_succeeds() {
        UserEntity user = testUser();
        TankInvertebrateStockEntity entity = testEntity();
        // entry was added 20 days ago; departure is today (valid)

        InvertebrateDepartureRecordTo record = new InvertebrateDepartureRecordTo();
        record.setDepartedOn(LocalDate.now());
        record.setDepartureReason(CoralDepartureReason.DIED);

        InvertebrateStockEntryTo resultTo = testEntryTo();
        resultTo.setDepartedOn(LocalDate.now());

        given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(user);
        given(invertebrateStockRepository.findByIdAndUserId(10L, 1L))
                .willReturn(Optional.of(entity));
        given(invertebrateStockRepository.saveAndFlush(any())).willReturn(entity);
        given(invertebrateStockMapper.toTo(any())).willReturn(resultTo);
        given(invertebratePhotoRepository.findByInvertebrateStockId(anyLong())).willReturn(Optional.empty());

        ResultTo<InvertebrateStockEntryTo> result =
                invertebrateStockService.recordDeparture(10L, record, TESTUSER_EMAIL1);

        assertNotNull(result);
        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
        assertNotNull(result.getValue().getDepartedOn());
    }

    /**
     * US3: recordDeparture with departure date BEFORE entry date → DEPARTURE_DATE_BEFORE_ENTRY error.
     */
    @Test
    public void recordDeparture_dateBeforeEntry_returnsError() {
        UserEntity user = testUser();
        TankInvertebrateStockEntity entity = testEntity();
        // entity.addedOn = 20 days ago; departure is 25 days ago = before entry

        InvertebrateDepartureRecordTo record = new InvertebrateDepartureRecordTo();
        record.setDepartedOn(LocalDate.now().minusDays(25)); // before addedOn (20 days ago)
        record.setDepartureReason(CoralDepartureReason.DIED);

        given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(user);
        given(invertebrateStockRepository.findByIdAndUserId(10L, 1L))
                .willReturn(Optional.of(entity));

        ResultTo<InvertebrateStockEntryTo> result =
                invertebrateStockService.recordDeparture(10L, record, TESTUSER_EMAIL1);

        assertNotNull(result);
        assertEquals(Message.CATEGORY.ERROR, result.getMessage().getType());
        assertEquals(InvertebrateStockMessageCodes.DEPARTURE_DATE_BEFORE_ENTRY,
                result.getMessage().getCode());
    }
}
