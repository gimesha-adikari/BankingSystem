package com.bankingsystem.core.features.auth.interfaces.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
