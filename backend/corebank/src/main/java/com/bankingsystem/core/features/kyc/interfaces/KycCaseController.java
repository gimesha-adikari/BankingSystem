package com.bankingsystem.core.features.kyc.interfaces;

import com.bankingsystem.core.features.kyc.application.KycAutoReviewOrchestrator;
import com.bankingsystem.core.features.kyc.application.KycCaseService;
import com.bankingsystem.core.features.kyc.domain.KycCase;
import com.bankingsystem.core.features.kyc.domain.KycCheck;
import com.bankingsystem.core.features.kyc.domain.repository.KycCaseRepository;
import com.bankingsystem.core.features.kyc.domain.repository.KycCheckRepository;
import com.bankingsystem.core.features.kyc.interfaces.dto.KycCaseResponse;
import com.bankingsystem.core.features.kyc.interfaces.dto.KycCheckDto;
import com.bankingsystem.core.features.kyc.interfaces.dto.KycSubmitRequest;
import com.bankingsystem.core.features.kyc.interfaces.dto.KycSubmitResponse;
import com.bankingsystem.core.modules.common.security.CurrentUserService;
import com.bankingsystem.core.features.kyc.domain.repository.KycIdemKeyRepository;
import com.bankingsystem.core.features.kyc.domain.KycIdemKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/kyc")
public class KycCaseController {

    private static final Logger log = LoggerFactory.getLogger(KycCaseController.class);
    private final KycCaseService service;
    private final CurrentUserService currentUserService;
    private final KycAutoReviewOrchestrator orchestrator;
    private final KycCheckRepository checks;
    private final KycIdemKeyRepository idemRepo;
    private final KycCaseRepository cases;

    public KycCaseController(KycCaseService service,
                             CurrentUserService currentUserService,
                             KycAutoReviewOrchestrator orchestrator,
                             KycCheckRepository checks,
                             KycIdemKeyRepository idemRepo, KycCaseRepository cases) {
        this.service = service;
        this.currentUserService = currentUserService;
        this.orchestrator = orchestrator;
        this.checks = checks;
        this.idemRepo = idemRepo;
        this.cases = cases;
    }

    @PostMapping("/submit")
    public KycSubmitResponse submit(@RequestBody KycSubmitRequest req,
                                    @RequestHeader(value = "X-Idempotency-Key", required = false) String idemKey,
                                    Authentication auth) {
        if (!req.isConsent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Consent required");
        }
        UUID userId = currentUserService.requireUserId(auth);

        var existingActive = service.getMyLatest(userId)
                .filter(c -> switch (c.getStatus()) {
//                    case PENDING, AUTO_REVIEW, UNDER_REVIEW, NEEDS_MORE_INFO -> true;
                    default -> false;
                });

        if (existingActive.isPresent()) {
            var c = existingActive.get();
            return new KycSubmitResponse(c.getStatus().name(), c.getId());
        }

        if (idemKey != null && !idemKey.isBlank()) {
            var existing = idemRepo.findByUserIdAndIdemKey(userId, idemKey);
            if (existing.isPresent()) {
                var c = service.getMyLatest(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
                return new KycSubmitResponse(c.getStatus().name(), c.getId());
            }
        }

        KycCase saved = service.submit(userId, req.getDocFrontId(), req.getDocBackId(), req.getSelfieId(), req.getAddressId());
        try {
            saved = orchestrator.run(saved.getId());
        } catch (Exception ex) {}

        if (idemKey != null && !idemKey.isBlank()) {
            idemRepo.save(new KycIdemKey(null, userId, idemKey, saved.getId(), Instant.now()));
        }
        return new KycSubmitResponse(saved.getStatus().name(), saved.getId());
    }

    @GetMapping("/me")
    public KycCaseResponse me(Authentication auth) {
        UUID userId = currentUserService.requireUserId(auth);
        KycCase c = service.getMyLatest(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No case"));
        return KycCaseResponse.from(c);
    }

    @GetMapping("/{id}/checks")
    public List<KycCheckDto> checks(@PathVariable String id, Authentication auth) {
        UUID userId = currentUserService.requireUserId(auth);

        KycCase kc = cases.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        boolean isOwner = kc.getUserId().equals(userId);
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isOwner && !isAdmin) throw new ResponseStatusException(HttpStatus.FORBIDDEN);

        return checks.findByCaseId(id).stream()
                .map(k -> new KycCheckDto(k.getType(), k.getScore(), k.getPassed()))
                .toList();
    }



}