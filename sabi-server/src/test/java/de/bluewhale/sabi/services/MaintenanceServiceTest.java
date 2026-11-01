/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.mapper.MaintenanceMapperImpl;
import de.bluewhale.sabi.model.ActivatedCarbonTo;
import de.bluewhale.sabi.model.CarbonMode;
import de.bluewhale.sabi.model.MaintenanceProfileTo;
import de.bluewhale.sabi.model.MaintenanceTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.WaterChangeIntervalTo;
import de.bluewhale.sabi.model.WaterChangeTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.TankMaintenanceProfileEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.model.WaterChangeEntity;
import de.bluewhale.sabi.persistence.repositories.AquariumRepository;
import de.bluewhale.sabi.persistence.repositories.TankMaintenanceProfileRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import de.bluewhale.sabi.persistence.repositories.WaterChangeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the ownership-safe maintenance operations (US1/US3).
 * Uses the real MapStruct-generated mapper so merge semantics are verified against entity state.
 */
@ExtendWith(MockitoExtension.class)
@Tag("ServiceTest")
class MaintenanceServiceTest {

    private static final String USER_EMAIL = "owner.maintenance@test.invalid";
    private static final Long AQUARIUM_ID = 10L;

    @Mock
    TankMaintenanceProfileRepository profileRepository;

    @Mock
    WaterChangeRepository waterChangeRepository;

    @Mock
    AquariumRepository aquariumRepository;

    @Mock
    UserRepository userRepository;

    MaintenanceServiceImpl maintenanceService;

    @BeforeEach
    void setUp() {
        maintenanceService = new MaintenanceServiceImpl(profileRepository, waterChangeRepository,
                aquariumRepository, userRepository, new MaintenanceMapperImpl());
    }

    // ---- ownership (403 on every operation, including reads) ----

    @Test
    void getMaintenanceForTank_forForeignAquarium_returnsErrorAndNeverQueriesRecords() {
        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(null);

        ResultTo<MaintenanceTo> result = maintenanceService.getMaintenanceForTank(AQUARIUM_ID, USER_EMAIL);

        assertEquals(Message.CATEGORY.ERROR, result.getMessage().getType());
        assertNull(result.getValue());
        verify(waterChangeRepository, never()).findByAquariumIdOrderByChangedOnDesc(any());
    }

