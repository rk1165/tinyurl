package com.tinyurl.service;

import com.tinyurl.model.SnowflakeId;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

import static com.tinyurl.ApplicationConstants.SNOWFLAKE_NEXT_ID_URL;

@Service
@Slf4j
public class KeyFetchingService {

    @Value("${snowflake.generator.baseUrl}")
    private String baseUrl;

    @Value("${snowflake.generator.timeout-ms:5000}")
    private int timeoutMs;

    private final WebClient.Builder webClientBuilder;
    private WebClient webClient;

    public KeyFetchingService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    /**
     * Calls the ID generator's /next endpoint with timeout.
     *
     * @return a snowflake id
     * @throws RuntimeException if the service is unavailable or times out
     */
    public SnowflakeId getNextId() {
        log.debug("Getting next id from Snowflake service");

        return webClient.get()
                .uri(SNOWFLAKE_NEXT_ID_URL)
                .retrieve()
                .bodyToMono(SnowflakeId.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .block();
    }

    @PostConstruct
    public void initWebClient() {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }
}
