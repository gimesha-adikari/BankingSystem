package com.bankingsystem.core.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
