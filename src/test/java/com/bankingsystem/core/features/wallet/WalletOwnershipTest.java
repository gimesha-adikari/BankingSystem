package com.bankingsystem.core.features.wallet;

import com.bankingsystem.core.features.wallet.application.IdempotencyService;
import com.bankingsystem.core.features.wallet.application.impl.WalletServiceImpl;
import com.bankingsystem.core.features.wallet.config.PayHereProperties;
import com.bankingsystem.core.features.wallet.domain.PaymentStatus;
import com.bankingsystem.core.features.wallet.domain.PaymentType;
import com.bankingsystem.core.features.wallet.domain.entity.PaymentIntent;
import com.bankingsystem.core.features.wallet.domain.repository.CardAddSessionRepository;
import com.bankingsystem.core.features.wallet.domain.repository.PaymentIntentRepository;
import com.bankingsystem.core.features.wallet.domain.repository.WalletCardRepository;
import com.bankingsystem.core.modules.common.exceptions.ResourceNotFoundException;
import com.bankingsystem.core.modules.common.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class WalletOwnershipTest {

    WalletCardRepository cardRepo = mock(WalletCardRepository.class);
    PaymentIntentRepository intentRepo = mock(PaymentIntentRepository.class);
    CurrentUserService currentUser = mock(CurrentUserService.class);
    IdempotencyService idem = mock(IdempotencyService.class);
    PayHereProperties cfg = mock(PayHereProperties.class);
    CardAddSessionRepository sessionRepo = mock(CardAddSessionRepository.class);

    WalletServiceImpl service;
    Authentication authA = mock(Authentication.class);
    Authentication authB = mock(Authentication.class);
    UUID userA = UUID.randomUUID();
    UUID userB = UUID.randomUUID();
    String intentId = "abc123intent";

    @BeforeEach
    void setUp() {
        service = new WalletServiceImpl(cardRepo, intentRepo, currentUser, idem, cfg, sessionRepo);
        when(currentUser.requireUserId(authA)).thenReturn(userA);
        when(currentUser.requireUserId(authB)).thenReturn(userB);
    }

    @Test
    void ownerCanRetrieveOwnIntent() {
        PaymentIntent intent = new PaymentIntent();
        intent.setId(intentId);
        intent.setUserId(userA);
        intent.setType(PaymentType.QR);
        intent.setStatus(PaymentStatus.PROCESSING);
        intent.setAmountValue(100.0);
        intent.setAmountCurrency("LKR");
        when(intentRepo.findByIdAndUserId(intentId, userA)).thenReturn(Optional.of(intent));

        var result = service.getPaymentIntent(authA, intentId);

        assertThat(result).isNotNull();
        assertThat(result.getIntentId()).isEqualTo(intentId);
    }

    @Test
    void differentUserCannotRetrieveAnotherUsersIntent() {
        when(intentRepo.findByIdAndUserId(intentId, userB)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPaymentIntent(authB, intentId))
                .isInstanceOf(ResourceNotFoundException.class);

        // Ownership-scoped query used; no plain findById that would expose other user's data
        verify(intentRepo, never()).findById(any());
    }

    @Test
    void nonExistentIntentThrowsNotFound() {
        when(intentRepo.findByIdAndUserId("nonexistent", userA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPaymentIntent(authA, "nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
