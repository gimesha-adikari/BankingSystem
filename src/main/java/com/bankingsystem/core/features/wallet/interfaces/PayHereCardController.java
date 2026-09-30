package com.bankingsystem.core.features.wallet.interfaces;

import com.bankingsystem.core.features.wallet.config.PayHereProperties;
import com.bankingsystem.core.features.wallet.domain.entity.CardAddSession;
import com.bankingsystem.core.features.wallet.domain.entity.WalletCard;
import com.bankingsystem.core.features.wallet.domain.repository.CardAddSessionRepository;
import com.bankingsystem.core.features.wallet.domain.repository.WalletCardRepository;
import com.bankingsystem.core.features.wallet.integration.payhere.PayHereSignature;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.OffsetDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/wallet/payhere")
@RequiredArgsConstructor
public class PayHereCardController {

    private final PayHereProperties cfg;
    private final CardAddSessionRepository sessions;
    private final WalletCardRepository cards;

    @GetMapping(value = "/preapprove/{sessionId}", produces = MediaType.TEXT_HTML_VALUE)
    public String preapprove(@PathVariable String sessionId) {
        Optional<CardAddSession> s = sessions.findById(sessionId);
        if (s.isEmpty() || s.get().isConsumed()) {
            return "<html><body><p>Session expired.</p></body></html>";
        }

        String action = cfg.isSandbox()
                ? "https://sandbox.payhere.lk/pay/preapprove"
                : "https://www.payhere.lk/pay/preapprove";

        String orderId = sessionId;
        String currency = "LKR";
        double amount = 10.00; // tokenization preauth (voided by PSP per doc)
        DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
        String amountStr = df.format(amount);
        String hash = PayHereSignature.requestHash(cfg.getMerchantId(), orderId, amount, currency, cfg.getMerchantSecret());

        String returnUrl = cfg.getServerBaseUrl() + "/api/v1/wallet/payhere/cards/return?sessionId=" + url(orderId);
        String cancelUrl = cfg.getServerBaseUrl() + "/api/v1/wallet/payhere/cards/cancel?sessionId=" + url(orderId);
        String notifyUrl = cfg.getServerBaseUrl() + "/api/v1/wallet/payhere/notify/preapprove";

        MultiValueMap<String,String> fields = new LinkedMultiValueMap<>();
        fields.add("merchant_id", cfg.getMerchantId());
        fields.add("return_url", returnUrl);
        fields.add("cancel_url", cancelUrl);
        fields.add("notify_url", notifyUrl);
        // Minimal payer details (PayHere requires these fields)
        fields.add("first_name", "Customer");
        fields.add("last_name", "App");
        fields.add("email", "na@example.com");
        fields.add("phone", "0770000000");
        fields.add("address", "N/A");
        fields.add("city", "Colombo");
        fields.add("country", "Sri Lanka");
        fields.add("order_id", orderId);
        fields.add("currency", currency);
        fields.add("amount", amountStr);
        fields.add("hash", hash);

        StringBuilder inputs = new StringBuilder();
        for (Map.Entry<String, List<String>> e : fields.entrySet()) {
            for (String v : e.getValue()) {
                inputs.append("<input type='hidden' name='").append(e.getKey())
                        .append("' value='").append(escape(v)).append("'>");
            }
        }
        return "<html><body onload='document.forms[0].submit()'>"
                + "<form method='post' action='" + action + "'>" + inputs + "</form>"
                + "<p>Redirecting…</p></body></html>";
    }

    @PostMapping(value = "/notify/preapprove", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Transactional
    public String notifyPreapprove(@RequestParam Map<String,String> form) {
        // Verify md5 signature
        if (!PayHereSignature.verifyNotify(form, cfg.getMerchantSecret())) return "invalid";

        String statusCode = form.getOrDefault("status_code", "");
        String orderId    = form.getOrDefault("order_id", "");
        String token      = firstNonEmpty(form.get("customer_token"), form.get("recurring_token"), form.get("token"));
        String method     = form.getOrDefault("method", "CARD");
        String masked     = firstNonEmpty(form.get("masked_card"), form.get("card_no"));
        String expiry     = form.getOrDefault("card_expiry", ""); // MM/YY or MM/YYYY

        if (!"2".equals(statusCode)) { // PayHere: 2 == success
            // Mark session consumed but don't create a card
            sessions.findById(orderId).ifPresent(s -> { s.setConsumed(true); sessions.save(s); });
            return "ignored";
        }

        Optional<CardAddSession> sOpt = sessions.findById(orderId);
        if (sOpt.isEmpty()) return "unknown-session";
        CardAddSession s = sOpt.get();
        if (s.isConsumed()) return "already";

        // Derive brand + last4
        String brand = method;
        String last4 = "";
        if (masked != null) {
            // masked may be like XXXX-XXXX-1234 or **** **** **** 1234
            String digits = masked.replaceAll("[^0-9]", "");
            if (digits.length() >= 4) last4 = digits.substring(digits.length() - 4);
        }

        Integer mm = null, yy = null;
        if (expiry.matches("\\d{2}/\\d{2,4}")) {
            String[] parts = expiry.split("/");
            try {
                mm = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                yy = (y < 100) ? 2000 + y : y;
            } catch (Exception ignored) {}
        }

        // First card becomes default
        boolean makeDefault = !cards.existsByUserIdAndIsDefaultTrue(s.getUserId());

        WalletCard card = WalletCard.builder()
                .id(UUID.randomUUID())
                .userId(s.getUserId())
                .brand(brand)
                .last4(last4)
                .expiryMonth(mm)
                .expiryYear(yy)
                .customerToken(token)
                .isDefault(makeDefault)
                .build();

        cards.save(card);
        s.setConsumed(true);
        sessions.save(s);
        return "OK";
    }

    @GetMapping(value = "/cards/return", produces = MediaType.TEXT_HTML_VALUE)
    public String cardReturn(@RequestParam String sessionId,
                             @RequestParam(required = false) String status,
                             @RequestParam(required = false, name = "status_code") String statusCode) {

        // Always bounce the user back into the app
        String finalStatus = firstNonEmpty(status, statusCode, "2");
        String deeplink = cfg.getAppCardDeeplink() + "?sessionId=" + url(sessionId) + "&status=" + url(finalStatus);

        return "<html><head><meta name='viewport' content='width=device-width, initial-scale=1' /></head>"
                + "<body style='font-family:system-ui,sans-serif'>"
                + "<p>Finishing up…</p>"
                + "<script>location.href='" + escape(deeplink) + "';</script>"
                + "<p>If you are not redirected, <a href='" + escape(deeplink) + "'>tap here</a>.</p>"
                + "</body></html>";
    }

    @GetMapping(value = "/cards/cancel", produces = MediaType.TEXT_HTML_VALUE)
    public String cardCancel(@RequestParam String sessionId) {
        String deeplink = cfg.getAppCardDeeplink() + "?sessionId=" + url(sessionId) + "&status=CANCELED";
        return "<html><body>"
                + "<script>location.href='" + escape(deeplink) + "';</script>"
                + "<p>Cancelled. <a href='" + escape(deeplink) + "'>Back to app</a></p>"
                + "</body></html>";
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&#39;");
    }
    private static String url(String s) {
        return s == null ? "" : URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
    private static String firstNonEmpty(String... v) {
        if (v == null) return "";
        for (String x : v) if (x != null && !x.isBlank()) return x;
        return "";
    }
}
