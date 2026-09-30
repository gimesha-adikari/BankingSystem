package com.bankingsystem.core.features.auth.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@AllArgsConstructor
@Getter
@Setter
public class JwtResponse {
    private String token;
    private String username;
    private String role;
}
