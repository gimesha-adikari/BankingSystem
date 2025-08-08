package com.bankingsystem.core.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Data
@Getter
@Setter
public class UserProfileResponse {
    private UUID userId;
    private String username;
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
    private String roleName;


}
