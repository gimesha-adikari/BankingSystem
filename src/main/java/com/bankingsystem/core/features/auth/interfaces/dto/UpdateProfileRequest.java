package com.bankingsystem.core.features.auth.interfaces.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {
    private String username;
    private String currentPassword;
    private String firstName;
    private String lastName;
    private String address;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String homeNumber;
    private String workNumber;
    private String officeNumber;
    private String mobileNumber;
    private String email;

}

