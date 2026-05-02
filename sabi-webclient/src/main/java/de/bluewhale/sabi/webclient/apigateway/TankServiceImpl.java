/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi.webclient.apigateway;

import tools.jackson.core.JacksonException;
import de.bluewhale.sabi.api.Endpoint;
import de.bluewhale.sabi.exception.*;
import de.bluewhale.sabi.model.AquariumTo;
import de.bluewhale.sabi.webclient.rest.exceptions.TankMessageCodes;
import de.bluewhale.sabi.webclient.utils.RestHelper;
import jakarta.inject.Named;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Arrays;
import java.util.List;

/**
 * Calls Sabi Backend to manage users aquariums.
 *
 * @author Stefan Schubert
 */
@Named
@RequestScope
@Slf4j
public class TankServiceImpl extends APIServiceImpl implements TankService {

    @Override
    public @NotNull List<AquariumTo> getUsersTanks(@NotNull String pJWTBackendAuthtoken) throws BusinessException {

        String listTankUri = sabiBackendUrl + Endpoint.TANKS.getPath()+"/list";
        List<AquariumTo> tankList;
        ResponseEntity<String> responseEntity = getAPIResponseFor(listTankUri, pJWTBackendAuthtoken, HttpMethod.GET);

        try {
            AquariumTo[] myObjects = objectMapper.readValue(responseEntity.getBody(), AquariumTo[].class);
            tankList = Arrays.asList(myObjects);
        } catch (JacksonException e) {
            log.error(String.format("Didn't understand response from %s got parsing exception %s",listTankUri,e.getMessage()),e.getMessage());
            e.printStackTrace();
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
        return tankList;
    }

    @Override
    public @NotNull List<AquariumTo> getAllUsersTanks(@NotNull String pJWTBackendAuthtoken) throws BusinessException {

        String listAllTankUri = sabiBackendUrl + Endpoint.TANKS.getPath()+"/list/all";
        List<AquariumTo> tankList;
        ResponseEntity<String> responseEntity = getAPIResponseFor(listAllTankUri, pJWTBackendAuthtoken, HttpMethod.GET);

        try {
            AquariumTo[] myObjects = objectMapper.readValue(responseEntity.getBody(), AquariumTo[].class);
            tankList = Arrays.asList(myObjects);
        } catch (JacksonException e) {
            log.error(String.format("Didn't understand response from %s got parsing exception %s",listAllTankUri,e.getMessage()),e.getMessage());
            e.printStackTrace();
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
        return tankList;
    }


    @Override
    public void deleteTankById(@NotNull Long tankId, @NotNull String pJWTBackendAuthtoken) throws BusinessException {
        String tankUri = sabiBackendUrl + Endpoint.TANKS.getPath()+"/"+tankId;

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity;

        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(pJWTBackendAuthtoken);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        try {
            responseEntity = restTemplate.exchange(tankUri, HttpMethod.DELETE, requestEntity, String.class);
            renewBackendToken(responseEntity);
        } catch (RestClientException e) {
            log.error(String.format("Couldn't reach %s",tankUri),e.getLocalizedMessage());
            throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
        }

        if (!responseEntity.getStatusCode().is2xxSuccessful()){
            if (responseEntity.getStatusCode().value()==409) {
                log.warn("Tried to delete non existing tank {}",tankId);
                throw new BusinessException(Message.error(TankMessageCodes.NO_SUCH_TANK));
            }
            if (responseEntity.getStatusCode().value()==401) {
                log.warn("Invalid Token when trying to delete tank {}",tankId);
                throw new BusinessException(Message.error(AuthMessageCodes.TOKEN_EXPIRED));
            }
        }
    }

    @Override
    public String reCreateTemperatureAPIKey(Long tankID, String pJWTBackendAuthtoken) throws BusinessException {
        String requestTempAPIKeyURI = sabiBackendUrl + Endpoint.TANKS.getPath()+"/"+tankID+"/tempApiKey";

        ResponseEntity<String> responseEntity = getAPIResponseFor(requestTempAPIKeyURI,pJWTBackendAuthtoken,HttpMethod.GET);

        if (responseEntity.getStatusCode() != HttpStatusCode.valueOf(200)) {
            log.error(String.format("Couldn't process %s",requestTempAPIKeyURI),responseEntity.getStatusCode());
            throw new BusinessException(TankExceptionCodes.TANK_NOT_FOUND_OR_DOES_NOT_BELONG_TO_USER);
        }

        AquariumTo myTankWithAPIKey;
        try {
            myTankWithAPIKey = objectMapper.readValue(responseEntity.getBody(), AquariumTo.class);
        } catch (JacksonException e) {
            log.error("Couldn't parse servers response",e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }

        return myTankWithAPIKey.getTemperatureApiKey();
    }

    @Override
    public AquariumTo save(AquariumTo tank, String pJWTBackendAuthtoken) throws BusinessException {

        String updateTankURI = sabiBackendUrl + Endpoint.TANKS; // PUT here
        String createTankURI = sabiBackendUrl + Endpoint.TANKS + "/create"; // POST here
        String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(tank);
        } catch (JacksonException e) {
            log.error("Couldn't convert tank object to json: {}",tank);
            e.printStackTrace();
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }


        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity;

        HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(pJWTBackendAuthtoken);
        HttpEntity<String> requestEntity = new HttpEntity<>(requestJson,headers);

        if (tank.getId() == null) {
            // Save case
            try {
                responseEntity = restTemplate.postForEntity(createTankURI, requestEntity, String.class);
                renewBackendToken(responseEntity);
            } catch (RestClientException e) {
                log.error(String.format("Couldn't reach %s",createTankURI),e.getLocalizedMessage());
                throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
            }
            if (!responseEntity.getStatusCode().is2xxSuccessful()){
                if (responseEntity.getStatusCode().equals(HttpStatusCode.valueOf(409))) {
                    log.info("Tried to create the same tank twice. Will be just ignored as we favour idempotent behavior. Tank ID: {}",tank.getId());
                }
                if (responseEntity.getStatusCode().equals(HttpStatusCode.valueOf(401))) {
                    log.warn("Invalid Token when trying to create tank: {}",tank.getId());
                    throw new BusinessException(Message.error(AuthMessageCodes.TOKEN_EXPIRED));
                }
            }
            // Parse response to get server-assigned ID
            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                try {
                    AquariumTo created = objectMapper.readValue(responseEntity.getBody(), AquariumTo.class);
                    return created;
                } catch (JacksonException e) {
                    log.warn("Could not parse create response for tank", e);
                }
            }

        } else {
            // update case
            try {
                responseEntity = restTemplate.exchange(updateTankURI, HttpMethod.PUT, requestEntity, String.class);
                renewBackendToken(responseEntity);
            } catch (RestClientException e) {
                log.error(String.format("Couldn't reach %s",updateTankURI),e.getLocalizedMessage());
                throw new BusinessException(CommonExceptionCodes.NETWORK_ERROR);
            }

            if (!responseEntity.getStatusCode().is2xxSuccessful()){
                if (responseEntity.getStatusCode().value()==409) {
                    log.warn("Tried to update non existing tank or internal error. Tank ID: {}",tank.getId());
                    throw new BusinessException(Message.error(TankMessageCodes.NO_SUCH_TANK));
                }
                if (responseEntity.getStatusCode().value()==401) {
                    log.warn("Invalid Token when trying to update tank: {}",tank.getId());
                    throw new BusinessException(Message.error(AuthMessageCodes.TOKEN_EXPIRED));
                }
            }

        }
        return tank;
    }

    @Override
    public void uploadPhoto(Long aquariumId, byte[] bytes, String contentType, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.TANKS.getPath() + "/" + aquariumId + "/photo";
        RestTemplate restTemplate = new RestTemplate();
        try {
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token, org.springframework.http.MediaType.MULTIPART_FORM_DATA);
            org.springframework.core.io.ByteArrayResource resource =
                    new org.springframework.core.io.ByteArrayResource(bytes) {
                        @Override
                        public String getFilename() {
                            return "photo" + extensionFor(contentType);
                        }
                    };
            org.springframework.util.MultiValueMap<String, Object> parts =
                    new org.springframework.util.LinkedMultiValueMap<>();
            parts.add("file", resource);
            HttpEntity<org.springframework.util.MultiValueMap<String, Object>> requestEntity =
                    new HttpEntity<>(parts, headers);
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, requestEntity, String.class);
            renewBackendToken(response);
        } catch (Exception e) {
            log.error("Failed to upload photo for aquarium {}", aquariumId, e);
            throw new BusinessException(CommonExceptionCodes.INTERNAL_ERROR);
        }
    }

    private String extensionFor(String contentType) {
        if (contentType == null) return ".jpg";
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }

    @Override
    public byte[] getPhoto(Long aquariumId, String token) throws BusinessException {
        String uri = sabiBackendUrl + Endpoint.TANKS.getPath() + "/" + aquariumId + "/photo";
        RestTemplate restTemplate = new RestTemplate();
        try {
            HttpHeaders headers = RestHelper.prepareAuthedHttpHeader(token);
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<byte[]> response = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, byte[].class);
            return response.getBody() != null ? response.getBody() : new byte[0];
        } catch (Exception e) {
            log.warn("Could not load photo for aquarium {}: {}", aquariumId, e.getMessage());
            return new byte[0];
        }
    }

}
