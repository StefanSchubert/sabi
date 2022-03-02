/*
 * Copyright (c) 2019 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.captcha.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// If you seek an example, see
// http://websystique.com/springmvc/spring-mvc-4-restful-web-services-crud-example-resttemplate/
// and http://www.leveluplunch.com/java/tutorials/014-post-json-to-spring-rest-webservice/


/**
 * Controller offering basic statistics as well as health checks for monitoring with nagios or cacti
 * Author: Stefan Schubert
 * Date: 26.06.17
 */
@RestController
@RequestMapping(value = "api/stats")
public class StatsController {


    @Operation(method = "/healthcheck")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service is responsive...I'm alive.") })
    @RequestMapping(value = {"/healthcheck"}, method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<String> helloAgain() {

        // Just received a ping request - NOP
        return new ResponseEntity<>("I'm alive :-)", HttpStatus.OK);
    }

}