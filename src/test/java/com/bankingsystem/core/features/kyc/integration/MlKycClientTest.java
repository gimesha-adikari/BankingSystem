package com.bankingsystem.core.features.kyc.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class MlKycClientTest {

    @Test
    void aggregateSendsInternalCredentialAndTrustedSubject() throws Exception {
        AtomicReference<String> header = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/kyc/aggregate", exchange -> respond(exchange, header, body));
        server.start();
        try {
            String secret = "test-service-secret-012345678901234567890123";
            MockEnvironment environment = new MockEnvironment()
                    .withProperty("ML_BASE_URL", "http://127.0.0.1:" + server.getAddress().getPort())
                    .withProperty("BANK_SERVICE_AUTH_SECRET", secret);
            MlWebClientConfig config = new MlWebClientConfig(environment);
            MlKycClient client = new MlKycClient(config.mlWebClient());
            UUID userId = UUID.randomUUID();

            client.aggregate(new MlKycClient.KycAggregateRequest(
                    userId, null, null, null, null, null, Map.of("caseId", "case-1")));

            assertThat(header.get()).isEqualTo(secret);
            assertThat(body.get()).contains(userId.toString());
        } finally {
            server.stop(0);
        }
    }

    private static void respond(HttpExchange exchange,
                                AtomicReference<String> header,
                                AtomicReference<String> body) throws java.io.IOException {
        header.set(exchange.getRequestHeaders().getFirst("X-Bank-Core-Auth"));
        body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        byte[] response = "{\"decision\":\"UNDER_REVIEW\",\"reasons\":[],\"checks\":[]}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.getResponseHeaders().add("X-Request-ID", "request-1");
        exchange.sendResponseHeaders(200, response.length);
        try (var stream = exchange.getResponseBody()) {
            stream.write(response);
        }
    }
}
