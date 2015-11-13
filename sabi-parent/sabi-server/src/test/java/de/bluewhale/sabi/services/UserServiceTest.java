package de.bluewhale.sabi.services;

import de.bluewhale.sabi.configs.AppConfig;
import de.bluewhale.sabi.configs.PersistenceJPAConfig;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.model.UserTo;
import de.bluewhale.sabi.persistence.dao.UserDao;
import de.bluewhale.sabi.persistence.model.UserEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * Business and Persistence Layer Test. Requires a running database.
 * User: Stefan
 * Date: 30.08.15
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = AppConfig.class)
public class UserServiceTest {

    @Autowired
    UserService userService;

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
    public void testCreateUserViaDAO() throws Exception {

        // given
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail("Test@bluewhale.de");
        userEntity.setPassword("Test123");
        userEntity.setValidateToken("abc123");
        userEntity.setId(4711L);

        // when
        userDao.create(userEntity);

        // then
        UserEntity foundUserEntity = userDao.find(userEntity.getId());

        assertEquals(foundUserEntity.getEmail(), userEntity.getEmail());

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
        userEntity.setValidateToken("abc123");
        userEntity.setId(4711l);

        // when
        repository.saveAndFlush(userEntity);

        // then
        UserEntity foundUserEntity = repository.findOne(userEntity.getId());

        assertEquals(foundUserEntity.getEmail(), userEntity.getEmail());

    }*/


    @Test
    public void testRegisterUserViaService() throws Exception {

        UserTo userTo = new UserTo("testservice@bluewhale.de", "NoPass123");

        final ResultTo<UserTo> userToResultTo = userService.registerNewUser(userTo);

        assertNotNull(userToResultTo);
        assertNotNull(userToResultTo.getValue());
        assertNotNull("New user was not issued with a token.", userToResultTo.getValue().getValidateToken());

        // cleanup
        userService.unregisterUserAndClearPersonalData(userToResultTo.getValue().getEmail());

    }
}
