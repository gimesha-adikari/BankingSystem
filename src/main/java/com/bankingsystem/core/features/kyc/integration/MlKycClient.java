package com.bankingsystem.core.features.kyc.integration;

import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
public class MlKycClient {

    private final WebClient client;

    public MlKycClient(WebClient mlWebClient) {
        this.client = mlWebClient;
    }

    public KycAggregateResult aggregate(KycAggregateRequest req) {
        var resp = client.post()
                .uri("/api/v1/kyc/aggregate")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(req)
                .retrieve()
                .toEntity(KycAggregateResponse.class)
                .block();

        String requestId = resp.getHeaders().getFirst("X-Request-ID"); // your current code expects this header
        return new KycAggregateResult(resp.getBody(), requestId);
    }

    // ---------- DTOs ----------

    public static final class KycAggregateRequest {
        public String selfie;
        public String docPortraitImage;
        public String docFrontImage;
        public String docBackImage;
        public String addressProofImage;
        public Map<String, String> meta;

        public KycAggregateRequest() {}

        public KycAggregateRequest(String selfie, String docPortraitImage, String docFrontImage, String docBackImage,
                                   String addressProofImage, Map<String, String> meta) {
            this.selfie = selfie;
            this.docPortraitImage = docPortraitImage;
            this.docFrontImage = docFrontImage;
            this.docBackImage = docBackImage;
            this.addressProofImage = addressProofImage;
            this.meta = meta;
        }
    }

    public static final class KycAggregateResponse {
        public String decision;
        public List<String> reasons;
        public List<Check> checks;
        public KycAggregateResponse() {}
    }

    public static final class KycAggregateResult {
        public final KycAggregateResponse body;
        public final String requestId;
        public KycAggregateResult(KycAggregateResponse body, String requestId) {
            this.body = body;
            this.requestId = requestId;
        }
    }

    public static final class Check {
        public String type;
        public Double score;
        public Boolean passed;
        public Map<String, Object> details;
    }

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
