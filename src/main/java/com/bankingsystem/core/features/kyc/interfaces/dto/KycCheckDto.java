package com.bankingsystem.core.features.kyc.interfaces.dto;

public record KycCheckDto(String type, Double score, Boolean passed) {}
