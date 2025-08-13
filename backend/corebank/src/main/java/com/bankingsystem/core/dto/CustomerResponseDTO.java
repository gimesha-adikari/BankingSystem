package com.bankingsystem.core.dto;

import com.bankingsystem.core.enums.Gender;
import com.bankingsystem.core.enums.Status;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CustomerResponseDTO {

    private UUID customerId;
    private String firstName;
    private String lastName;
    private Gender gender;
    private String email;
    private String phone;
    private String address;
    private String dateOfBirth;
    private Status status;
    private String createdAt;
    private String updatedAt;
    private String username;
    private UUID userId;

}
