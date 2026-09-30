package com.bankingsystem.core.features.kyc.interfaces;

import com.bankingsystem.core.features.kyc.domain.KycCase;
import com.bankingsystem.core.features.kyc.domain.KycCheck;
import com.bankingsystem.core.features.kyc.domain.repository.KycCheckRepository;
import com.bankingsystem.core.features.kyc.interfaces.dto.KycCaseResponse;
import com.bankingsystem.core.features.kyc.interfaces.dto.KycDecisionRequest;
import com.bankingsystem.core.features.kyc.interfaces.dto.PageResponse;
import com.bankingsystem.core.features.kyc.application.KycCaseService;
import com.bankingsystem.core.modules.common.enums.KycStatus;
import com.bankingsystem.core.modules.common.security.CurrentUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;

@RestController
@RequestMapping("/admin/kyc")
@PreAuthorize("hasRole('ADMIN')")
public class KycReviewController {

    private final KycCaseService service;
    private final CurrentUserService currentUserService;
    private final KycCheckRepository checks;
    public KycReviewController(KycCaseService service, CurrentUserService currentUserService, KycCheckRepository checks) {
        this.service = service;
        this.currentUserService = currentUserService;
        this.checks = checks;
    }

    @GetMapping
    public PageResponse<KycCaseResponse> list(@RequestParam(defaultValue = "PENDING") String status,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        KycStatus st = KycStatus.valueOf(status.toUpperCase());
        Page<KycCase> p = service.listByStatus(st, PageRequest.of(page, size));
        return new PageResponse<>(
                page,
                size,
                p.getTotalElements(),
                p.getContent().stream().map(KycCaseResponse::from).collect(Collectors.toList())
        );
    }

    @PostMapping("/{id}/decision")
    public KycCaseResponse decide(@PathVariable String id, @RequestBody KycDecisionRequest req, Authentication auth) {
        KycStatus target = KycStatus.valueOf(req.getDecision().toUpperCase());
        UUID reviewer = currentUserService.requireUserId(auth);
        return KycCaseResponse.from(service.decide(id, target, req.getReason(), reviewer));
    }

    @GetMapping("/{id}/checks")
    public List<KycCheck> checks(@PathVariable String id) {
        return checks.findByCaseId(id);
    }
}
