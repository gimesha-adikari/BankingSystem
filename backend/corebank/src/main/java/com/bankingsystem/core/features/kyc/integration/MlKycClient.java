package com.bankingsystem.core.features.kyc.integration;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
public class MlKycClient {

    private static final String PREFIX = "/api/v1/kyc";
    private final WebClient http;

    public MlKycClient(WebClient mlWebClient) {
        this.http = mlWebClient;
    }

    // -------------------------
    // Aggregate (recommended)
    // -------------------------
    public KycAggregateResult aggregate(KycAggregateRequest req) {
        String idem = UUID.randomUUID().toString();

        ResponseEntity<KycAggregateResponse> ent = http.post()
                .uri(PREFIX + "/aggregate")
                .header("X-Idempotency-Key", idem)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .toEntity(KycAggregateResponse.class)
                // retry ONLY on 5xx/429
                .retryWhen(Retry.max(2).filter(MlKycClient::isRetryable))
                .block(Duration.ofSeconds(15));

        String reqId = ent != null ? ent.getHeaders().getFirst("X-Request-ID") : null;
        KycAggregateResponse body = ent != null ? ent.getBody() : null;
        return new KycAggregateResult(body, reqId);
    }

    // ---------------------------------------------------
    // Simple endpoints (kept for compatibility/debugging)
    // ---------------------------------------------------
    public Result faceMatch(byte[] selfie, byte[] docFront) {
        SimplePayload p = new SimplePayload(selfie, docFront, null, null);
        return postSimple(PREFIX + "/face/match", p);
    }

    public Result liveness(byte[] selfie) {
        SimplePayload p = new SimplePayload(selfie, null, null, null);
        return postSimple(PREFIX + "/liveness", p);
    }

    public Result ocrId(byte[] front, byte[] back) {
        // Python expects docFrontImage/docBackImage here
        SimplePayload p = new SimplePayload(null, null, front, back);
        return postSimple(PREFIX + "/ocr/id", p);
    }

    public Result docClass(byte[] front, byte[] back) {
        SimplePayload p = new SimplePayload(null, null, front, back);
        return postSimple(PREFIX + "/doc/class", p);
    }

    private Result postSimple(String path, SimplePayload payload) {
        return http.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Result.class)
                .onErrorResume(ex -> Mono.just(Result.failed(errorMsg(ex))))
                .block(Duration.ofSeconds(15));
    }

    private static boolean isRetryable(Throwable ex) {
        if (ex instanceof WebClientResponseException we) {
            int s = we.getStatusCode().value();
            return s >= 500 || s == 429;
        }
        return true; // network/timeouts
    }

    private static String errorMsg(Throwable ex) {
        if (ex instanceof WebClientResponseException we) {
            return "HTTP " + we.getStatusCode().value() + " - " + we.getResponseBodyAsString();
        }
        return ex.getMessage();
    }

    // -------------------------
    // DTOs (kept local for now)
    // -------------------------
    /** For simple endpoints; Jackson will base64-encode byte[] in JSON. */
    public record SimplePayload(byte[] selfie, byte[] docFront, byte[] docFrontImage, byte[] docBackImage) {}

    /** Aggregate request DTO matching the Python contract. */
    public static final class KycAggregateRequest {
        public String selfie;            // base64 string
        public String docPortraitImage;  // base64 string (preferred)
        public String docFrontImage;     // base64 string (optional)
        public String docBackImage;      // base64 string (optional)
        public Map<String, String> meta; // e.g., {"countryHint":"LK","docClassHint":"NIC"}

        public KycAggregateRequest() {}
        public KycAggregateRequest(String selfie, String docPortraitImage, String docFrontImage, String docBackImage, Map<String,String> meta) {
            this.selfie = selfie;
            this.docPortraitImage = docPortraitImage;
            this.docFrontImage = docFrontImage;
            this.docBackImage = docBackImage;
            this.meta = meta;
        }
    }

    /** Aggregate response pieces. */
    public static final class Check {
        public String type;
        public Double score;
        public Boolean passed;
        public Map<String,Object> details;
        public Check() {}
    }

    public static final class KycAggregateResponse {
        public String decision;       // APPROVE | UNDER_REVIEW | REJECT
        public List<String> reasons;  // e.g. face_match_below_threshold
        public List<Check> checks;
        public KycAggregateResponse() {}
    }

    /** Wrapper so callers get both body + X-Request-ID. */
    public static final class KycAggregateResult {
        public final KycAggregateResponse body;
        public final String requestId;
        public KycAggregateResult(KycAggregateResponse body, String requestId) {
            this.body = body;
            this.requestId = requestId;
        }
    }

    /** Simple endpoint result (your existing shape). */
    public static final class Result {
        public String type;
        public Double score;
        public Boolean passed;
        public String detailsJson;

        public static Result failed(String message) {
            Result r = new Result();
            r.type = "ERROR";
            r.passed = false;
            r.score = 0.0;
            r.detailsJson = "{\"error\":\"" + message.replace("\"","'") + "\"}";
            return r;
        }
    }
}
