/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.services;

import de.bluewhale.sabi.TestDataFactory;
import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.model.AquariumEntity;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.AquariumRepository;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
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

import java.util.List;

import static de.bluewhale.sabi.TestDataFactory.TESTUSER_EMAIL1;
import static de.bluewhale.sabi.TestDataFactory.TEST_TANK_ID;
import static de.bluewhale.sabi.configs.TestContainerVersions.MARIADB_11_3_2;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
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
public class TankServiceTest {

	/*
	NOTICE This Testclass initializes a Testcontainer to satisfy the
	   Spring Boot Context Initialization of JPAConfig. In fact we don't rely here on the
	   Database level, as for test layer isolation we completely mock the repositories here.
	   The Testcontainer is just needed to satisfy the Spring Boot Context Initialization.
	   In future this Testclass might be refactored to be able to run without spring context,
	   but for now we keep it as it is.
   */
	private static final String JABA_DABA_DOOOOO = "JabaDabaDooooo";


	static TestDataFactory testDataFactory = TestDataFactory.getInstance();

	@Container
	@ServiceConnection
	// This does the trick. Spring Autoconfigures itself to connect against this container data requests-
	static MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>(MARIADB_11_3_2);

	// ------------------------------ FIELDS ------------------------------

	@Autowired
	private TankService tankService;

	@Autowired
	private UserService userService;

	@MockBean
	private UserRepository userRepository;
	@MockBean
	private AquariumRepository aquariumRepository;

// -------------------------- OTHER METHODS --------------------------

	/**
	 * Tank properties are something like name, description, size.
	 * Excluded are inhabitants etc... they are linked to a tank
	 *
	 * @throws Exception
	 */
	@Test
	@Rollback
	public void testAlterTankProperties() throws Exception {
		// Given
		final UserTo testUser = testDataFactory.getNewTestUserTo(TESTUSER_EMAIL1);
		UserEntity userEntity = testDataFactory.getNewTestUserEntity(testUser);

		AquariumTo aquariumTo = testDataFactory.getTestAquariumTo();
		aquariumTo.setDescription(JABA_DABA_DOOOOO); // Change the description
		AquariumEntity aquariumEntity = testDataFactory.getTestAquariumEntity(aquariumTo, userEntity);

		given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(userEntity);
		given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(TEST_TANK_ID, userEntity.getId())).willReturn(aquariumEntity);
		given(aquariumRepository.saveAndFlush(any())).willReturn(aquariumEntity);

		// When
		ResultTo<AquariumTo> aquariumToResultTo = tankService.updateTank(aquariumTo, TESTUSER_EMAIL1);

		// Then
		aquariumTo = tankService.getTank(aquariumToResultTo.getValue().getId(), TESTUSER_EMAIL1);

