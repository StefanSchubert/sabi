/*
 * Copyright (c) 2023 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.services.MeasurementService;
import de.bluewhale.sabi.services.PlagueCenterService;
import de.bluewhale.sabi.services.TankService;
import de.bluewhale.sabi.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

;

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

    UserService userService;

    @Autowired
    TankService tankService;

    @Autowired
    MeasurementService measurementService;

    @Autowired
    PlagueCenterService plagueCenterService;

    @Operation(method = "Lifeness-Probe. Might be used e.g. by kubernetes to decide if the service is still up and running. "+
    "Deprecated as we are using Spring actuator. Take /actuator/health as healthcheck endpoint instead.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service is responsive...I'm alive.")
    })
    @RequestMapping(value = {"/healthcheck"}, method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    @Deprecated
    public ResponseEntity<String> helloAgain() {
        // Just received a ping request - NOP
        return new ResponseEntity<>("I'm alive :-)", HttpStatus.OK);
    }

    @Operation(method = "Provides the amount of registered tanks. Might be cached and therefore not actual. Will be refreshed automatically from time to time.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "XY (Number of registered Participants)")})
    @RequestMapping(value = {"/participants"}, method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<String> fetchNumberOfParticipants() {
        String amountOfParticipants = userService.fetchAmountOfParticipants();
        return new ResponseEntity<>(amountOfParticipants, HttpStatus.OK);
    }

    @Operation(method = "Provides the amount of registered tanks. Might be cached and therefore not actual. Will be refreshed automatically from time to time.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "XY (Number of tanks)")})
    @RequestMapping(value = {"/tanks"}, method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<String> fetchNumberOfTanks() {
        String amountOfTanks = tankService.fetchAmountOfTanks();
        return new ResponseEntity<>(amountOfTanks, HttpStatus.OK);
    }

    @Operation(method = "Provides the amount of overall stored measurements. Might be cached and therefore not actual. Will be refreshed automatically from time to time.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "XY (Number of measurements)")})
    @RequestMapping(value = {"/measurements"}, method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<String> fetchNumberOfMeasurements() {
        String amountOfMeasurements = measurementService.fetchAmountOfMeasurements();
        return new ResponseEntity<>(amountOfMeasurements, HttpStatus.OK);
    }

    @Operation(method = "Provides the amount of overall stored plague records. Might be cached and therefore not actual. Will be refreshed automatically from time to time.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "XY (Number of plague records)")})
    @RequestMapping(value = {"/plagues"}, method = RequestMethod.GET, produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<String> fetchNumberOfPlagueRecords() {
        String amountOfPlagueRecords = plagueCenterService.fetchAmountOfPlagueRecords();
        return new ResponseEntity<>(amountOfPlagueRecords, HttpStatus.OK);
    }

}