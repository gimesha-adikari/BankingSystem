package com.bankingsystem.core.features.wallet.integration.payhere;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;

public final class PayHereSignature {
    private PayHereSignature() {}

    public static String requestHash(String merchantId, String orderId, double amount, String currency, String merchantSecret) {
        DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
        String amountStr = df.format(amount);
        String secretMd5 = md5(merchantSecret).toUpperCase();
        String raw = merchantId + orderId + amountStr + currency + secretMd5;
        return md5(raw).toUpperCase();
    }

    public static boolean verifyNotify(Map<String, String> form, String merchantSecret) {
        String merchantId = form.get("merchant_id");
        String orderId = form.get("order_id");
        String amount = form.get("payhere_amount");
        String currency = form.get("payhere_currency");
        String statusCode = form.get("status_code");
        String md5sig = form.get("md5sig");
        String secretMd5 = md5(merchantSecret).toUpperCase();
        String raw = merchantId + orderId + amount + currency + statusCode + secretMd5;
        String calc = md5(raw).toUpperCase();
        return calc.equalsIgnoreCase(md5sig);
    }

    private static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(s.getBytes());
            String out = new BigInteger(1, digest).toString(16).toUpperCase();
            StringBuilder sb = new StringBuilder(out);
            while (sb.length() < 32) sb.insert(0, '0');
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
