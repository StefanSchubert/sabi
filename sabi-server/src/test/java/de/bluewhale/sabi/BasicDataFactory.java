/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi;

import de.bluewhale.sabi.model.SizeUnit;
import de.bluewhale.sabi.persistence.model.*;
import de.bluewhale.sabi.persistence.repositories.*;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

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
    protected static final String P_USER_EMAIL = "sabi@bluewhale.de";

    @Before
    public void initOnlyOnce()  {

        if (populatedBasicData) return;
        populatedBasicData = true;

        populateBasicData();

    }

    public void populateBasicData() {
        UserEntity testuser = new UserEntity();
        testuser.setId(1l);
        testuser.setEmail(P_USER_EMAIL);
        testuser.setPassword("098f6bcd4621d373cade4e832627b4f6");
        testuser.setUsername("stefan");
        testuser.setValidateToken("NO_IDEA");
        testuser.setValidated(true);
        testuser.setLanguage("de");
        testuser.setCountry("DE");

        userRepository.saveAndFlush(testuser);

        UnitEntity unitEntity = new UnitEntity();
        unitEntity.setName("KH");
        unitEntity.setDescription("Karbonathärte / Alkanität");
        unitEntity.setId(1);

        UnitEntity unitEntity2 = new UnitEntity();
        unitEntity2.setName("°C");
        unitEntity2.setDescription("Grad Celsius");
        unitEntity2.setId(2);

        unitRepository.saveAndFlush(unitEntity);
        unitRepository.saveAndFlush(unitEntity2);

        ParameterEntity parameterEntity = new ParameterEntity();
        parameterEntity.setDescription("Karbonathärte");
        parameterEntity.setUsedThresholdUnitId(1);
        parameterEntity.setMinThreshold(6.5f);
        parameterEntity.setMaxThreshold(10f);

        ParameterEntity parameterEntity2 = new ParameterEntity();
        parameterEntity2.setDescription("Temperatur");
        parameterEntity2.setUsedThresholdUnitId(2);
        parameterEntity2.setMinThreshold(24f);
        parameterEntity2.setMaxThreshold(27f);

        parameterRepository.saveAndFlush(parameterEntity);
        parameterRepository.saveAndFlush(parameterEntity2);

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
        aquariumEntity.setDescription("Nano-Reef_H2");
        aquariumEntity.setActive(Boolean.TRUE);
        aquariumEntity.setUser(testuser);

        AquariumEntity aquariumEntity2 = new AquariumEntity();
        aquariumEntity2.setId(2l);
        aquariumEntity2.setSize(200);
        aquariumEntity2.setSizeUnit(SizeUnit.LITER);
        aquariumEntity2.setDescription("Freshwater_H2");
        aquariumEntity2.setActive(Boolean.TRUE);
        aquariumEntity2.setUser(testuser);

        aquariumRepository.saveAndFlush(aquariumEntity);
        aquariumRepository.saveAndFlush(aquariumEntity2);

        MeasurementEntity measurementEntity = new MeasurementEntity();
        measurementEntity.setId(1L);
        measurementEntity.setMeasuredOn(LocalDateTime.now());
        measurementEntity.setMeasuredValue(27);
        measurementEntity.setUnitId(unitEntity2.getId());
        measurementEntity.setAquarium(aquariumEntity);
        measurementEntity.setUser(testuser);

        MeasurementEntity measurementEntity2 = new MeasurementEntity();
        measurementEntity2.setId(2L);
        measurementEntity2.setMeasuredOn(LocalDateTime.now());
        measurementEntity2.setMeasuredValue(15.5f);
        measurementEntity2.setUnitId(unitEntity.getId());
        measurementEntity2.setAquarium(aquariumEntity2);
        measurementEntity2.setUser(testuser);

        measurementRepository.saveAndFlush(measurementEntity);
        measurementRepository.saveAndFlush(measurementEntity2);
    }

//    @After
//    public void tearDown() Ø {
//        JdbcTestUtils.deleteFromTables(jdbcTemplate, "measurement", "fish",
//                "fish_catalogue", "remedy", "parameter", "aquarium", "unit", "users");
//    }


}
