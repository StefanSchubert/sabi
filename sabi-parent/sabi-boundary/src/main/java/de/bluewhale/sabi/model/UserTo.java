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

    public UserTo(String pEmail, String pPasswort) {
        this.email = pEmail;
        this.password = pPasswort;
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
