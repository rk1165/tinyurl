package com.tinyurl.controller;

import com.tinyurl.model.Request;
import com.tinyurl.repository.UrlRepository;
import com.tinyurl.service.ClickTrackingService;
import com.tinyurl.service.KeyFetchingService;
import com.tinyurl.service.UrlCacheService;
import com.tinyurl.utils.Base62Encoder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static com.tinyurl.ApplicationConstants.MAX_RETRIES;

@Slf4j
@RestController
@RequestMapping("/api/v1/tinyurl")
@RequiredArgsConstructor
public class TinyUrlController {

    private final Base62Encoder base62Encoder;
    private final KeyFetchingService keyFetchingService;
    private final UrlRepository urlRepository;
    private final UrlCacheService urlCacheService;
    private final ClickTrackingService clickTrackingService;

    @PostMapping("/shorten")
    public ResponseEntity<?> post(@Valid @RequestBody Request request) {
        String longUrl = request.getLongUrl();
        log.info("Received request for longUrl={}", longUrl);

        // check if it already exists in the DB
        String existingShortUrl = urlRepository.findShortUrlByLongUrl(longUrl);
        if (existingShortUrl != null) {
            log.info("Found existing shortUrl={}", existingShortUrl);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Map.of("shortUrl", existingShortUrl));
        }

        // if it doesn't exist in the DB, call the KGS nextId endpoint and get an ID
        long id = keyFetchingService.getNextId().getId();

        // calls base62encoder's encode method to get a short url
        String shortUrl = base62Encoder.encode(id);

        // save the url in db
        boolean inserted = urlRepository.save(shortUrl, longUrl);

        // if inserted, return the shortened url to the caller
        if (inserted) {
            // Pre-populate cache for first lookup
            urlCacheService.put(shortUrl, longUrl);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(Map.of("shortUrl", shortUrl));
        } else {
            // This can happen if two threads tried to insert the same url and one inserted it and the other didn't.
            // But now there is a possibility of race condition here
            // Suppose thread T1 and T2 tried inserting the same URL. T1 succeeded and T2 didn't.
            // T2 will try to look the value of longUrl thinking it's already inserted in the DB
            // But now suppose T1 hasn't committed the transaction yet, which will result in T2 returning null
            // To fix this we can add a retry mechanism

            log.warn("Race condition detected for longUrl={}. Handling gracefully...", longUrl);
            String finalShortUrl = null;
            int attempts = 0;
            while (finalShortUrl == null && attempts < MAX_RETRIES) {
                finalShortUrl = urlRepository.findShortUrlByLongUrl(longUrl);
                if (finalShortUrl == null) {
                    try {
                        attempts++;
                        Thread.sleep(50L * attempts);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            if (finalShortUrl == null) {
                log.error("Failed to retrieve shortUrl for existing longUrl={} after maximum retries", longUrl);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            log.debug("Found existing shortUrl={} for longUrl={}", finalShortUrl, longUrl);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Map.of("shortUrl", finalShortUrl));
        }
    }

    @GetMapping("/{shortUrl}")
    public ResponseEntity<?> get(@PathVariable("shortUrl") String shortUrl) {
        // First check the cache
        String longUrl = urlCacheService.get(shortUrl);

        // On Cache HIT - track click_count in Redis
        if (longUrl != null) {
            clickTrackingService.incrementInRedis(shortUrl);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Map.of("longUrl", longUrl));
        }

        // On Cache MISS - fetch from DB (this also increments click count)
        longUrl = urlRepository.incrementAndGetLongUrl(shortUrl);

        if (longUrl == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    String.format("Short URL '%s' not found", shortUrl));
        }

        // Populate the cache for future requests - Cache Aside Pattern
        urlCacheService.put(shortUrl, longUrl);

        // I am returning 200 OK for now.
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Map.of("longUrl", longUrl));
        // Return 301 / 302 depending on actual requirement
        /* return ResponseEntity
                .status(HttpStatus.FOUND)
                .header("Location", longUrl)
                .build();*/
    }

}
