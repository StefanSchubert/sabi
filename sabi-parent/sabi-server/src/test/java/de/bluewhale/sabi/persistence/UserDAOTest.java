/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence;

import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.dao.UserDao;
import de.bluewhale.sabi.persistence.model.UserEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

import static org.junit.Assert.*;


/**
 * Persistence-Layer Test for userDAO
 * User: Stefan
 * Date: 14.11.2015
 */
@SpringBootTest
@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = AppConfig.class)
public class UserDAOTest {

    @Autowired
    UserDao userDao;

/*
    @BeforeClass
    public static void init() throws NamingException {
    }
*/

/*    @AfterClass
    public static void tearDownClass() throws Exception {
    }
*/

    @Test
    public void testProbeTracebleAttributeMappingsOnTestData() throws Exception {
        UserTo userTo = userDao.loadUserByEmail("sabi@bluewhale.de");
        UserEntity userEntity = userDao.find(userTo.getId());
        assertNotNull("Missing Default Testdata", userEntity);
        assertNotNull("EntityState should have been set.", userEntity.getEntityState());
        assertNotNull("Temporal Column not mapped.", userEntity.getEntityState().getCreatedOn());
        assertNotNull("Temporal Column not mapped.", userEntity.getEntityState().getLastmodOn());
    }


    @Test
    @Transactional
    public void testCreateUser() throws Exception {

        // given
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail("Test@bluewhale.de");
        userEntity.setPassword("Test123");
        userEntity.setCountry(Locale.GERMANY.getCountry());
        userEntity.setLanguage(Locale.GERMAN.getLanguage());
        userEntity.setValidateToken("abc123");
        userEntity.setId(4711L);

        // when
        userDao.create(userEntity);

        // then
        UserEntity foundUserEntity = userDao.find(userEntity.getId());

        assertEquals(foundUserEntity.getEmail(), userEntity.getEmail());

    }

    @Test
    @Transactional
    // This test is "lying" from integration test perspective.
    // During #sabi-22 we could observe (by testing the use case via rest calls),
    // that the datetime will be set be the Generic dao but ignored through jpa mapping.
    // Meaning test is green because of cache, but database had ignore the modifier mapping (before sabi-22 has been fixed)
    public void testModifierAttributesViaGenericDAO() throws Exception {

        // given
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail("ModifierTest@bluewhale.de");
        userEntity.setPassword("Test123");
        userEntity.setCountry(Locale.GERMANY.getCountry());
        userEntity.setLanguage(Locale.GERMAN.getLanguage());
        userEntity.setValidateToken("abc123");
        userEntity.setId(4712L);

        // when
        userDao.create(userEntity);
        UserEntity foundUserEntity = userDao.find(userEntity.getId());
        assertEquals(foundUserEntity.getEmail(), userEntity.getEmail());
        assertNull(foundUserEntity.getEntityState().getLastmodOn());

        // Now do a validation
        userDao.toggleValidationFlag(foundUserEntity.getEmail(), true);
        UserEntity updatedUserEntity = userDao.find(userEntity.getId());

        // then
        assertNotNull(updatedUserEntity.getEntityState().getLastmodOn());

    }

/*
    USED TO TEST SPRING-DATA-JPA APPROACH. CURRENTLY AUTOWIRING OF THE REPOSITORY IS NOT WORKING.
    @Test
    @Transactional
    // @Rollback(false)
    public void testCreateUserViaRepository() throws Exception {


        // given
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail("Test@bluewhale.de");
        userEntity.setPassword("Test123");
        userEntity.setxAuthToken("abc123");
        userEntity.setId(4711l);

        // when
        repository.saveAndFlush(userEntity);

        // then
        UserEntity foundUserEntity = repository.findOne(userEntity.getId());

        assertEquals(foundUserEntity.getEmail(), userEntity.getEmail());

    }*/

}
