/*
 * Copyright (c) 2026 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import de.bluewhale.sabi.model.AquariumExportTo;
import de.bluewhale.sabi.model.MeasurementExportTo;
import de.bluewhale.sabi.model.ReefDataExportTo;
import de.bluewhale.sabi.persistence.model.*;
import de.bluewhale.sabi.persistence.repositories.*;
import de.bluewhale.sabi.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
// removed slf4j LoggerFactory import (using Log4j2 LogManager instead)
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

import static de.bluewhale.sabi.util.TestContainerVersions.MARIADB_11_3_2;
import static de.bluewhale.sabi.util.TestDataFactory.TESTUSER_EMAIL1;
import static de.bluewhale.sabi.util.TestDataFactory.TEST_USER_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

/**
 * Unit tests for {@link ReefDataExportService} — FR-014 audit log and
 * unit name resolution (T021, T028).
 */
@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("ServiceTest")
public class ReefDataExportServiceTest {

    @Container
    @ServiceConnection
    static MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>(MARIADB_11_3_2);

    @Autowired
    private ReefDataExportService reefDataExportService;

    // --- Repository mocks ---
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private AquariumRepository aquariumRepository;
    @MockitoBean
    private MeasurementRepository measurementRepository;
    @MockitoBean
    private PlagueRecordEntityRepository plagueRecordRepository;
    @MockitoBean
    private FishRepository fishRepository;
    @MockitoBean
    private CoralRepository coralRepository;
    @MockitoBean
    private TreatmentRepository treatmentRepository;
    @MockitoBean
    private LocalizedUnitRepository localizedUnitRepository;
    @MockitoBean
    private UnitRepository unitRepository;
    @MockitoBean
    private LocalizedPlagueRepository localizedPlagueRepository;
    @MockitoBean
    private LocalizedPlagueStatusRepository localizedPlagueStatusRepository;
    @MockitoBean
    private FishCatalogueRepository fishCatalogueRepository;
    @MockitoBean
    private CoralCatalogueRepository coralCatalogueRepository;
    @MockitoBean
    private RemedyRepository remedyRepository;

    private static final TestDataFactory testDataFactory = TestDataFactory.getInstance();

    /** Capturing appender — captures log events from the service under test (Log4j2). */
    private CapturingAppender logAppender;

    @BeforeEach
    public void setUpLogCapture() {
        // Use Log4j2 core API to attach a capturing appender
        org.apache.logging.log4j.core.Logger serviceLogger =
                (org.apache.logging.log4j.core.Logger) LogManager.getLogger(ReefDataExportServiceImpl.class);

        // Remove any previous instance to avoid duplicates across tests
        if (serviceLogger.getAppenders().containsKey("list")) {
            // remove by Appender instance to avoid signature ambiguity across Log4j2 versions
            org.apache.logging.log4j.core.Appender existing = serviceLogger.getAppenders().get("list");
            if (existing != null) {
                serviceLogger.removeAppender(existing);
            }
        }

        logAppender = new CapturingAppender("list", PatternLayout.newBuilder().withPattern("%m").build());
        logAppender.start();
        serviceLogger.addAppender(logAppender);
    }

    /** Simple Log4j2 appender that records LogEvent instances in memory. */
    private static class CapturingAppender extends AbstractAppender {
        private final java.util.List<LogEvent> events = new java.util.concurrent.CopyOnWriteArrayList<>();

        protected CapturingAppender(String name, org.apache.logging.log4j.core.Layout<?> layout) {
            super(name, null, (org.apache.logging.log4j.core.Layout<?>) layout, false, null);
        }

        @Override
        public void append(LogEvent event) {
            // store an immutable copy to avoid lifecycle issues
            events.add(event.toImmutable());
        }

        public java.util.List<LogEvent> getEvents() {
            return events;
        }
    }

    // -------------------------------------------------------------------------
    // Internal test utilities
    // -------------------------------------------------------------------------

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private UserEntity buildTestUser() {
        UserEntity user = new UserEntity();
        user.setId(TEST_USER_ID);
        user.setEmail(TESTUSER_EMAIL1);
        user.setUsername("testservice1");
        user.setPassword("hashed");
        user.setValidateToken("tok");
        user.setValidated(true);
        user.setLanguage("de");
        user.setCountry("DE");
        return user;
    }

    private AquariumEntity buildTestAquarium() {
        AquariumEntity a = testDataFactory.getTestAquariumEntity(
                testDataFactory.getTestAquariumTo(), buildTestUser());
        return a;
    }

