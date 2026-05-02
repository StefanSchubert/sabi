/*
 * Copyright (c) 2022 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.rest.controller;

import de.bluewhale.sabi.exception.Message;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.model.ResultTo;
import de.bluewhale.sabi.services.TankService;
import de.bluewhale.sabi.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

import static de.bluewhale.sabi.api.HttpHeader.AUTH_TOKEN;

;

/**
 * Author: Stefan Schubert
 * Date: 16.06.17
 */
@RestController
@RequestMapping(value = "api/tank")
@Slf4j
public class TankController {

// ------------------------------ FIELDS ------------------------------

    @Autowired
    UserService userService;

    @Autowired
    TankService tankService;

// -------------------------- OTHER METHODS --------------------------

    @Operation(method = "List all tanks belonging to calling user. You need to set the token issued by login or registration in the request header field 'Authorization'.")
    @ApiResponses({
            @ApiResponse(responseCode = "202",
                    description = "Success tanks returned."),
            @ApiResponse(responseCode = "401", description = "Unauthorized - request did not contained a valid user token.")
    })
    @RequestMapping(value = {"/list"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<List<AquariumTo>> listUsersTanks(@RequestHeader(name = AUTH_TOKEN, required = true) String token, Principal principal) {
        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.
        log.debug("Request Tank list for {}",principal.getName());
        List<AquariumTo> aquariumToList = tankService.listActiveTanks(principal.getName());
        return new ResponseEntity<>(aquariumToList, HttpStatus.ACCEPTED);
    }

    @Operation(method = "List all tanks (including inactive ones) belonging to calling user. Required e.g. for history views like PlagueCenter. You need to set the token issued by login or registration in the request header field 'Authorization'.")
    @ApiResponses({
            @ApiResponse(responseCode = "202",
                    description = "Success - all tanks (active and inactive) returned."),
            @ApiResponse(responseCode = "401", description = "Unauthorized - request did not contained a valid user token.")
    })
    @RequestMapping(value = {"/list/all"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<List<AquariumTo>> listAllUsersTanks(@RequestHeader(name = AUTH_TOKEN, required = true) String token, Principal principal) {
        log.debug("Request all tanks (incl. inactive) for {}", principal.getName());
        List<AquariumTo> aquariumToList = tankService.listTanks(principal.getName());
        return new ResponseEntity<>(aquariumToList, HttpStatus.ACCEPTED);
    }

    @Operation(method = "Read details of a specific tank. You need to set the token issued by login or registration in the request header field 'Authorization'.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success tank with temperatureAPI-Key returned."),
            @ApiResponse(responseCode = "401", description = "Unauthorized - request did not contained a valid user token.")
    })
    @RequestMapping(value = {"/{id}"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<AquariumTo> fetchUsersTank(@RequestHeader(name = AUTH_TOKEN, required = true) String token,
                                                     @PathVariable(value = "id", required = true)
                                                           @Parameter(name = "id", description = "id of your aquarium..") String id,
                                                     Principal principal) {
        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.
        AquariumTo aquariumTo = tankService.getTank(Long.valueOf(id), principal.getName());

        if (aquariumTo == null) {
            return new ResponseEntity<>(new AquariumTo(), HttpStatus.UNAUTHORIZED);
        }

        return new ResponseEntity<>(aquariumTo, HttpStatus.OK);
    }

    @Operation(method = "Provides an API Key for temperature measurement submission by IoT devices. You need to set the token issued by login or registration in the request header field 'Authorization'.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success APIKey returned."),
            @ApiResponse(responseCode = "400", description = "Could not parse id - must be an integer"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - request did not contained a valid user token, or the tank is not yours.")
    })
    @RequestMapping(value = {"/{id}/tempApiKey"}, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<AquariumTo> getTemperatureAPIKeyForTank(@RequestHeader(name = AUTH_TOKEN, required = true) String token,
                                                     @PathVariable(value = "id", required = true)
                                                     @Parameter(name = "id", description = "id of your aquarium..") String id,
                                                     Principal principal) {
        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.

        ResponseEntity<AquariumTo> responseEntity;
        AquariumTo updatedAquarium;

        try {
            ResultTo<AquariumTo> aquariumToResultTo = tankService.generateAndAssignNewTemperatureApiKey(Long.valueOf(id), principal.getName());

            final Message resultMessage = aquariumToResultTo.getMessage();
            if (Message.CATEGORY.INFO.equals(resultMessage.getType())) {
                updatedAquarium = aquariumToResultTo.getValue();
                responseEntity = new ResponseEntity<>(updatedAquarium, HttpStatus.OK);
            } else {
                String msg = "Unauthorized - request did not contained a valid user token, or the tank with id " + Long.valueOf(id) + " is not yours. ";
                log.warn(msg);
                responseEntity = new ResponseEntity<>(new AquariumTo(), HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            log.warn("Could not process generateAndAssignNewTemperatureApiKey for id = {}",id);
            responseEntity = new ResponseEntity<>(new AquariumTo(), HttpStatus.BAD_REQUEST);
        }

        return responseEntity;
    }

    @Operation(method = "Delete a tank from users profile. You need to set the token issued by login or registration in the request header field 'Authorization'.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Tank deleted"),
            @ApiResponse(responseCode = "409",
                    description = "Tank does not exists or does not belong to requesting user."),
            @ApiResponse(responseCode = "401", description = "Unauthorized - request did not contained a valid user token.")
    })
    @RequestMapping(value = {"/{id}"}, method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> removeTank(@RequestHeader(name = AUTH_TOKEN, required = true) String token,
                                                     @PathVariable(value = "id", required = true)
                                                     @Parameter(name = "id", description = "id of your aquarium..") String id,
                                                     Principal principal) {
        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.
        ResultTo<AquariumTo> resultTo = tankService.removeTank(Long.valueOf(id), principal.getName());

        ResponseEntity<String> responseEntity;

        if (resultTo.getMessage().getType().equals(Message.CATEGORY.INFO)) {
            responseEntity = new ResponseEntity<>("", HttpStatus.OK);
        } else {
            responseEntity = new ResponseEntity<>("", HttpStatus.CONFLICT);
        }

        return responseEntity;
    }

    @Operation(method="Add a new tank to users profile.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created - Remember Id of returned tank if you want to update it afterwards or retrieve it via list operation."),
            @ApiResponse(responseCode = "409", description = "AlreadyCreated - A tank with this Id has already been created. Create double called?."),
            @ApiResponse(responseCode = "401", description = "Unauthorized - request did not contained a valid user token.")
    })
    @RequestMapping(value = {"/create"}, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public ResponseEntity<AquariumTo> registerNewTank(@RequestHeader(name = AUTH_TOKEN, required = true) String token,
                                                      @RequestBody AquariumTo aquariumTo, Principal principal) {
        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.
        ResultTo<AquariumTo> aquariumToResultTo = tankService.registerNewTank(aquariumTo, principal.getName());

        ResponseEntity<AquariumTo> responseEntity;
        final Message resultMessage = aquariumToResultTo.getMessage();
        if (Message.CATEGORY.INFO.equals(resultMessage.getType())) {
            AquariumTo createdAquarium = aquariumToResultTo.getValue();
            responseEntity = new ResponseEntity<>(createdAquarium, HttpStatus.CREATED);
        } else {
            String msg="A Tank with Id " + aquariumTo.getId() + " already exist.";
            log.warn("Cannot create twice: {}",msg);
            responseEntity = new ResponseEntity<>(aquariumTo, HttpStatus.CONFLICT);
        }
        return responseEntity;
    }

    @Operation(method="Update a specific tank. Tank data needs to be provides via json body.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK - Aquarium has been updated"),
            @ApiResponse(responseCode = "409", description = "Something wrong - Tank ID does not exists or something like that."),
            @ApiResponse(responseCode = "401", description = "Unauthorized - request did not contained a valid user token.")
    })
    @RequestMapping(value = {""}, method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public ResponseEntity<AquariumTo> updateTank(@RequestHeader(name = AUTH_TOKEN, required = true) String token,
                                                 @RequestBody AquariumTo aquariumTo, Principal principal) {
        // If we come so far, the JWTAuthenticationFilter has already validated the token,
        // and we can be sure that spring has injected a valid Principal object.
        ResultTo<AquariumTo> aquariumToResultTo = tankService.updateTank(aquariumTo, principal.getName());

        ResponseEntity<AquariumTo> responseEntity;
        final Message resultMessage = aquariumToResultTo.getMessage();
        if (Message.CATEGORY.INFO.equals(resultMessage.getType())) {
            AquariumTo updatedAquarium = aquariumToResultTo.getValue();
            responseEntity = new ResponseEntity<>(updatedAquarium, HttpStatus.OK);
        } else {
            log.warn("Could not update tank: {}",resultMessage.toString());
            responseEntity = new ResponseEntity<>(aquariumTo, HttpStatus.CONFLICT);
        }
        return responseEntity;
    }

    // ---- Photo upload ----

    @Operation(summary = "Upload a photo for an aquarium (max 5 MB, JPEG/PNG/WebP/GIF).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Photo uploaded."),
            @ApiResponse(responseCode = "400", description = "Invalid format or file too large."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadPhoto(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        log.debug("POST /api/tank/{}/photo for {}", id, principal.getName());
        try {
            String resolvedContentType = (file.getContentType() != null) ? file.getContentType() : "image/jpeg";
            byte[] fileBytes = file.getBytes();
            tankService.uploadPhoto(Long.valueOf(id), fileBytes, resolvedContentType, principal.getName());
        } catch (Exception e) {
            log.warn("Photo upload failed for aquarium {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.noContent().build();
    }

    // ---- Photo download ----

    @Operation(summary = "Download the photo of an aquarium (ownership check).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Photo bytes returned."),
            @ApiResponse(responseCode = "401", description = "Unauthorized."),
            @ApiResponse(responseCode = "404", description = "No photo for this aquarium.")
    })
    @GetMapping(value = "/{id}/photo")
    public ResponseEntity<byte[]> getPhoto(
            @PathVariable String id,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        log.debug("GET /api/tank/{}/photo for {}", id, principal.getName());
        byte[] bytes = tankService.getPhotoBytes(Long.valueOf(id), principal.getName());
        if (bytes.length == 0) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CACHE_CONTROL, "max-age=3600");
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"tank-" + id + ".jpg\"");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.IMAGE_JPEG)
                .body(bytes);
    }

    // ---- Photo delete ----

    @Operation(summary = "Delete the photo of an aquarium.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Photo deleted."),
            @ApiResponse(responseCode = "401", description = "Unauthorized.")
    })
    @DeleteMapping(value = "/{id}/photo")
    public ResponseEntity<Void> deletePhoto(
            @PathVariable String id,
            @RequestHeader(name = AUTH_TOKEN, required = true) String token,
            Principal principal) {
        log.debug("DELETE /api/tank/{}/photo for {}", id, principal.getName());
        tankService.deletePhoto(Long.valueOf(id), principal.getName());
        return ResponseEntity.noContent().build();
    }
}