    @Test
    void createWaterChange_forForeignAquarium_returnsErrorAndNeverSaves() {
        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(null);

        ResultTo<WaterChangeTo> result = maintenanceService.createWaterChange(AQUARIUM_ID, validWaterChange(), USER_EMAIL);

        assertEquals(Message.CATEGORY.ERROR, result.getMessage().getType());
        verify(waterChangeRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateWaterChangeInterval_forForeignAquarium_returnsErrorAndNeverSaves() {
        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(null);

        ResultTo<MaintenanceProfileTo> result = maintenanceService.updateWaterChangeInterval(AQUARIUM_ID, interval(21, 25), USER_EMAIL);

        assertEquals(Message.CATEGORY.ERROR, result.getMessage().getType());
        verify(profileRepository, never()).save(any());
    }

    @Test
    void updateActivatedCarbon_forForeignAquarium_returnsErrorAndNeverSaves() {
        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(null);

        ResultTo<MaintenanceProfileTo> result = maintenanceService.updateActivatedCarbon(AQUARIUM_ID, carbon(true, CarbonMode.INTERVAL, 28), USER_EMAIL);

        assertEquals(Message.CATEGORY.ERROR, result.getMessage().getType());
        verify(profileRepository, never()).save(any());
    }

    @Test
    void updateWaterChange_recordNotInThisTank_returnsErrorAndNeverSaves() {
        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(aquarium());
        given(waterChangeRepository.findByIdAndAquariumId(77L, AQUARIUM_ID)).willReturn(Optional.empty());

        ResultTo<WaterChangeTo> result = maintenanceService.updateWaterChange(AQUARIUM_ID, 77L, validWaterChange(), USER_EMAIL);

        assertEquals(Message.CATEGORY.ERROR, result.getMessage().getType());
        verify(waterChangeRepository, never()).save(any());
    }

    @Test
    void deleteWaterChange_recordNotInThisTank_returnsErrorAndNeverDeletes() {
        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(aquarium());
        given(waterChangeRepository.findByIdAndAquariumId(77L, AQUARIUM_ID)).willReturn(Optional.empty());

        ResultTo<WaterChangeTo> result = maintenanceService.deleteWaterChange(AQUARIUM_ID, 77L, USER_EMAIL);

        assertEquals(Message.CATEGORY.ERROR, result.getMessage().getType());
        verify(waterChangeRepository, never()).delete(any());
    }

    // ---- read: profile + records, defaults when no profile row ----

    @Test
    void getMaintenanceForTank_asOwner_returnsProfileAndWaterChanges() {
        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(aquarium());
        given(profileRepository.findByAquariumId(AQUARIUM_ID))
                .willReturn(Optional.of(profile(21, 25, true, CarbonMode.INTERVAL, 28)));
        given(waterChangeRepository.findByAquariumIdOrderByChangedOnDesc(AQUARIUM_ID)).willReturn(List.of(
                waterChange(1L, LocalDate.of(2026, 10, 20), 25, "partial change"),
                waterChange(2L, LocalDate.of(2026, 10, 3), 50, null)));

        ResultTo<MaintenanceTo> result = maintenanceService.getMaintenanceForTank(AQUARIUM_ID, USER_EMAIL);

        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
        MaintenanceTo to = result.getValue();
        assertEquals(21, to.getProfile().getRegularIntervalDays());
        assertEquals(25, to.getProfile().getRegularPercentage());
        assertTrue(to.getProfile().getCarbonEnabled());
        assertEquals(CarbonMode.INTERVAL, to.getProfile().getCarbonMode());
        assertEquals(28, to.getProfile().getCarbonReplacementIntervalDays());
        assertEquals(2, to.getWaterChanges().size());
        assertEquals(LocalDate.of(2026, 10, 20), to.getWaterChanges().get(0).getChangedOn());
        assertEquals("partial change", to.getWaterChanges().get(0).getNote());
    }

    @Test
    void getMaintenanceForTank_withoutProfileRow_returnsDefaults() {
        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(aquarium());
        given(profileRepository.findByAquariumId(AQUARIUM_ID)).willReturn(Optional.empty());
        given(waterChangeRepository.findByAquariumIdOrderByChangedOnDesc(AQUARIUM_ID)).willReturn(List.of());

        ResultTo<MaintenanceTo> result = maintenanceService.getMaintenanceForTank(AQUARIUM_ID, USER_EMAIL);

        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
        MaintenanceProfileTo profile = result.getValue().getProfile();
        assertNull(profile.getRegularIntervalDays());
        assertNull(profile.getRegularPercentage());
        assertFalse(profile.getCarbonEnabled());
        assertNull(profile.getCarbonMode());
        assertNull(profile.getCarbonReplacementIntervalDays());
        assertTrue(result.getValue().getWaterChanges().isEmpty());
    }

    // ---- regular interval: upsert / clear / lazy profile creation ----

    @Test
    void updateWaterChangeInterval_upsertsBothFields() {
        TankMaintenanceProfileEntity existing = profile(14, 10, false, null, null);
        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(aquarium());
        given(profileRepository.findByAquariumId(AQUARIUM_ID)).willReturn(Optional.of(existing));
        given(profileRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        ResultTo<MaintenanceProfileTo> result = maintenanceService.updateWaterChangeInterval(AQUARIUM_ID, interval(21, 25), USER_EMAIL);

        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
        ArgumentCaptor<TankMaintenanceProfileEntity> captor = ArgumentCaptor.forClass(TankMaintenanceProfileEntity.class);
        verify(profileRepository).save(captor.capture());
        assertEquals(21, captor.getValue().getRegularIntervalDays());
        assertEquals(25, captor.getValue().getRegularPercentage());
        // untouched fields stay as they were
        assertFalse(captor.getValue().getCarbonEnabled());
    }

    @Test
    void updateWaterChangeInterval_clearsBothFieldsWhenBothNull() {
        TankMaintenanceProfileEntity existing = profile(21, 25, false, null, null);
        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(aquarium());
        given(profileRepository.findByAquariumId(AQUARIUM_ID)).willReturn(Optional.of(existing));
        given(profileRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        ResultTo<MaintenanceProfileTo> result = maintenanceService.updateWaterChangeInterval(AQUARIUM_ID, interval(null, null), USER_EMAIL);

        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
        ArgumentCaptor<TankMaintenanceProfileEntity> captor = ArgumentCaptor.forClass(TankMaintenanceProfileEntity.class);
        verify(profileRepository).save(captor.capture());
        assertNull(captor.getValue().getRegularIntervalDays());
        assertNull(captor.getValue().getRegularPercentage());
    }

    @Test
    void updateWaterChangeInterval_createsProfileRowWhenMissing() {
        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(aquarium());
        given(profileRepository.findByAquariumId(AQUARIUM_ID)).willReturn(Optional.empty());
        given(profileRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        ResultTo<MaintenanceProfileTo> result = maintenanceService.updateWaterChangeInterval(AQUARIUM_ID, interval(21, 25), USER_EMAIL);

        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
        ArgumentCaptor<TankMaintenanceProfileEntity> captor = ArgumentCaptor.forClass(TankMaintenanceProfileEntity.class);
        verify(profileRepository).save(captor.capture());
        assertEquals(AQUARIUM_ID, captor.getValue().getAquariumId());
        assertEquals(21, captor.getValue().getRegularIntervalDays());
        assertEquals(25, captor.getValue().getRegularPercentage());
    }

    // ---- activated carbon: FR-011 deterministic clearing on disable ----

    @Test
    void updateActivatedCarbon_disabledNullsModeAndReplacementInterval() {
        TankMaintenanceProfileEntity existing = profile(21, 25, true, CarbonMode.INTERVAL, 28);
        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(aquarium());
        given(profileRepository.findByAquariumId(AQUARIUM_ID)).willReturn(Optional.of(existing));
        given(profileRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        ResultTo<MaintenanceProfileTo> result = maintenanceService.updateActivatedCarbon(AQUARIUM_ID, carbon(false, null, null), USER_EMAIL);

        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
        ArgumentCaptor<TankMaintenanceProfileEntity> captor = ArgumentCaptor.forClass(TankMaintenanceProfileEntity.class);
        verify(profileRepository).save(captor.capture());
        assertFalse(captor.getValue().getCarbonEnabled());
        assertNull(captor.getValue().getCarbonMode());
        assertNull(captor.getValue().getCarbonReplacementIntervalDays());
    }

    @Test
    void updateActivatedCarbon_enabledContinuous_clearsReplacementInterval() {
        TankMaintenanceProfileEntity existing = profile(21, 25, true, CarbonMode.INTERVAL, 28);
        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(aquarium());
        given(profileRepository.findByAquariumId(AQUARIUM_ID)).willReturn(Optional.of(existing));
        given(profileRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        ResultTo<MaintenanceProfileTo> result = maintenanceService.updateActivatedCarbon(AQUARIUM_ID, carbon(true, CarbonMode.CONTINUOUS, null), USER_EMAIL);

        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
        ArgumentCaptor<TankMaintenanceProfileEntity> captor = ArgumentCaptor.forClass(TankMaintenanceProfileEntity.class);
        verify(profileRepository).save(captor.capture());
        assertTrue(captor.getValue().getCarbonEnabled());
        assertEquals(CarbonMode.CONTINUOUS, captor.getValue().getCarbonMode());
        assertNull(captor.getValue().getCarbonReplacementIntervalDays());
    }

    // ---- water change records: path-derived aquariumId, trim-to-null ----

    @Test
    void createWaterChange_setsAquariumIdFromPathAndNormalizesNote() {
        WaterChangeTo to = validWaterChange();
        to.setAquariumId(999L); // must be ignored, taken from the path
        to.setNote("   ");

        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user(7L));
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(AQUARIUM_ID, 7L)).willReturn(aquarium());
        given(waterChangeRepository.saveAndFlush(any())).willAnswer(invocation -> {
            WaterChangeEntity entity = invocation.getArgument(0);
            entity.setId(42L);
            return entity;
        });

        ResultTo<WaterChangeTo> result = maintenanceService.createWaterChange(AQUARIUM_ID, to, USER_EMAIL);

        ArgumentCaptor<WaterChangeEntity> captor = ArgumentCaptor.forClass(WaterChangeEntity.class);
        verify(waterChangeRepository).saveAndFlush(captor.capture());
        assertEquals(AQUARIUM_ID, captor.getValue().getAquariumId());
        assertNull(captor.getValue().getNote(), "blank note must be normalized to null");
        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
        assertEquals(42L, result.getValue().getId());
    }

    @Test
    void futureDatedWaterChange_failsValidationPredicate() {
        WaterChangeTo to = new WaterChangeTo();
        to.setChangedOn(LocalDate.now().plusDays(1));

        assertFalse(to.isChangedOnNotInFuture(), "future dates must fail the Bean Validation predicate (400 in the IT)");
    }

    // ---- helpers ----

    private UserEntity user(Long id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(USER_EMAIL);
        return user;
    }

    private AquariumEntity aquarium() {
        AquariumEntity aquarium = new AquariumEntity();
        aquarium.setId(AQUARIUM_ID);
        return aquarium;
    }

    private TankMaintenanceProfileEntity profile(Integer intervalDays, Integer percentage,
                                                 Boolean carbonEnabled, CarbonMode mode, Integer replacementDays) {
        TankMaintenanceProfileEntity entity = new TankMaintenanceProfileEntity();
        entity.setId(5L);
        entity.setAquariumId(AQUARIUM_ID);
        entity.setRegularIntervalDays(intervalDays);
        entity.setRegularPercentage(percentage);
        entity.setCarbonEnabled(carbonEnabled);
        entity.setCarbonMode(mode);
        entity.setCarbonReplacementIntervalDays(replacementDays);
        return entity;
    }

    private WaterChangeEntity waterChange(Long id, LocalDate changedOn, Integer percentage, String note) {
        WaterChangeEntity entity = new WaterChangeEntity();
        entity.setId(id);
        entity.setAquariumId(AQUARIUM_ID);
        entity.setChangedOn(changedOn);
        entity.setPercentage(percentage);
        entity.setNote(note);
        return entity;
    }

    private WaterChangeTo validWaterChange() {
        WaterChangeTo to = new WaterChangeTo();
        to.setChangedOn(LocalDate.of(2026, 10, 26));
        to.setPercentage(25);
        return to;
    }

    private WaterChangeIntervalTo interval(Integer intervalDays, Integer percentage) {
        WaterChangeIntervalTo to = new WaterChangeIntervalTo();
        to.setIntervalDays(intervalDays);
        to.setPercentage(percentage);
        return to;
    }

    private ActivatedCarbonTo carbon(Boolean enabled, CarbonMode mode, Integer replacementDays) {
        ActivatedCarbonTo to = new ActivatedCarbonTo();
        to.setEnabled(enabled);
        to.setMode(mode);
        to.setReplacementIntervalDays(replacementDays);
        return to;
    }
}
