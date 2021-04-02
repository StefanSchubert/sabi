/*
 * Copyright (c) 2021 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;

// exclude here means using Custom Errorpage, spring will search resource/template for generic and
// /resource/template/error/404.html for specific error codes
// in case you want to do some more, you may implement an ErrorController
@SpringBootApplication(exclude = {ErrorMvcAutoConfiguration.class})
public class SpringPrimeFacesApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringPrimeFacesApplication.class, args);
    }
}
