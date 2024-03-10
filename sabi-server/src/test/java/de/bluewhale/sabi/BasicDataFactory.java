/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi;

import de.bluewhale.sabi.model.SizeUnit;
import de.bluewhale.sabi.model.WaterType;
import de.bluewhale.sabi.persistence.model.*;
import de.bluewhale.sabi.persistence.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

/**
 * Derive your tests from this class to inject required minimum required basic data into H2.
 *
 * @author Stefan Schubert
 */
@Slf4j
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

    // pre existing test user
    protected static final String P_USER1_EMAIL = "sabi_I@bluewhale.de";
    protected static final String P_USER2_EMAIL = "sabi_II@bluewhale.de";


    // @Transactional
    public void populateBasicData() {

        log.info("Setup some basic Test data");

        UserEntity testuser1 = new UserEntity();
        testuser1.setId(101l); // Don't rely on this ID it is replaced through as sequence on persistence when creating a new entity
        testuser1.setEmail(P_USER1_EMAIL);
        testuser1.setPassword("098f6bcd4621d373cade4e832627b4f6");
        testuser1.setUsername("stefan");
        testuser1.setValidateToken("NO_IDEA");
        testuser1.setValidated(true);
        testuser1.setLanguage("de");
        testuser1.setCountry("DE");

        UserEntity testuser2 = new UserEntity();
        testuser2.setId(102l); // Don't rely on this ID it is replaced through a sequence on persistence when creating a new entity
        testuser2.setEmail(P_USER2_EMAIL);
        testuser2.setPassword("098f6bcd4621d373cade4e832627b4f6");
        testuser2.setUsername("steven");
        testuser2.setValidateToken("NO_IDEA");
        testuser2.setValidated(true);
        testuser2.setLanguage("en");
        testuser2.setCountry("US");

        userRepository.save(testuser1);
        userRepository.save(testuser2);



        UnitEntity unitEntity = new UnitEntity();
        unitEntity.setName("KH");
        unitEntity.setId(1);
        UnitEntity savedUnitEntity = unitRepository.save(unitEntity);

        UnitEntity unitEntity2 = new UnitEntity();
        unitEntity2.setName("°C");
        unitEntity2.setId(2);
        UnitEntity savedUnitEntity2 = unitRepository.save(unitEntity2);

        LocalizedUnitEntity localizedUnitEntity = new LocalizedUnitEntity();
        localizedUnitEntity.setDescription("Karbonathärte / Alkanität");
        localizedUnitEntity.setLanguage("de");
        localizedUnitEntity.setUnitId(savedUnitEntity.getId());
        LocalizedUnitEntity savedLocalizedUnitEntity1 = localizedUnitRepository.save(localizedUnitEntity);

        // unitEntity.setLocalizedUnitEntities(List.of(localizedUnitEntity));

        LocalizedUnitEntity localizedUnitEntity2 = new LocalizedUnitEntity();
        localizedUnitEntity2.setDescription("Grad Celsius");
        localizedUnitEntity2.setLanguage("de");
        localizedUnitEntity2.setUnitId(savedUnitEntity2.getId());
        LocalizedUnitEntity savedLocalizedUnitEntity2 = localizedUnitRepository.save(localizedUnitEntity2);

        // unitEntity2.setLocalizedUnitEntities(List.of(localizedUnitEntity2));

        ParameterEntity parameterEntity = new ParameterEntity();
        parameterEntity.setId(101);
        parameterEntity.setBelongingUnitId(1);
        parameterEntity.setMinThreshold(6.5f);
        parameterEntity.setMaxThreshold(10.1f);
        ParameterEntity savedParameterEntity1 = parameterRepository.save(parameterEntity);

        ParameterEntity parameterEntity2 = new ParameterEntity();
        parameterEntity2.setId(102);
        parameterEntity2.setBelongingUnitId(2);
        parameterEntity2.setMinThreshold(24f);
        parameterEntity2.setMaxThreshold(27f);
        ParameterEntity savedParameterEntity2 = parameterRepository.save(parameterEntity2);

        LocalizedParameterEntity localizedParameterEntity = new LocalizedParameterEntity();
        localizedParameterEntity.setDescription("Karbonathärte");
        localizedParameterEntity.setLanguage("de");
        localizedParameterEntity.setParameter_id(savedParameterEntity1.getId());

        // savedParameterEntity1.setLocalizedParameterEntities(List.of(localizedParameterEntity));

        LocalizedParameterEntity localizedParameterEntity2 = new LocalizedParameterEntity();
        localizedParameterEntity2.setDescription("Temperatur");
        localizedParameterEntity2.setLanguage("de");
        localizedParameterEntity2.setParameter_id(savedParameterEntity2.getId());

        // savedParameterEntity2.setLocalizedParameterEntities(List.of(localizedParameterEntity2));


        RemedyEntity remedyEntity = new RemedyEntity();
        remedyEntity.setProductname("KH+");
        remedyEntity.setVendor("Dupla");

        remedyRepository.save(remedyEntity);

        FishCatalogueEntity fishCatalogueEntity = new FishCatalogueEntity();
        fishCatalogueEntity.setScientificName("Acreichthys tomentosus");
        fishCatalogueEntity.setDescription("Seegras Feilenfisch");
        fishCatalogueEntity.setMeerwasserwikiUrl("http://meerwasserwiki.de/w/index.php?title=Acreichthys_tomentosus");

        fishCatalogueRepository.save(fishCatalogueEntity);

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

        aquariumRepository.save(aquariumEntity);
        aquariumRepository.save(aquariumEntity2);
        aquariumRepository.save(aquariumEntity3);

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

        measurementRepository.save(measurementEntity);
        measurementRepository.save(measurementEntity2);
        measurementRepository.save(measurementEntity3);
    }

}
