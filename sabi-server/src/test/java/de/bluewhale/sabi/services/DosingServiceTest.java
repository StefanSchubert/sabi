/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.mapper.DosingMapper;
import de.bluewhale.sabi.model.DosingTo;
import de.bluewhale.sabi.model.DosingType;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.DosingEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.AquariumRepository;
import de.bluewhale.sabi.persistence.repositories.DosingRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Tag("ServiceTest")
class DosingServiceTest {

    private static final String USER_EMAIL = "owner.dosing@test.invalid";

    @Mock
    DosingRepository dosingRepository;

    @Mock
    AquariumRepository aquariumRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    DosingMapper dosingMapper;

    @InjectMocks
    DosingServiceImpl dosingService;

    @Test
    void createDosing_forForeignAquarium_returnsErrorAndNeverSaves() {
        UserEntity user = user(7L);
        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user);
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(99L, 7L)).willReturn(null);

        ResultTo<DosingTo> result = dosingService.createDosing(99L, validManualDosing(), USER_EMAIL);

        assertNotNull(result.getMessage());
        assertEquals(Message.CATEGORY.ERROR, result.getMessage().getType());
        verify(dosingRepository, never()).save(any());
    }

    @Test
    void createAutomatedDosing_setsAquariumAndNormalizesOptionalValues() {
        UserEntity user = user(7L);
        AquariumEntity aquarium = new AquariumEntity();
        aquarium.setId(99L);
        DosingEntity mapped = new DosingEntity();
        mapped.setDosingType(DosingType.AUTOMATED_DOSING);
        mapped.setProductName("  Calcium solution  ");
        mapped.setAmount(new BigDecimal("5.000"));
        mapped.setAmountUnit(" ml ");
        mapped.setDosingInterval(" daily ");
        mapped.setNote("  ");
        DosingEntity saved = new DosingEntity();
        saved.setId(15L);
        DosingTo savedTo = validManualDosing();
        savedTo.setId(15L);

        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user);
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(99L, 7L)).willReturn(aquarium);
        given(dosingMapper.mapToToEntity(any())).willReturn(mapped);
        given(dosingRepository.save(any())).willReturn(saved);
        given(dosingMapper.mapEntityToTo(saved)).willReturn(savedTo);

        ResultTo<DosingTo> result = dosingService.createDosing(99L, validAutomatedDosing(), USER_EMAIL);

        ArgumentCaptor<DosingEntity> savedCaptor = ArgumentCaptor.forClass(DosingEntity.class);
        verify(dosingRepository).save(savedCaptor.capture());
        DosingEntity persisted = savedCaptor.getValue();
        assertEquals(99L, persisted.getAquariumId());
        assertEquals("Calcium solution", persisted.getProductName());
        assertEquals("ml", persisted.getAmountUnit());
        assertEquals("daily", persisted.getDosingInterval());
        assertEquals(null, persisted.getNote());
        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
    }

    private UserEntity user(Long id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(USER_EMAIL);
        return user;
    }

    private DosingTo validManualDosing() {
        DosingTo dosing = new DosingTo();
        dosing.setRecordedOn(LocalDateTime.of(2026, 9, 4, 10, 30));
        dosing.setDosingType(DosingType.MANUAL_ADDITION);
        dosing.setProductName("Magnesium");
        dosing.setAmount(new BigDecimal("5.000"));
        dosing.setAmountUnit("ml");
        return dosing;
    }

    private DosingTo validAutomatedDosing() {
        DosingTo dosing = validManualDosing();
        dosing.setDosingType(DosingType.AUTOMATED_DOSING);
        dosing.setDosingInterval("daily");
        return dosing;
    }
}