		// Then
		assertEquals("Tank was not updated", aquariumTo.getDescription(), JABA_DABA_DOOOOO);
	}


	@Test
	public void testListUsersTanks() throws Exception {
		// Given
		UserTo testUser = testDataFactory.getNewTestUserTo(TESTUSER_EMAIL1);
		UserEntity userEntity = testDataFactory.getNewTestUserEntity(testUser);

		AquariumEntity aquarium1 = testDataFactory.getTestAquariumEntity(testDataFactory.getTestAquariumTo(), userEntity);
		AquariumEntity aquarium2 = testDataFactory.getTestAquariumEntity(testDataFactory.getTestAquariumTo(), userEntity);
		aquarium2.setDescription("Second Tank");

		// Mocking
		given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(userEntity);
		given(aquariumRepository.findAllByUser_IdIs(userEntity.getId())).willReturn(List.of(aquarium1, aquarium2));

		// When
		List<AquariumTo> usersAquariums = tankService.listTanks(TESTUSER_EMAIL1);

		// Then
		assertNotNull("Oops", usersAquariums);
		assertEquals("Lost Testdata?", 2, usersAquariums.size());
		assertTrue(usersAquariums.get(0).getDescription() == aquarium1.getDescription());
		assertTrue(usersAquariums.get(1).getDescription() == aquarium2.getDescription());
	}


	@Test
	@Rollback
	public void testRegisterNewTank() throws Exception {
		// Given
		final UserTo testUser = testDataFactory.getNewTestUserTo(TESTUSER_EMAIL1);
		UserEntity userEntity = testDataFactory.getNewTestUserEntity(testUser);

		final AquariumTo aquariumTo = testDataFactory.getTestAquariumTo();
		AquariumEntity aquarium1 = testDataFactory.getTestAquariumEntity(aquariumTo, userEntity);

		// Mocking
		given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(userEntity);
		given(aquariumRepository.existsById(TEST_TANK_ID)).willReturn(false);
		given(aquariumRepository.saveAndFlush(any())).willReturn(aquarium1);

		// When
		final ResultTo<AquariumTo> aquariumToResultTo = tankService.registerNewTank(aquariumTo, TESTUSER_EMAIL1);

		// Then
		assertNotNull("ResultObject must not be empty", aquariumToResultTo);
		final AquariumTo aquarium = aquariumToResultTo.getValue();
		assertNotNull("ResultObject had no Aquarium inside!", aquarium);
		assertNotNull("Tank ID was not provided!", aquarium.getId());
		assertEquals("User Assignment missing.", testUser.getId(), aquarium.getUserId());
		assertEquals("Wrong message type.", Message.CATEGORY.INFO, aquariumToResultTo.getMessage().getType());
	}

	@Test
	@Rollback
	public void testCreateTemperatureAPIKeyForTankAndRetrieveTankByAPIKey() throws Exception {
		// Given
		UserTo testUserTo = testDataFactory.getNewTestUserTo(TESTUSER_EMAIL1);
		UserEntity testUserEntity = testDataFactory.getNewTestUserEntity(testUserTo);
		AquariumTo aquariumTo = testDataFactory.getTestAquariumFor(testUserTo);
		AquariumEntity testAquariumEntity = testDataFactory.getTestAquariumEntity(aquariumTo, testUserEntity);
		AquariumEntity testAquariumEntityWithAPIKey = testDataFactory.getTestAquariumEntity(aquariumTo, testUserEntity);
		testAquariumEntityWithAPIKey.setTemperatureApiKey("1234567890");

		// Mocking
		given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(testUserEntity);
		given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(TEST_TANK_ID, testUserTo.getId())).willReturn(testAquariumEntity);
		given(aquariumRepository.getAquariumEntityByTemperatureApiKeyEquals(any()))
				.willReturn(null)
				.willReturn(testAquariumEntityWithAPIKey);

		// When
		ResultTo<AquariumTo> resultOfAPIKeyGeneration = tankService.generateAndAssignNewTemperatureApiKey(aquariumTo.getId(), TESTUSER_EMAIL1);
		AquariumTo retrievedTankByAPIKey = tankService.getTankForTemperatureApiKey(resultOfAPIKeyGeneration.getValue().getTemperatureApiKey());

		// Then

		assertNotNull("ResultObject for generate an API Key must not be empty", resultOfAPIKeyGeneration);

		final AquariumTo aquariumWithJustAddedAPIKey = resultOfAPIKeyGeneration.getValue();
		assertNotNull("ResultObject for generate an API Key had no Aquarium inside!", aquariumWithJustAddedAPIKey);
		assertNotNull("API Key has not been stored!", aquariumWithJustAddedAPIKey.getTemperatureApiKey());
		assertEquals("Wrong message type.", Message.CATEGORY.INFO, resultOfAPIKeyGeneration.getMessage().getType());

		assertNotNull("Did not retrieved a tank by API Key", retrievedTankByAPIKey);
		assertEquals("Ouch - returned different Tank! This is a MAJOR Bug", aquariumWithJustAddedAPIKey.getId(), retrievedTankByAPIKey.getId());
	}

	@Test
	@Rollback
	public void testRemoveTank() throws Exception {

		// Given
		final UserTo testUserTo = testDataFactory.getNewTestUserTo(TESTUSER_EMAIL1);
		UserEntity testUserEntity = testDataFactory.getNewTestUserEntity(testUserTo);
		final AquariumTo aquariumTo = testDataFactory.getTestAquariumTo();
		AquariumEntity testAquariumEntity = testDataFactory.getTestAquariumEntity(aquariumTo, testUserEntity);

		given(userRepository.getByEmail(TESTUSER_EMAIL1)).willReturn(testUserEntity);
		given(aquariumRepository.getAquariumEntityByIdAndUser_IdIs(aquariumTo.getId(), testUserTo.getId()))
				.willReturn(testAquariumEntity)
				.willReturn(null);
		doNothing().when(aquariumRepository).delete(testAquariumEntity);

		// When
		tankService.removeTank(aquariumTo.getId(), TESTUSER_EMAIL1);

		// Then
		AquariumTo tankAfterDeletion = tankService.getTank(aquariumTo.getId(), TESTUSER_EMAIL1);
		assertNull("Users tank was supposed to be removed!", tankAfterDeletion);

	}
}
