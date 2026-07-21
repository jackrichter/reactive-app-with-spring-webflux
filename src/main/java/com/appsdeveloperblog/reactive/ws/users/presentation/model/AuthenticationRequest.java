package com.appsdeveloperblog.reactive.ws.users.presentation.model;

/**
 * Class to accept a User's identifiers like email(used as userName) and password for that user's logging purpose.
 */
public class AuthenticationRequest {

    private String email;
    private  String password;

    public AuthenticationRequest() {
    }

    public AuthenticationRequest(String email, String password) {
        this.email = email;
        this.password = password;
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
}
