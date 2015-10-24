package de.bluewhale.sabi.rest.model;

/**
 * Created with IntelliJ IDEA.
 * Author: Stefan Schubert
 * Date: 27.09.15
 */
public class User {

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private String email;
    private String password;

}
