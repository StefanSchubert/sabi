package de.bluewhale.sabi.rest.model;

/**
 * Created with IntelliJ IDEA.
 * Author: Stefan Schubert
 * Date: 27.09.15
 */
public class SabiAuthToken {

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    private String token;

}
