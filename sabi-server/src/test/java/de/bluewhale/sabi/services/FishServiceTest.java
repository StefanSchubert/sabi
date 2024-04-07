/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.exception.Message.CATEGORY;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.FishTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.FishEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.AquariumRepository;
import de.bluewhale.sabi.persistence.repositories.FishRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import de.bluewhale.sabi.util.TestDataFactory;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static de.bluewhale.sabi.util.TestContainerVersions.MARIADB_11_3_2;
import static de.bluewhale.sabi.util.TestDataFactory.TESTUSER_EMAIL1;
import static de.bluewhale.sabi.util.TestDataFactory.TEST_FISH_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.AssertionErrors.*;


/**
 * Business-Layer tests for TankServices. Requires a running database.
 * User: Stefan
 * Date: 30.08.15
 */
@SpringBootTest
@Testcontainers
@ContextConfiguration(classes = AppConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("ServiceTest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class FishServiceTest {
// ------------------------------ FIELDS ------------------------------

        /*
     NOTICE This Testclass initializes a Testcontainer to satisfy the
        Spring Boot Context Initialization of JPAConfig. In fact we don't rely here on the
        Database level, as for test layer isolation we completely mock the repositories here.
        The Testcontainer is just needed to satisfy the Spring Boot Context Initialization.
        In future this Testclass might be refactored to be able to run without spring context,
        but for now we keep it as it is.
    */

    @Autowired
    private TankService tankService;
    @Autowired
    private FishService fishService;

    static TestDataFactory testDataFactory = TestDataFactory.getInstance();

    @Container
    @ServiceConnection
    // This does the trick. Spring Autoconfigures itself to connect against this container data requests-
    static MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>(MARIADB_11_3_2);

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private FishRepository fishRepository;

    @MockBean
    private AquariumRepository aquariumRepository;


// -------------------------- OTHER METHODS --------------------------

    /**
     * Add a fish and write it automatically to tanks history via log entry
     *
     * @throws Exception
     */
    @Test
    @Rollback
    public void testAddFish() throws Exception {
        // Given
        final UserTo testUser = testDataFactory.getNewTestUserTo(TESTUSER_EMAIL1);
        UserEntity testUserEntity = testDataFactory.getNewTestUserEntity(testUser);
        final AquariumTo aquariumTo = testDataFactory.getTestAquariumTo();
        AquariumEntity testAquariumEntity = testDataFactory.getTestAquariumEntity(aquariumTo, testUserEntity);
        FishTo testFishTo = testDataFactory.getTestFishTo(aquariumTo);

        given(userRepository.getOne(testUser.getId())).willReturn(testUserEntity);
        given(aquariumRepository.getOne(testFishTo.getAquariumId())).willReturn(testAquariumEntity);
        when(fishRepository.save(any(FishEntity.class))).thenAnswer(invocation -> {
                    FishEntity fishEntity = invocation.getArgument(0);
                    fishEntity.setId(TEST_FISH_ID);
                    return fishEntity;
                });

        // When
        // The user is required to check that he or she really possesses the tank
        final ResultTo<FishTo> fishResultTo = fishService.registerNewFish(testFishTo,testUser);

        // Then
        assertNotNull("ResultObject must not be empty",fishResultTo);
        final FishTo persistedFish = fishResultTo.getValue();
        assertNotNull("ResultObject had no Fish inside!",persistedFish);
        assertNotNull("Tank ID was not provided!",persistedFish.getAquariumId());
        assertEquals("Wrong message type.", CATEGORY.INFO, fishResultTo.getMessage().getType());
    }


    // A User cannot register a fish for a tank that he or she does not own.
    @Test
    @Rollback
        public void testAddFishForOtherUsersTank() throws Exception {
        // Given
        final UserTo testUser = testDataFactory.getNewTestUserTo(TESTUSER_EMAIL1);
        UserEntity testUserEntity = testDataFactory.getNewTestUserEntity(testUser);

        AquariumTo testAquariumFor = testDataFactory.getTestAquariumFor(testUser);
        AquariumEntity testAquariumEntity = testDataFactory.getTestAquariumEntity(testAquariumFor, testUserEntity);

        FishTo testFishTo = testDataFactory.getTestFishTo(testAquariumFor);
        final UserTo fraudUser = testDataFactory.getNewTestUserTo("I_Intent@No.good");
        fraudUser.setId(88L);
        UserEntity testFraudUserEntity = testDataFactory.getNewTestUserEntity(fraudUser);

        given(userRepository.getOne(fraudUser.getId())).willReturn(testFraudUserEntity);
        given(aquariumRepository.getOne(testFishTo.getAquariumId())).willReturn(testAquariumEntity); // an Aquarium that is not owned by the fraud user

        // When
        // The the fraud user tries to place something in a different tank
        final ResultTo<FishTo> fishResultTo = fishService.registerNewFish(testFishTo, fraudUser);

        // then
        assertNull("ResultObject Value should be empty as creation was not permitted.",fishResultTo.getValue());
        assertEquals("Wrong message type.", CATEGORY.ERROR, fishResultTo.getMessage().getType());

        }


    /**
     * Remode a fish and write it automatically to tanks history via log entry
     *
     * @throws Exception
     */
    @Test
    @Rollback
    public void testRemoveFish() throws Exception {
        // Given
        final UserTo testUser = testDataFactory.getNewTestUserTo(TESTUSER_EMAIL1);
        UserEntity testUserEntity = testDataFactory.getNewTestUserEntity(testUser);
        final AquariumTo aquariumTo = testDataFactory.getTestAquariumTo();
        AquariumEntity testAquariumEntity = testDataFactory.getTestAquariumEntity(aquariumTo, testUserEntity);
        FishTo testFishTo = testDataFactory.getTestFishTo(aquariumTo);
        FishEntity testFishEntity = testDataFactory.getTestFishEntity(testFishTo, testAquariumEntity.getId());

        Long fishId = testFishTo.getId();
        given(fishRepository.findUsersFish(fishId, testUser.getId())).willReturn(testFishEntity).willReturn(null);
        doNothing().when(fishRepository).delete(testFishEntity);

        // When
        fishService.removeFish(fishId, testUser);

        // Then
        FishTo removedFish = fishService.getUsersFish(fishId, testUser);
        assertNull("Fish was not removed!", removedFish);
    }
}
