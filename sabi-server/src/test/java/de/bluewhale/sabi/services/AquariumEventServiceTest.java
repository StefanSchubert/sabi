/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.mapper.AquariumEventMapper;
import de.bluewhale.sabi.model.AquariumEventTo;
import de.bluewhale.sabi.model.AquariumEventType;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.AquariumEventEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.AquariumEventRepository;
import de.bluewhale.sabi.persistence.repositories.AquariumRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link AquariumEventServiceImpl} — ownership failure scenario.
 * Feature: 004-aquarium-events / T-035.
 *
 * @author Stefan Schubert
 */
@ExtendWith(MockitoExtension.class)
@Tag("ServiceTest")
public class AquariumEventServiceTest {

    private static final String USER_EMAIL = "testowner@bluewhale.de";

    @Mock
    AquariumEventRepository aquariumEventRepository;

    @Mock
    AquariumRepository aquariumRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    AquariumEventMapper aquariumEventMapper;

    @InjectMocks
    AquariumEventServiceImpl aquariumEventService;

    @Test
    public void testCreateEvent_ownershipFailure_returnsError_andNeverSaves() {
        // Given — user exists but aquarium does not belong to them
        Long aquariumId = 99L;
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setEmail(USER_EMAIL);

        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user);
        // Ownership check fails: getAquariumEntityByIdAndUser_IdIs returns null
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumId, user.getId())).willReturn(null);

        AquariumEventTo eventTo = new AquariumEventTo();
        eventTo.setAquariumId(aquariumId);
        eventTo.setEventDate(LocalDate.now());
        eventTo.setDescription("Should not be saved");

        // When
        ResultTo<AquariumEventTo> result = aquariumEventService.createEvent(aquariumId, eventTo, USER_EMAIL);

        // Then
        assertNotNull(result, "Result must not be null");
        assertNotNull(result.getMessage(), "Result message must not be null");
        assertEquals(Message.CATEGORY.ERROR, result.getMessage().getType(),
                "Expected ERROR category when ownership check fails");

        // Verify that save was never called
        verify(aquariumEventRepository, never()).save(any());
    }

    @Test
    public void testCreateEvent_manualAddition_buildsStructuredSummary() {
        Long aquariumId = 99L;
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setEmail(USER_EMAIL);

        AquariumEntity aquarium = new AquariumEntity();
        aquarium.setId(aquariumId);

        AquariumEventEntity mappedEntity = new AquariumEventEntity();
        AquariumEventEntity savedEntity = new AquariumEventEntity();
        savedEntity.setId(15L);
        savedEntity.setAquariumId(aquariumId);
        savedEntity.setEventType(AquariumEventType.MANUAL_ADDITION);
        savedEntity.setDescription("Phos-Ex - 5 ml (Nutrient)");

        AquariumEventTo savedTo = new AquariumEventTo();
        savedTo.setId(15L);
        savedTo.setAquariumId(aquariumId);
        savedTo.setEventType(AquariumEventType.MANUAL_ADDITION);
        savedTo.setDescription("Phos-Ex - 5 ml (Nutrient)");

        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user);
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumId, user.getId())).willReturn(aquarium);
        given(aquariumEventMapper.mapToToEntity(any())).willReturn(mappedEntity);
        given(aquariumEventRepository.save(any())).willReturn(savedEntity);
        given(aquariumEventMapper.mapEntityToTo(savedEntity)).willReturn(savedTo);

        AquariumEventTo eventTo = new AquariumEventTo();
        eventTo.setAquariumId(aquariumId);
        eventTo.setEventDate(LocalDate.now());
        eventTo.setEventTime("10:30");
        eventTo.setEventType(AquariumEventType.MANUAL_ADDITION);
        eventTo.setProductName("Phos-Ex");
        eventTo.setAmount(new BigDecimal("5.000"));
        eventTo.setAmountUnit("ml");
        eventTo.setCategory("Nutrient");

        ResultTo<AquariumEventTo> result = aquariumEventService.createEvent(aquariumId, eventTo, USER_EMAIL);

        ArgumentCaptor<AquariumEventEntity> entityCaptor = ArgumentCaptor.forClass(AquariumEventEntity.class);
        verify(aquariumEventRepository).save(entityCaptor.capture());
        AquariumEventEntity persisted = entityCaptor.getValue();

        assertEquals(AquariumEventType.MANUAL_ADDITION, persisted.getEventType());
        assertEquals("Phos-Ex - 5 ml (Nutrient)", persisted.getDescription());
        assertEquals(Message.CATEGORY.INFO, result.getMessage().getType());
        assertEquals(15L, result.getValue().getId());
    }
}
