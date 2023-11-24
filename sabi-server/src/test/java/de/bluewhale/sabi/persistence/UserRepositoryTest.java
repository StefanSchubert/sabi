/*
 * Copyright (c) 2023 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.persistence;

import de.bluewhale.sabi.BasicDataFactory;
import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.persistence.model.UserEntity;
import de.bluewhale.sabi.persistence.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertNotEquals;
import static org.springframework.test.util.AssertionErrors.assertNotNull;


/**
 * Persistence-Layer Test for userDAO
 * User: Stefan
 * Date: 14.11.2015
 */
@SpringBootTest
@ContextConfiguration(classes = AppConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class UserRepositoryTest extends BasicDataFactory {

    @Autowired
    UserRepository userRepository;

    /**
     * There seems to be a timing problem with H2, that causes, that the basic data is not available
     * for some test classes, while for others it worked out. Until we know what's going wrong...
     * we "double inject" by extending the BasicTestDataFactory and by calling it directly.
     * The different behaviour can be observed by e.g. calling the master test suite and as comparising
     * the measurement testsuite while this is method is deaktivated.
     */
    @BeforeEach
    public void ensureBasicDataAvailability() {
        UserEntity byEmail = userRepository.getByEmail(P_USER1_EMAIL);
        if (byEmail == null) populateBasicData();
        UserEntity byEmail2 = userRepository.getByEmail(P_USER1_EMAIL);
        assertNotNull("H2-Basicdata injection did not work!" ,byEmail2);
    }

    @Test
    @Transactional
    public void testProbeTracebleAttributeMappingsOnTestData() throws Exception {

        UserEntity userEntity = userRepository.getByEmail("sabi@bluewhale.de");
        assertNotNull("Missing Default Testdata", userEntity);
        assertNotNull("Temporal Column CreatedOn not mapped.", userEntity.getCreatedOn());
        assertNotNull("Temporal Column LastmodOn not mapped.", userEntity.getLastmodOn());
    }

    @Test
    @Transactional
    public void testCreateUser() throws Exception {

        // given
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail("Test@bluewhale.de");
        userEntity.setUsername("Tester");
        userEntity.setPassword("Test123");
        userEntity.setCountry(Locale.GERMANY.getCountry());
        userEntity.setLanguage(Locale.GERMAN.getLanguage());
        userEntity.setValidateToken("abc123");
        userEntity.setId(4711L);

        // when
        userRepository.save(userEntity);

        // then
        UserEntity foundUserEntity = userRepository.getOne(userEntity.getId());

        assertEquals(foundUserEntity.getEmail(), userEntity.getEmail());

    }

    @Test
    @Transactional
    public void testModifierAttributesViaGenericDAO() throws Exception {

        // given
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail("ModifierTest@bluewhale.de");
        userEntity.setUsername("Sharky");
        userEntity.setPassword("Test123");
        userEntity.setCountry(Locale.GERMANY.getCountry());
        userEntity.setLanguage(Locale.GERMAN.getLanguage());
        userEntity.setValidateToken("abc123");
        userEntity.setId(4712L);

        UserEntity foundUserEntity = userRepository.saveAndFlush(userEntity);
        final LocalDateTime initalModDate = foundUserEntity.getLastmodOn();

        // when we do an update
        Thread.sleep(1100); // make sure we have a second difference
        foundUserEntity.setValidated(true);
        UserEntity updatedUserEntity = userRepository.saveAndFlush(foundUserEntity);

        // then
        assertNotEquals("LastModDates should differ.",initalModDate,updatedUserEntity.getLastmodOn());

    }

}
