package com.bankingsystem.core.features.kyc.integration;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class MlWebClientConfig {

    private static final Logger log = LoggerFactory.getLogger(MlWebClientConfig.class);
    private final Environment environment;

    public MlWebClientConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public WebClient mlWebClient() {
        String baseUrl = environment.getProperty("ML_BASE_URL", getenvOr("ML_BASE_URL", "http://127.0.0.1:8000"));
        String serviceAuthSecret = environment.getProperty("BANK_SERVICE_AUTH_SECRET", "");
        validateServiceAuthSecret(serviceAuthSecret);
        int connectMs = parseIntOr("ML_CONNECT_TIMEOUT_MS", 1000);
        int readMs = parseIntOr("ML_READ_TIMEOUT_MS", 180000);
        int maxMem = parseIntOr("ML_MAX_BODY_BYTES", 20 * 1024 * 1024);

        log.info("ML WebClient baseUrl={} connectMs={} readMs={} maxMem={}", baseUrl, connectMs, readMs, maxMem);

        HttpClient http = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectMs)
                .responseTimeout(Duration.ofMillis(readMs))
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(readMs, TimeUnit.MILLISECONDS)));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Bank-Core-Auth", serviceAuthSecret)
                .clientConnector(new ReactorClientHttpConnector(http))
                .exchangeStrategies(
                        ExchangeStrategies.builder()
                                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(maxMem))
                                .build())
                .build();
    }

    private static void validateServiceAuthSecret(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("BANK_SERVICE_AUTH_SECRET must be configured");
        }
        if (value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32
                || "CHANGE_ME_TO_A_RANDOM_SECRET".equals(value.trim())) {
            throw new IllegalStateException("BANK_SERVICE_AUTH_SECRET is invalid");
        }
    }

    private static String getenvOr(String k, String def) {
        String v = System.getenv(k);
        return (v == null || v.isBlank()) ? def : v;
    }
    private static int parseIntOr(String k, int def) {
        try { return Integer.parseInt(getenvOr(k, String.valueOf(def))); }
        catch (Exception e) { return def; }
    }
}
