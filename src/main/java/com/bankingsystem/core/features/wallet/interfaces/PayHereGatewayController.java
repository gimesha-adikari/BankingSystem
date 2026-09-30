package com.bankingsystem.core.features.wallet.interfaces;

import com.bankingsystem.core.features.wallet.config.PayHereProperties;
import com.bankingsystem.core.features.wallet.domain.PaymentStatus;
import com.bankingsystem.core.features.wallet.domain.entity.PaymentIntent;
import com.bankingsystem.core.features.wallet.domain.repository.PaymentIntentRepository;
import com.bankingsystem.core.features.wallet.integration.payhere.PayHereSignature;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/wallet/payhere")
@RequiredArgsConstructor
public class PayHereGatewayController {

    private final PayHereProperties cfg;
    private final PaymentIntentRepository intents;

    @GetMapping(value = "/checkout/{intentId}", produces = MediaType.TEXT_HTML_VALUE)
    public String checkout(@PathVariable String intentId) {
        PaymentIntent p = intents.findById(intentId).orElseThrow();
        String action = cfg.isSandbox() ? "https://sandbox.payhere.lk/pay/checkout" : "https://www.payhere.lk/pay/checkout";
        String orderId = p.getId();
        String items = p.getDescription() != null ? p.getDescription() : "Wallet Payment";
        String currency = p.getAmountCurrency();
        DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
        String amountFormatted = df.format(p.getAmountValue());
        String hash = PayHereSignature.requestHash(cfg.getMerchantId(), orderId, p.getAmountValue(), currency, cfg.getMerchantSecret());
        String returnUrl = cfg.getServerBaseUrl() + "/api/v1/wallet/payhere/return?intentId=" + orderId;
        String cancelUrl = cfg.getServerBaseUrl() + "/api/v1/wallet/payhere/cancel?intentId=" + orderId;
        String notifyUrl = cfg.getServerBaseUrl() + "/api/v1/wallet/payhere/notify";

        MultiValueMap<String,String> fields = new LinkedMultiValueMap<>();
        fields.add("merchant_id", cfg.getMerchantId());
        fields.add("return_url", returnUrl);
        fields.add("cancel_url", cancelUrl);
        fields.add("notify_url", notifyUrl);
        fields.add("first_name", "Customer");
        fields.add("last_name", "App");
        fields.add("email", "na@example.com");
        fields.add("phone", "0770000000");
        fields.add("address", "N/A");
        fields.add("city", "Colombo");
        fields.add("country", "Sri Lanka");
        fields.add("order_id", orderId);
        fields.add("items", items);
        fields.add("currency", currency);
        fields.add("amount", amountFormatted);
        fields.add("hash", hash);

        StringBuilder inputs = new StringBuilder();
        for (Map.Entry<String, java.util.List<String>> e : fields.entrySet()) {
            for (String v : e.getValue()) {
                inputs.append("<input type='hidden' name='").append(e.getKey()).append("' value='").append(escape(v)).append("'>");
            }
        }
        return "<html><body onload='document.forms[0].submit()'><form method='post' action='" + action + "'>" + inputs + "</form><p>Redirecting…</p></body></html>";
    }

    @PostMapping(value = "/notify", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Transactional
    public String notify(@RequestParam Map<String,String> form) {
        if (!PayHereSignature.verifyNotify(form, cfg.getMerchantSecret())) return "invalid";
        String orderId = form.get("order_id");
        String statusCode = form.get("status_code");
        String message = form.getOrDefault("status_message", "");
        return intents.findById(orderId).map(p -> {
            switch (statusCode) {
                case "2" -> p.setStatus(PaymentStatus.SUCCESS);
                case "0" -> p.setStatus(PaymentStatus.PROCESSING);
                case "-1" -> p.setStatus(PaymentStatus.CANCELED);
                case "-2", "-3" -> p.setStatus(PaymentStatus.FAILED);
                default -> p.setStatus(PaymentStatus.FAILED);
            }
            p.setDescription(message);
            intents.save(p);
            return "ok";
        }).orElse("ok");
    }

    @GetMapping(value = "/return", produces = MediaType.TEXT_HTML_VALUE)
    public String ret(@RequestParam(required = false) String intentId, HttpServletResponse resp) {
        String deep = cfg.getAppDeeplink() + (intentId != null ? ("?intentId=" + intentId) : "");
        return "<html><head><meta http-equiv='refresh' content='0;url=" + deep + "'/></head><body><p>Returning to app…</p></body></html>";
    }

    @GetMapping(value = "/cancel", produces = MediaType.TEXT_HTML_VALUE)
    public String cancel(@RequestParam(required = false) String intentId) {
        String deep = cfg.getAppDeeplink() + (intentId != null ? ("?intentId=" + intentId + "&canceled=1") : "");
        return "<html><head><meta http-equiv='refresh' content='0;url=" + deep + "'/></head><body><p>Payment canceled.</p></body></html>";
    }

    private static String escape(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("'","&#39;").replace("\"","&quot;");
    }
}
