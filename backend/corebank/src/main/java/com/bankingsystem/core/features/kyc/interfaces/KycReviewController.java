package com.bankingsystem.core.features.kyc.interfaces;

import com.bankingsystem.core.features.kyc.domain.KycCase;
import com.bankingsystem.core.modules.common.enums.KycStatus;
import com.bankingsystem.core.features.kyc.interfaces.dto.KycCaseResponse;
import com.bankingsystem.core.features.kyc.interfaces.dto.KycDecisionRequest;
import com.bankingsystem.core.features.kyc.interfaces.dto.PageResponse;
import com.bankingsystem.core.features.kyc.application.KycCaseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/kyc")
@PreAuthorize("hasRole('ADMIN')")
public class KycReviewController {

    private final KycCaseService service;

    public KycReviewController(KycCaseService service) {
        this.service = service;
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
    public KycCaseResponse decide(@PathVariable String id, @RequestBody KycDecisionRequest req) {
        KycStatus target = KycStatus.valueOf(req.getDecision().toUpperCase());
        return KycCaseResponse.from(service.decide(id, target, req.getReason()));
    }
}
