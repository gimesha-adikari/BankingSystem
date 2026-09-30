package com.bankingsystem.core.features.kyc;

import com.bankingsystem.core.features.kyc.application.KycAutoReviewOrchestrator;
import com.bankingsystem.core.features.kyc.application.KycCaseService;
import com.bankingsystem.core.features.kyc.domain.KycCase;
import com.bankingsystem.core.features.kyc.domain.repository.KycCaseRepository;
import com.bankingsystem.core.features.kyc.domain.repository.KycCheckRepository;
import com.bankingsystem.core.features.kyc.domain.repository.KycIdemKeyRepository;
import com.bankingsystem.core.features.kyc.interfaces.KycCaseController;
import com.bankingsystem.core.features.kyc.interfaces.dto.KycSubmitRequest;
import com.bankingsystem.core.features.kyc.interfaces.dto.KycSubmitResponse;
import com.bankingsystem.core.modules.common.enums.KycStatus;
import com.bankingsystem.core.modules.common.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class KycDuplicateActiveCaseTest {

    KycCaseService service = mock(KycCaseService.class);
    CurrentUserService currentUserService = mock(CurrentUserService.class);
    KycAutoReviewOrchestrator orchestrator = mock(KycAutoReviewOrchestrator.class);
    KycCheckRepository checks = mock(KycCheckRepository.class);
    KycIdemKeyRepository idemRepo = mock(KycIdemKeyRepository.class);
    KycCaseRepository cases = mock(KycCaseRepository.class);
    Authentication auth = mock(Authentication.class);

    KycCaseController controller;
    UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new KycCaseController(
                service,
                currentUserService,
                orchestrator,
                checks,
                idemRepo,
                cases
        );
        when(currentUserService.requireUserId(auth)).thenReturn(userId);
    }

    @Test
    void activeCaseInUnderReviewReturnsExistingCaseWithoutCreatingDuplicate() {
        KycCase activeCase = new KycCase();
        activeCase.setId(UUID.randomUUID().toString());
        activeCase.setUserId(userId);
        activeCase.setStatus(KycStatus.UNDER_REVIEW);

        when(service.getMyLatest(userId)).thenReturn(Optional.of(activeCase));

        KycSubmitRequest req = new KycSubmitRequest();
        req.setConsent(true);
        req.setDocFrontId(UUID.randomUUID().toString());
        req.setDocBackId(UUID.randomUUID().toString());
        req.setSelfieId(UUID.randomUUID().toString());
        req.setAddressId(UUID.randomUUID().toString());

        KycSubmitResponse response = controller.submit(req, null, auth);

        // When an active case exists, the controller must return existing case details
        assertThat(response.getStatus()).isEqualTo(KycStatus.UNDER_REVIEW.name());
        assertThat(response.getCaseId()).isEqualTo(activeCase.getId());

        // And must NOT submit a duplicate case
        verify(service, never()).submit(any(), any(), any(), any(), any());
    }

    @Test
    void activeCaseInAutoReviewReturnsExistingCaseWithoutCreatingDuplicate() {
        KycCase activeCase = new KycCase();
        activeCase.setId(UUID.randomUUID().toString());
        activeCase.setUserId(userId);
        activeCase.setStatus(KycStatus.AUTO_REVIEW);

        when(service.getMyLatest(userId)).thenReturn(Optional.of(activeCase));

        KycSubmitRequest req = new KycSubmitRequest();
        req.setConsent(true);
        req.setDocFrontId(UUID.randomUUID().toString());
        req.setDocBackId(UUID.randomUUID().toString());
        req.setSelfieId(UUID.randomUUID().toString());
        req.setAddressId(UUID.randomUUID().toString());

        KycSubmitResponse response = controller.submit(req, null, auth);

        assertThat(response.getStatus()).isEqualTo(KycStatus.AUTO_REVIEW.name());
        assertThat(response.getCaseId()).isEqualTo(activeCase.getId());
        verify(service, never()).submit(any(), any(), any(), any(), any());
    }
}
