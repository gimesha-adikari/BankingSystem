package com.bankingsystem.core.features.wallet.application;

import com.bankingsystem.core.features.wallet.interfaces.dto.*;
import org.springframework.security.core.Authentication;
import java.util.List;

public interface WalletService {
    List<CardDto> listCards(Authentication auth);
    CreateCardSessionResponse startAddCardSession(Authentication auth, String returnUrl);
    void makeDefault(Authentication auth, String cardId);
    void deleteCard(Authentication auth, String cardId);
    PaymentIntentDto createQrPayment(Authentication auth, QrPaymentRequest req, String idemKey);
    PaymentIntentDto createReloadPayment(Authentication auth, ReloadRequest req, String idemKey);
    PaymentIntentDto createBillPayment(Authentication auth, BillPayRequest req, String idemKey);
    PaymentIntentDto getPaymentIntent(Authentication auth, String intentId);
}
