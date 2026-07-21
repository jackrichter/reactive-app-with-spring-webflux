package com.appsdeveloperblog.reactive.ws.users.presentation.model;

import java.util.UUID;

/**
 * Class to hold user details we want to return to the client application.
 */
public class UserRest {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
//    private String password;

    public UserRest() {
    }

    public UserRest(UUID id, String firstNane, String lastName, String email/*, String password*/) {
        this.id = id;
        this.firstName = firstNane;
        this.lastName = lastName;
        this.email = email;
//        this.password = password;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

//    public String getPassword() {
//        return password;
//    }
//
//    public void setPassword(String password) {
//        this.password = password;
//    }
}
