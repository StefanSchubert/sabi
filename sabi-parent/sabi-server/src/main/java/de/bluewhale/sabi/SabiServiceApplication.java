/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
// import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableCaching
@SpringBootApplication
@EntityScan("de.bluewhale.sabi.persistence.model")
@ComponentScan("de.bluewhale.sabi")
public class SabiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SabiServiceApplication.class, args);
    }
}
