package com.bankingsystem.core.features.kyc.interfaces;

import com.bankingsystem.core.features.kyc.domain.KycCase;
import com.bankingsystem.core.features.kyc.interfaces.dto.KycCaseResponse;
import com.bankingsystem.core.features.kyc.interfaces.dto.KycSubmitRequest;
import com.bankingsystem.core.features.kyc.interfaces.dto.KycSubmitResponse;
import com.bankingsystem.core.modules.common.security.CurrentUserService;
import com.bankingsystem.core.features.kyc.application.KycCaseService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/kyc")
public class KycCaseController {

    private final KycCaseService service;
    private final CurrentUserService currentUserService;

    public KycCaseController(KycCaseService service, CurrentUserService currentUserService) {
        this.service = service;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/submit")
    public KycSubmitResponse submit(@RequestBody KycSubmitRequest req, Authentication auth) {
        if (!req.isConsent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Consent required");
        }
        UUID userId = currentUserService.requireUserId(auth);
        KycCase saved = service.submit(userId, req.getDocFrontId(), req.getDocBackId(), req.getSelfieId(), req.getAddressId());
        return new KycSubmitResponse(saved.getStatus().name(), saved.getId());
    }

    @GetMapping("/me")
    public KycCaseResponse me(Authentication auth) {
        UUID userId = currentUserService.requireUserId(auth);
        KycCase c = service.getMyLatest(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No case"));
        return KycCaseResponse.from(c);
    }
}
