/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence;

import de.bluewhale.sabi.configs.AppConfig;
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
    @Transactional
    // @Rollback(false)
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
    // @Rollback(false)
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
        assertNull(foundUserEntity.getLastmodOn());

        // Now do a midification
        userDao.toggleValidationFlag(foundUserEntity.getEmail(),true);

        // then
        assertNotNull(foundUserEntity.getLastmodOn());

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