    /**
     * T021 — FR-014: audit log must contain exactly one INFO entry with
     * anonymised user hash and must NOT contain the raw email string.
     */
    @Test
    public void testAuditLogWrittenWithHashAndWithoutEmail() {
        // Given
        UserEntity user = buildTestUser();
        given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(user);
        given(aquariumRepository.findAllByUser_IdIs(TEST_USER_ID)).willReturn(Collections.emptyList());

        // When
        reefDataExportService.buildExportForUser(TESTUSER_EMAIL1);

        // Then — exactly one DATA_EXPORT log entry
        List<LogEvent> logs = logAppender.getEvents();
        long dataExportLogs = logs.stream()
                .filter(e -> e.getMessage().getFormattedMessage().contains("DATA_EXPORT"))
                .count();
        assertEquals(1, dataExportLogs, "Exactly one DATA_EXPORT audit log entry expected");

        // Hash must be present
        String expectedHash = sha256Hex(TEST_USER_ID.toString());
        boolean hashFound = logs.stream()
                .anyMatch(e -> e.getMessage().getFormattedMessage().contains(expectedHash));
        assertTrue(hashFound, "Audit log must contain SHA-256 hash of userId");

        // Email must NOT appear in any log message
        boolean emailFound = logs.stream()
                .anyMatch(e -> e.getMessage().getFormattedMessage().contains(TESTUSER_EMAIL1));
        assertFalse(emailFound, "Email address must NOT appear in audit log (FR-014)");
    }

    /**
     * T028 — Unit name resolution: resolvable unit returns unitNameResolved=true + correct names;
     * unresolvable unit returns unitNameResolved=false with raw unitId preserved.
     */
    @Test
    public void testUnitNameResolution() {
        // Given
        UserEntity user = buildTestUser();
        AquariumEntity aquarium = buildTestAquarium();
        given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(user);
        given(aquariumRepository.findAllByUser_IdIs(TEST_USER_ID)).willReturn(List.of(aquarium));

        // Two measurements: unitId=7 (resolvable) and unitId=99 (not resolvable)
        MeasurementEntity mResolvable = new MeasurementEntity();
        mResolvable.setId(1L);
        mResolvable.setUnitId(7);
        mResolvable.setMeasuredValue(0.05f);

        MeasurementEntity mUnresolvable = new MeasurementEntity();
        mUnresolvable.setId(2L);
        mUnresolvable.setUnitId(99);
        mUnresolvable.setMeasuredValue(8.2f);

        given(measurementRepository.findByAquarium_Id(aquarium.getId()))
                .willReturn(List.of(mResolvable, mUnresolvable));

        // Stub for resolvable unit (unitId=7)
        UnitEntity unitEntity = new UnitEntity();
        unitEntity.setId(7);
        unitEntity.setName("PO4");
        given(unitRepository.findAll()).willReturn(List.of(unitEntity));

        LocalizedUnitEntity localizedUnit = new LocalizedUnitEntity();
        localizedUnit.setUnitId(7);
        localizedUnit.setLanguage("en");
        localizedUnit.setDescription("Phosphate");
        given(localizedUnitRepository.findByLanguageAndUnitId("en", 7)).willReturn(localizedUnit);

        // Stub for unresolvable unit (unitId=99) — no entry
        given(localizedUnitRepository.findByLanguageAndUnitId("en", 99)).willReturn(null);

        // Stub empty lists for other sub-data
        given(plagueRecordRepository.findByAquarium_Id(aquarium.getId())).willReturn(Collections.emptyList());
        given(fishRepository.findFishEntitiesByAquariumId(aquarium.getId())).willReturn(Collections.emptyList());
        given(coralRepository.findCoralEntitiesByAquariumId(aquarium.getId())).willReturn(Collections.emptyList());
        given(treatmentRepository.findTreatmentEntitiesByAquariumId(aquarium.getId())).willReturn(Collections.emptyList());

        // When
        ReefDataExportTo result = reefDataExportService.buildExportForUser(TESTUSER_EMAIL1);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getAquariums().size());
        AquariumExportTo aquariumExport = result.getAquariums().get(0);
        assertEquals(2, aquariumExport.getMeasurements().size());

        // Find the resolvable and unresolvable measurement by unitId
        MeasurementExportTo resolvable = aquariumExport.getMeasurements().stream()
                .filter(m -> m.getUnitId() == 7)
                .findFirst()
                .orElseThrow();
        assertTrue(resolvable.getUnitNameResolved(), "unitNameResolved must be true for resolvable unit");
        assertEquals("Phosphate", resolvable.getUnitName(), "unitName must match catalogue description");
        assertEquals("PO4", resolvable.getUnitSign(), "unitSign must match unit abbreviation");

        MeasurementExportTo unresolvable = aquariumExport.getMeasurements().stream()
                .filter(m -> m.getUnitId() == 99)
                .findFirst()
                .orElseThrow();
        assertFalse(unresolvable.getUnitNameResolved(), "unitNameResolved must be false for unresolvable unit");
        assertNull(unresolvable.getUnitName(), "unitName must be null for unresolvable unit");
        assertEquals(99, unresolvable.getUnitId(), "raw unitId must be preserved as fallback");
    }
}
