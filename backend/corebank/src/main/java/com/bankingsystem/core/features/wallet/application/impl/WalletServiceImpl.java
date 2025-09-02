package com.bankingsystem.core.features.wallet.application.impl;

import com.bankingsystem.core.features.wallet.application.IdempotencyService;
import com.bankingsystem.core.features.wallet.application.WalletService;
import com.bankingsystem.core.features.wallet.config.PayHereProperties;
import com.bankingsystem.core.features.wallet.domain.PaymentStatus;
import com.bankingsystem.core.features.wallet.domain.PaymentType;
import com.bankingsystem.core.features.wallet.domain.entity.CardAddSession;
import com.bankingsystem.core.features.wallet.domain.entity.PaymentIntent;
import com.bankingsystem.core.features.wallet.domain.entity.WalletCard;
import com.bankingsystem.core.features.wallet.domain.repository.CardAddSessionRepository;
import com.bankingsystem.core.features.wallet.domain.repository.PaymentIntentRepository;
import com.bankingsystem.core.features.wallet.domain.repository.WalletCardRepository;
import com.bankingsystem.core.features.wallet.interfaces.dto.*;
import com.bankingsystem.core.modules.common.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletCardRepository cards;
    private final PaymentIntentRepository intents;
    private final CurrentUserService currentUser;
    private final IdempotencyService idem;
    private final PayHereProperties cfg;
    private final CardAddSessionRepository sessions;

    @Override
    public List<CardDto> listCards(Authentication auth) {
        UUID userId = currentUser.requireUserId(auth);
        return cards.findByUserIdOrderByCreatedAtDesc(userId).stream().map(CardDto::from).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CreateCardSessionResponse startAddCardSession(Authentication auth, String returnUrl) {
        UUID userId = currentUser.requireUserId(auth);
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        CardAddSession s = new CardAddSession();
        s.setId(sessionId);
        s.setUserId(userId);
        s.setCreatedAt(OffsetDateTime.now());
        s.setConsumed(false);
        sessions.save(s);
        String url = cfg.getServerBaseUrl() + "/api/v1/wallet/payhere/preapprove/" + sessionId;
        return CreateCardSessionResponse.builder().sessionId(sessionId).url(url).build();
    }

    @Override
    @Transactional
    public void makeDefault(Authentication auth, String cardId) {
        UUID userId = currentUser.requireUserId(auth);
        UUID id = UUID.fromString(cardId);
        List<WalletCard> my = cards.findByUserIdOrderByCreatedAtDesc(userId);
        for (WalletCard c : my) c.setDefault(c.getId().equals(id));
        cards.saveAll(my);
    }

    @Override
    @Transactional
    public void deleteCard(Authentication auth, String cardId) {
        UUID userId = currentUser.requireUserId(auth);
        UUID id = UUID.fromString(cardId);
        cards.findByIdAndUserId(id, userId).ifPresent(cards::delete);
    }

    @Override
    @Transactional
    public PaymentIntentDto createQrPayment(Authentication auth, QrPaymentRequest req, String idemKey) {
        return idem.withIdempotency(idemKey, req, PaymentIntentDto.class, () ->
                createIntent(auth, PaymentType.QR, req.getAmount(), "QR Payment")
        );
    }

    @Override
    @Transactional
    public PaymentIntentDto createReloadPayment(Authentication auth, ReloadRequest req, String idemKey) {
        return idem.withIdempotency(idemKey, req, PaymentIntentDto.class, () ->
                createIntent(auth, PaymentType.RELOAD, req.getAmount(), "Mobile Reload")
        );
    }

    @Override
    @Transactional
    public PaymentIntentDto createBillPayment(Authentication auth, BillPayRequest req, String idemKey) {
        return idem.withIdempotency(idemKey, req, PaymentIntentDto.class, () ->
                createIntent(auth, PaymentType.BILL, req.getAmount(), "Bill Payment")
        );
    }

    @Override
    public PaymentIntentDto getPaymentIntent(Authentication auth, String intentId) {
        currentUser.requireUserId(auth);
        PaymentIntent p = intents.findById(intentId).orElseThrow();
        return PaymentIntentDto.from(p);
    }

    private PaymentIntentDto createIntent(Authentication auth, PaymentType type, AmountDto amt, String description) {
        UUID userId = currentUser.requireUserId(auth);
        String id = com.bankingsystem.core.features.wallet.domain.entity.PaymentIntent.newId();
        String checkoutUrl = cfg.getServerBaseUrl() + "/api/v1/wallet/payhere/checkout/" + id;
        PaymentIntent p = PaymentIntent.builder()
                .id(id)
                .userId(userId)
                .type(type)
                .status(PaymentStatus.PROCESSING)
                .amountValue(amt.getValue())
                .amountCurrency(amt.getCurrency())
                .description(description)
                .returnUrl(checkoutUrl)
                .build();
        intents.save(p);
        return PaymentIntentDto.from(p);
    }
}
