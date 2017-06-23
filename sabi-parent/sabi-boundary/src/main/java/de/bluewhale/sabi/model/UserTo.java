/*
 * Copyright (c) 2017 by Stefan Schubert
 */

package de.bluewhale.sabi.model;

import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * DTO of the user
 * User: Stefan
 * Date: 29.08.15
 */
public class UserTo implements Serializable {

    private Long id;

    public Long getId() {
        return this.id;
    }

    public void setId(Long pId) {
        this.id = pId;
    }

    private String email;

    private String password;

    private String xAuthToken;

    private String captchaCode;

    private boolean validated;


    public UserTo() {
    }

    public UserTo(String pEmail, String pPassword) {
        this.email = pEmail;
        this.password = pPassword;
    }



    public boolean isValidated() {
        return this.validated;
    }


    public void setValidated(final boolean pValidated) {
        validated = pValidated;
    }


    @ApiModelProperty(notes = "Users Email as ID", required = true)
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @ApiModelProperty(notes = "Users secret", required = true)
    public String getPassword() {
        return password;
    }


    public void setPassword(String password) {
        this.password = password;
    }

    @ApiModelProperty(notes = "If set, this token identifies the user and is to be sent as 'Authorization' within the request header on secured api calls.", required = false)
    public String getxAuthToken() {
        return xAuthToken;
    }


    public void setxAuthToken(String validateToken) {
        this.xAuthToken = validateToken;
    }

    @ApiModelProperty(notes = "reserved - will be used for avoiding DOS-attacks in future", required = false)
    public String getCaptchaCode() {
        return captchaCode;
    }


    public void setCaptchaCode(final String pCaptchaCode) {
        captchaCode = pCaptchaCode;
    }
}
