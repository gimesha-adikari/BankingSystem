package com.bankingsystem.core.features.kyc.application;

import java.util.Map;

public interface KycService {
    String handleUploadId(String id);
    boolean validateRequired(Map<String, String> ids);
}
