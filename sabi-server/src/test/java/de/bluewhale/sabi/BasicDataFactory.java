/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi;

import de.bluewhale.sabi.model.SizeUnit;
import de.bluewhale.sabi.model.WaterType;
import de.bluewhale.sabi.persistence.model.*;
import de.bluewhale.sabi.persistence.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Derive your tests from this class to inject required minimum required basic data into H2.
 *
 * @author Stefan Schubert
 */
public class BasicDataFactory {

    @Autowired
    UserRepository userRepository;

    @Autowired
    ParameterRepository parameterRepository;

    @Autowired
    UnitRepository unitRepository;

    @Autowired
    LocalizedUnitRepository localizedUnitRepository;

    @Autowired
    RemedyRepository remedyRepository;

    @Autowired
    FishCatalogueRepository fishCatalogueRepository;

    @Autowired
    AquariumRepository aquariumRepository;

    @Autowired
    MeasurementRepository measurementRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static boolean populatedBasicData = false;

    // pre existing test user
    protected static final String P_USER1_EMAIL = "sabi@bluewhale.de";
    protected static final String P_USER2_EMAIL = "sabi_II@bluewhale.de";

    @BeforeEach
    public void initOnlyOnce()  {

        if (populatedBasicData) return;
        populatedBasicData = true;

        populateBasicData();

    }

