package com.tinyurl.service;

import com.tinyurl.model.SnowflakeId;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class KeyFetchingService {

    @Value("${snowflake.generator.baseUrl}")
    private String baseUrl;

    private final WebClient.Builder webClientBuilder;
    private WebClient webClient;

    public KeyFetchingService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public SnowflakeId getNextId() {
        log.info("Getting next id for url={}", baseUrl);

        String uri = "/api/v1/snowflake/next";

        // 1. Specify the method (GET)
        return webClient.get()
                // 2. Specify the URI
                .uri(uri)
                // 3. Retrieve the response
                .retrieve()
                // 4. Convert the response body to a String and block the thread until response is received.
                // In a blocking environment (like a typical MVC controller), block() is necessary.
                .bodyToMono(SnowflakeId.class)
                .block();
    }

    @PostConstruct
    public void initWebClient() {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }
}
