/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.mapper.*;
import de.bluewhale.sabi.model.PublicReefReportTo;
import de.bluewhale.sabi.persistence.model.*;
import de.bluewhale.sabi.persistence.repositories.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link PublicReportServiceImpl}:
 * <ul>
 *   <li>T-034: 12-month (365-day) rolling filter applied to recentEvents in getReport()</li>
 *   <li>T-036: updateIncludeEvents() returns false when no link exists → no save called</li>
 * </ul>
 *
 * Feature: 004-aquarium-events.
 *
 * @author Stefan Schubert
 */
@ExtendWith(MockitoExtension.class)
@Tag("ServiceTest")
public class PublicReportServiceTest {

    private static final String LINK_TOKEN = "test-share-token-uuid";
    private static final String USER_EMAIL = "reportowner@bluewhale.de";

    // ---- Mocked dependencies for PublicReportServiceImpl ----

    @Mock
    PublicReportLinkRepository publicReportLinkRepository;

    @Mock
    AquariumRepository aquariumRepository;

    @Mock
    AquariumPhotoRepository aquariumPhotoRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    TankFishStockRepository tankFishStockRepository;

    @Mock
    FishPhotoRepository fishPhotoRepository;

    @Mock
    MeasurementRepository measurementRepository;

    @Mock
    UnitRepository unitRepository;

    @Mock
    LocalizedUnitRepository localizedUnitRepository;

    @Mock
    AquariumMapper aquariumMapper;

    @Mock
    FishStockMapper fishStockMapper;

    @Mock
    MeasurementMapper measurementMapper;

    @Mock
    PhotoStorageService aquariumPhotoStorage;

    @Mock
    PhotoStorageService fishPhotoStorage;

    @Mock
    AquariumEventRepository aquariumEventRepository;

    @Mock
    AquariumEventMapper aquariumEventMapper;

    @InjectMocks
    PublicReportServiceImpl publicReportService;

    // ---- T-034: 12-month filter in getReport() ----

    /**
     * T-034: Given a report link with includeEvents=true, a tank with two events
     * (–6 months = in window, –13 months = outside window), getReport() must include
     * only the recent event (–6 months).
     */
    @Test
    public void testGetReport_includeEvents_onlyRecentEventsReturned() {
        // Given — a valid, non-expired report link with includeEvents = true
        Long aquariumId = 10L;
        PublicReportLinkEntity link = new PublicReportLinkEntity();
        link.setId(1L);
        link.setAquariumId(aquariumId);
        link.setLinkToken(LINK_TOKEN);
        link.setIncludeEvents(true);
        // no expiry

        given(publicReportLinkRepository.findByLinkToken(LINK_TOKEN))
                .willReturn(Optional.of(link));

        // Aquarium + owner
        UserEntity owner = new UserEntity();
        owner.setId(5L);
        owner.setUsername("reportOwner");

        AquariumEntity aquarium = new AquariumEntity();
        aquarium.setId(aquariumId);
        aquarium.setUser(owner);

        given(aquariumRepository.findById(aquariumId)).willReturn(Optional.of(aquarium));
        given(aquariumMapper.mapAquariumEntity2To(aquarium)).willReturn(new de.bluewhale.sabi.model.AquariumTo());
        given(aquariumPhotoRepository.findByAquariumId(aquariumId)).willReturn(Optional.empty());
        given(tankFishStockRepository.findAllByAquariumId(aquariumId)).willReturn(List.of());
        given(measurementRepository.findByAquarium_Id(aquariumId)).willReturn(List.of());

        // Two events: one in window (–6 months), one outside window (–13 months)
        LocalDate inWindow = LocalDate.now().minusMonths(6);
        LocalDate outsideWindow = LocalDate.now().minusMonths(13);

        AquariumEventEntity recentEvent = new AquariumEventEntity();
        recentEvent.setId(100L);
        recentEvent.setAquariumId(aquariumId);
        recentEvent.setEventDate(inWindow);
        recentEvent.setDescription("Recent event");

        // The repository query uses minusDays(365) as cutoff:
        // outsideWindow (-13 months) is before the 365-day cutoff, so only recentEvent is returned
        LocalDate expectedCutoff = LocalDate.now().minusDays(365);

        given(aquariumEventRepository.findByAquariumIdAndEventDateGreaterThanEqualOrderByEventDateDesc(
                eq(aquariumId), any(LocalDate.class)))
                .willReturn(List.of(recentEvent));

        de.bluewhale.sabi.model.AquariumEventTo recentEventTo = new de.bluewhale.sabi.model.AquariumEventTo();
        recentEventTo.setId(100L);
        recentEventTo.setEventDate(inWindow);
        recentEventTo.setDescription("Recent event");

        given(aquariumEventMapper.mapEntitiesToTos(List.of(recentEvent)))
                .willReturn(List.of(recentEventTo));

        // When
        PublicReefReportTo report = publicReportService.getReport(LINK_TOKEN, "en");

        // Then
        assertFalse(report.isLinkExpired(), "Link should not be expired");
        assertNotNull(report.getRecentEvents(), "recentEvents must not be null when includeEvents=true");
        assertEquals(1, report.getRecentEvents().size(), "Exactly 1 event (the in-window one) must be present");

        de.bluewhale.sabi.model.AquariumEventTo returnedEvent = report.getRecentEvents().get(0);
        assertEquals(100L, returnedEvent.getId(), "The returned event must be the recent (in-window) one");
        assertTrue(returnedEvent.getEventDate().isAfter(expectedCutoff) ||
                        returnedEvent.getEventDate().isEqual(expectedCutoff),
                "The event date must be within the 365-day window");
    }

