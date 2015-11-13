package de.bluewhale.sabi.model;

/**
 * DTO of the user
 * User: Stefan
 * Date: 29.08.15
 */
public class UserTo {

    private String email;

    private String password;

    private String validateToken;

    private boolean validated;


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


    public String getValidateToken() {
        return validateToken;
    }


    public void setValidateToken(String validateToken) {
        this.validateToken = validateToken;
    }
}