    public void populateBasicData() {
        UserEntity testuser1 = new UserEntity();
        testuser1.setId(1l); // Don't rely on this ID it is replaced through H2 sequence on persistance when cerating a new entity
        testuser1.setEmail(P_USER1_EMAIL);
        testuser1.setPassword("098f6bcd4621d373cade4e832627b4f6");
        testuser1.setUsername("stefan");
        testuser1.setValidateToken("NO_IDEA");
        testuser1.setValidated(true);
        testuser1.setLanguage("de");
        testuser1.setCountry("DE");

        UserEntity testuser2 = new UserEntity();
        testuser2.setId(2l); // Don't rely on this ID it is replaced through H2 sequence on persistance when cerating a new entity
        testuser2.setEmail(P_USER2_EMAIL);
        testuser2.setPassword("098f6bcd4621d373cade4e832627b4f6");
        testuser2.setUsername("steven");
        testuser2.setValidateToken("NO_IDEA");
        testuser2.setValidated(true);
        testuser2.setLanguage("en");
        testuser2.setCountry("US");

        userRepository.saveAndFlush(testuser1);
        userRepository.saveAndFlush(testuser2);

        UnitEntity unitEntity = new UnitEntity();
        unitEntity.setName("KH");
        unitEntity.setId(1);

        UnitEntity unitEntity2 = new UnitEntity();
        unitEntity2.setName("°C");
        unitEntity2.setId(2);

        UnitEntity savedUnitEntity = unitRepository.saveAndFlush(unitEntity);

        LocalizedUnitEntity localizedUnitEntity = new LocalizedUnitEntity();
        localizedUnitEntity.setDescription("Karbonathärte / Alkanität");
        localizedUnitEntity.setUnitId(savedUnitEntity.getId());
        localizedUnitRepository.saveAndFlush(localizedUnitEntity);
        unitEntity.setLocalizedUnitEntities(List.of(localizedUnitEntity));

        UnitEntity savedUnitEntity2 = unitRepository.saveAndFlush(unitEntity2);

        LocalizedUnitEntity localizedUnitEntity2 = new LocalizedUnitEntity();
        localizedUnitEntity2.setDescription("Grad Celsius");
        localizedUnitEntity2.setUnitId(savedUnitEntity2.getId());
        localizedUnitRepository.saveAndFlush(localizedUnitEntity2);
        unitEntity2.setLocalizedUnitEntities(List.of(localizedUnitEntity2));

        ParameterEntity parameterEntity = new ParameterEntity();
        parameterEntity.setBelongingUnitId(1);
        parameterEntity.setMinThreshold(6.5f);
        parameterEntity.setMaxThreshold(10f);

        ParameterEntity parameterEntity2 = new ParameterEntity();
        parameterEntity2.setBelongingUnitId(2);
        parameterEntity2.setMinThreshold(24f);
        parameterEntity2.setMaxThreshold(27f);

        ParameterEntity savedParameterEntity1 = parameterRepository.saveAndFlush(parameterEntity);
        LocalizedParameterEntity localizedParameterEntity = new LocalizedParameterEntity();
        localizedParameterEntity.setDescription("Karbonathärte");
        localizedParameterEntity.setLanguage("de");
        localizedParameterEntity.setParameter_id(savedParameterEntity1.getId());
        savedParameterEntity1.setLocalizedParameterEntities(List.of(localizedParameterEntity));

        ParameterEntity savedParameterEntity2 = parameterRepository.saveAndFlush(parameterEntity2);
        LocalizedParameterEntity localizedParameterEntity2 = new LocalizedParameterEntity();
        localizedParameterEntity2.setDescription("Temperatur");
        localizedParameterEntity2.setLanguage("de");
        localizedParameterEntity2.setParameter_id(savedParameterEntity2.getId());
        savedParameterEntity2.setLocalizedParameterEntities(List.of(localizedParameterEntity2));

        RemedyEntity remedyEntity = new RemedyEntity();
        remedyEntity.setProductname("KH+");
        remedyEntity.setVendor("Dupla");

        remedyRepository.saveAndFlush(remedyEntity);

        FishCatalogueEntity fishCatalogueEntity = new FishCatalogueEntity();
        fishCatalogueEntity.setScientificName("Acreichthys tomentosus");
        fishCatalogueEntity.setDescription("Seegras Feilenfisch");
        fishCatalogueEntity.setMeerwasserwikiUrl("http://meerwasserwiki.de/w/index.php?title=Acreichthys_tomentosus");

        fishCatalogueRepository.saveAndFlush(fishCatalogueEntity);

        AquariumEntity aquariumEntity = new AquariumEntity();
        aquariumEntity.setId(1l);
        aquariumEntity.setSize(80);
        aquariumEntity.setSizeUnit(SizeUnit.LITER);
        aquariumEntity.setWaterType(WaterType.SEA_WATER);
        aquariumEntity.setDescription("Nano-Reef_H2");
        aquariumEntity.setActive(Boolean.TRUE);
        aquariumEntity.setUser(testuser1);

        AquariumEntity aquariumEntity2 = new AquariumEntity();
        aquariumEntity2.setId(2l);
        aquariumEntity2.setSize(200);
        aquariumEntity2.setSizeUnit(SizeUnit.LITER);
        aquariumEntity2.setWaterType(WaterType.SEA_WATER);
        aquariumEntity2.setDescription("Freshwater_H2");
        aquariumEntity2.setActive(Boolean.TRUE);
        aquariumEntity2.setUser(testuser1);

        AquariumEntity aquariumEntity3 = new AquariumEntity();
        aquariumEntity3.setId(3l);
        aquariumEntity3.setSize(150);
        aquariumEntity3.setSizeUnit(SizeUnit.GALLONS);
        aquariumEntity3.setWaterType(WaterType.SEA_WATER);
        aquariumEntity3.setDescription("Exhibit");
        aquariumEntity3.setActive(Boolean.TRUE);
        aquariumEntity3.setUser(testuser2);

        aquariumRepository.saveAndFlush(aquariumEntity);
        aquariumRepository.saveAndFlush(aquariumEntity2);
        aquariumRepository.saveAndFlush(aquariumEntity3);

        MeasurementEntity measurementEntity = new MeasurementEntity();
        measurementEntity.setId(1L);
        measurementEntity.setMeasuredOn(LocalDateTime.now());
        measurementEntity.setMeasuredValue(27);
        measurementEntity.setUnitId(unitEntity2.getId());
        measurementEntity.setAquarium(aquariumEntity);
        measurementEntity.setUser(testuser1);

        MeasurementEntity measurementEntity2 = new MeasurementEntity();
        measurementEntity2.setId(2L);
        measurementEntity2.setMeasuredOn(LocalDateTime.now());
        measurementEntity2.setMeasuredValue(20.5f);
        measurementEntity2.setUnitId(unitEntity2.getId());
        measurementEntity2.setAquarium(aquariumEntity);
        measurementEntity2.setUser(testuser1);

        MeasurementEntity measurementEntity3 = new MeasurementEntity();
        measurementEntity3.setId(3L);
        measurementEntity3.setMeasuredOn(LocalDateTime.now());
        measurementEntity3.setMeasuredValue(0.892f);
        measurementEntity3.setUnitId(unitEntity.getId());
        measurementEntity3.setAquarium(aquariumEntity2);
        measurementEntity3.setUser(testuser1);

        measurementRepository.saveAndFlush(measurementEntity);
        measurementRepository.saveAndFlush(measurementEntity2);
        measurementRepository.saveAndFlush(measurementEntity3);
    }

//    @After
//    public void tearDown() Ø {
//        JdbcTestUtils.deleteFromTables(jdbcTemplate, "measurement", "fish",
//                "fish_catalogue", "remedy", "parameter", "aquarium", "unit", "users");
//    }


}