    /**
     * T-034 complementary: When includeEvents=false, recentEvents remains null.
     */
    @Test
    public void testGetReport_includeEventsFalse_recentEventsIsNull() {
        // Given — link with includeEvents = false
        Long aquariumId = 20L;
        PublicReportLinkEntity link = new PublicReportLinkEntity();
        link.setId(2L);
        link.setAquariumId(aquariumId);
        link.setLinkToken(LINK_TOKEN);
        link.setIncludeEvents(false);

        given(publicReportLinkRepository.findByLinkToken(LINK_TOKEN))
                .willReturn(Optional.of(link));

        UserEntity owner = new UserEntity();
        owner.setId(6L);
        owner.setUsername("noEventsOwner");

        AquariumEntity aquarium = new AquariumEntity();
        aquarium.setId(aquariumId);
        aquarium.setUser(owner);

        given(aquariumRepository.findById(aquariumId)).willReturn(Optional.of(aquarium));
        given(aquariumMapper.mapAquariumEntity2To(aquarium)).willReturn(new de.bluewhale.sabi.model.AquariumTo());
        given(aquariumPhotoRepository.findByAquariumId(aquariumId)).willReturn(Optional.empty());
        given(tankFishStockRepository.findAllByAquariumId(aquariumId)).willReturn(List.of());
        given(measurementRepository.findByAquarium_Id(aquariumId)).willReturn(List.of());

        // When
        PublicReefReportTo report = publicReportService.getReport(LINK_TOKEN, "en");

        // Then
        assertNull(report.getRecentEvents(), "recentEvents must be null when includeEvents=false");
        // Verify event repository was never queried
        verify(aquariumEventRepository, never()).findByAquariumIdAndEventDateGreaterThanEqualOrderByEventDateDesc(
                any(), any());
    }

    // ---- T-036: updateIncludeEvents() when no link exists ----

    /**
     * T-036: When no PublicReportLink exists for the given aquarium,
     * updateIncludeEvents() must return false and must NOT call save().
     */
    @Test
    public void testUpdateIncludeEvents_noLinkExists_returnsFalse() {
        // Given — user exists, aquarium owned by user, but NO report link
        Long aquariumId = 30L;
        UserEntity user = new UserEntity();
        user.setId(8L);
        user.setEmail(USER_EMAIL);

        AquariumEntity aquarium = new AquariumEntity();
        aquarium.setId(aquariumId);
        aquarium.setUser(user);

        given(userRepository.getByEmail(USER_EMAIL)).willReturn(user);
        given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumId, user.getId()))
                .willReturn(aquarium);
        given(publicReportLinkRepository.findByAquariumId(aquariumId))
                .willReturn(Optional.empty()); // no link!

        // When
        boolean result = publicReportService.updateIncludeEvents(aquariumId, true, USER_EMAIL);

        // Then
        assertFalse(result, "Must return false when no report link exists for the aquarium");
        verify(publicReportLinkRepository, never()).save(any());
    }
}

