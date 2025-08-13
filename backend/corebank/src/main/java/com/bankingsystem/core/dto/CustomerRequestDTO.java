package com.bankingsystem.core.dto;

import com.bankingsystem.core.enums.Gender;
import com.bankingsystem.core.enums.Status;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class CustomerRequestDTO {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotNull
    private Gender gender;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String phone;

    private String address;

    @NotNull
    private LocalDate dateOfBirth;

    @NotNull
    private Status status;

    @NotNull
    private UUID userId;

}
