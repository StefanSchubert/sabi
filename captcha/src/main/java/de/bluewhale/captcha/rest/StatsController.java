/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.captcha.rest;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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


    @ApiOperation("/healthcheck")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Service is responsive...I'm alive.", response = HttpStatus.class) })
    @RequestMapping(value = {"/healthcheck"}, method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<String> helloAgain() {

        // Just received a ping request - NOP
        return new ResponseEntity<>("I'm alive :-)", HttpStatus.OK);
    }

}