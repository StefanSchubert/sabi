/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.mapper.AquariumEventMapper;
import de.bluewhale.sabi.model.AquariumEventTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.AquariumEventRepository;
import de.bluewhale.sabi.persistence.repositories.AquariumRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}

