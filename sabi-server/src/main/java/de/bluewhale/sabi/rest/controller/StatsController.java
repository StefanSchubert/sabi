/*
 * Copyright (c) 2020 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.services.MeasurementService;
import de.bluewhale.sabi.services.TankService;
import de.bluewhale.sabi.services.UserService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    UserService userService;

    @Autowired
    TankService tankService;

    @Autowired
    MeasurementService measurementService;

    @ApiOperation(value = "Lifeness-Probe", notes = "Might be used e.g. by kubernetes to decide if the service is still up and running.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Service is responsive...I'm alive.", response = HttpStatus.class)})
    @RequestMapping(value = {"/healthcheck"}, method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody

    public ResponseEntity<String> helloAgain() {

        // Just received a ping request - NOP
        return new ResponseEntity<>("I'm alive :-)", HttpStatus.OK);
    }

    @ApiOperation(value = "Provides the amount of registered tanks.", notes = "Might be cached and therefore not actual. Will be refreshed automatically from time to time.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "XY (Number of registered Participants)", response = HttpStatus.class)})
    @RequestMapping(value = {"/participants"}, method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<String> fetchNumberOfParticipants() {
        String amountOfParticipants = userService.fetchAmountOfParticipants();
        return new ResponseEntity<>(amountOfParticipants, HttpStatus.OK);
    }

    @ApiOperation(value = "Provides the amount of registered tanks.", notes = "Might be cached and therefore not actual. Will be refreshed automatically from time to time.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "XY (Number of tanks)", response = HttpStatus.class)})
    @RequestMapping(value = {"/tanks"}, method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<String> fetchNumberOfTanks() {
        String amountOfTanks = tankService.fetchAmountOfTanks();
        return new ResponseEntity<>(amountOfTanks, HttpStatus.OK);
    }

    @ApiOperation(value = "Provides the amount of overall stores measurements.", notes = "Might be cached and therefore not actual. Will be refreshed automatically from time to time.")
    @ApiResponses({
            @ApiResponse(code = 200, message = "XY (Number of measurements)", response = HttpStatus.class)})
    @RequestMapping(value = {"/measurements"}, method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<String> fetchNumberOfMeasurements() {
        String amountOfMeasurements = measurementService.fetchAmountOfMeasurements();
        return new ResponseEntity<>(amountOfMeasurements, HttpStatus.OK);
    }

}