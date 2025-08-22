package com.bankingsystem.core.features.kyc.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

public class KycMlContractIT {

    static WebClient client;

    @BeforeAll
    static void setup() {
        String baseUrl = envOr("ML_BASE_URL", "http://127.0.0.1:8000");
        client = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    static List<Object[]> cases() {
        return List.of(
                new Object[]{"kyc/approve.json",      "APPROVE",      null},
                new Object[]{"kyc/under_review.json", "UNDER_REVIEW", "face_match_below_threshold"},
                new Object[]{"kyc/reject.json",       "REJECT",       "liveness_check_failed"}
        );
    }

    @DisplayName("Python ML contract fixtures")
    @ParameterizedTest(name = "{index}: {0} -> {1}")
    @MethodSource("cases")
    void contract_case(String resourcePath, String expectedDecision, String mustContainReason) throws IOException {
        String json = readResource(resourcePath);

        var entity = client.post()
                .uri("/api/v1/kyc/aggregate")
                .bodyValue(json)
                .retrieve()
                .toEntity(KycAggregateResponse.class)
                .block(Duration.ofSeconds(15));

        assertThat(entity).isNotNull();
        assertThat(entity.getStatusCode().is2xxSuccessful()).isTrue();

        String reqId = entity.getHeaders().getFirst("X-Request-ID");
        assertThat(reqId).isNotBlank();

        var body = entity.getBody();
        assertThat(body).isNotNull();
        assertThat(body.decision).isEqualTo(expectedDecision);
        if (mustContainReason != null) {
            assertThat(body.reasons).contains(mustContainReason);
        }
    }

    private static String envOr(String k, String def) {
        String v = System.getenv(k);
        return (v == null || v.isBlank()) ? def : v;
    }

    private static String readResource(String path) throws IOException {
        try (var is = KycMlContractIT.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) throw new IOException("Missing resource: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
