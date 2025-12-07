package com.tinyurl.controller;

import com.tinyurl.model.Request;
import com.tinyurl.repository.UrlRepository;
import com.tinyurl.service.ClickTrackingService;
import com.tinyurl.service.KeyFetchingService;
import com.tinyurl.service.UrlCacheService;
import com.tinyurl.utils.Base62Encoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

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
    public ResponseEntity<?> post(@RequestBody Request request) {
        String longUrl = request.getLongUrl();
        log.info("Received request for longUrl={}", longUrl);
        // check if it already exists in the DB
        String existingShortUrl = urlRepository.findShortUrlByLongUrl(longUrl);
        if (existingShortUrl != null) {
            log.info("Existing shortUrl={}", existingShortUrl);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Map.of("shortUrl", existingShortUrl));
        }
        // calls the KGS nextId endpoint and get an ID
        long id = keyFetchingService.getNextId().getId();
        // calls base62encoder's encode method to get a short url
        String shortUrl = base62Encoder.encode(id);
        // save the url in the db
        boolean inserted = urlRepository.save(shortUrl, longUrl);
        // if inserted, return the shortened url to the caller
        if (inserted) {
            // Pre-populate cache so first lookup is also a cache hit
            urlCacheService.put(shortUrl, longUrl);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(Map.of("shortUrl", shortUrl));
        } else {
            log.warn("Race condition detected and handled gracefully for longUrl={}.", longUrl);
            String finalShortUrl = urlRepository.findShortUrlByLongUrl(longUrl);

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Map.of("shortUrl", finalShortUrl));
        }
    }

    @GetMapping("/{shortUrl}")
    public ResponseEntity<?> get(@PathVariable("shortUrl") String shortUrl) {
        // 1. First check cache (Redis)
        String longUrl = urlCacheService.get(shortUrl);
        
        if (longUrl != null) {
            // Cache HIT - track click in Redis (batched, flushed to DB periodically)
            clickTrackingService.incrementInRedis(shortUrl);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(Map.of("longUrl", longUrl));
        }
        
        // 2. Cache MISS - fetch from DB (this also increments click count)
        longUrl = urlRepository.incrementAndGetLongUrl(shortUrl);
        if (longUrl == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("shortUrl %s not found",  shortUrl));
        }
        
        // 3. Populate cache for future requests
        urlCacheService.put(shortUrl, longUrl);
        
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Map.of("longUrl", longUrl));
//                .status(HttpStatus.FOUND)
//                .header("Location", longUrl)
//                .build();
    }

}
