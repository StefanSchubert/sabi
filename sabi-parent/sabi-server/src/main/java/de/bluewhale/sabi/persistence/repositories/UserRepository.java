package de.bluewhale.sabi.persistence.repositories;

import de.bluewhale.sabi.persistence.model.UserEntity;
// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.stereotype.Repository;

/**
 * Created with IntelliJ IDEA.
 * Author: Stefan Schubert
 * Date: 26.09.15
 *
 * THIS WOULD BE THE WAY TO SPARE QUITE A LOT OF BOILERPLATE CODE THROUGH
 * USAGE OF SPRING-DATA-JPA. SADLY IN CURRENT SETUP THIS APPROACH IS NOT
 * WORKING AS THE @AUTOWIRED OF THE REPOSITORY IS NOT WORKING.
 * (USING SPRING-DATA-JPA VERSION 1.9.0.RELEASE)
 * I LEAVE THIS CODE HERE TO TEST IT FROM TIME TO TIME, HAVING IN MIND
 * THAT I WOULD LIKE TO REFACTOR THIS AS SOON AS IT IS WORKING.
 *
 */
public interface UserRepository { // extends JpaRepository<UserEntity, Long> {


    /**
     * Find persons by last name.
     */
    public UserEntity findByEmail(String email);

}
