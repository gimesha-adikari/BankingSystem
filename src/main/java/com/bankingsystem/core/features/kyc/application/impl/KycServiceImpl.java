package com.bankingsystem.core.features.kyc.application.impl;

import com.bankingsystem.core.features.kyc.domain.repository.KycUploadRepository;
import com.bankingsystem.core.features.kyc.application.KycService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class KycServiceImpl implements KycService {

    private final KycUploadRepository uploads;

    public KycServiceImpl(KycUploadRepository uploads) {
        this.uploads = uploads;
    }

    @Override
    public String handleUploadId(String id) {
        return id;
    }

    @Override
    public boolean validateRequired(Map<String, String> ids) {
        if (ids == null) return false;
        String[] keys = {"doc_front","doc_back","selfie","address"};
        for (String k : keys) {
            String v = ids.get(k);
            if (v == null) return false;
            try {
                if (!uploads.existsById(UUID.fromString(v))) return false;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return true;
    }
}
