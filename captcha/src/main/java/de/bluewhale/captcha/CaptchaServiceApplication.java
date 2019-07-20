/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.captcha;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("de.bluewhale.captcha")
public class CaptchaServiceApplication {

    public static void main(final String[] args) {
        SpringApplication.run(CaptchaServiceApplication.class, args);
    }
}
