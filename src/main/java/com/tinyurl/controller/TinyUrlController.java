package com.tinyurl.controller;

import com.tinyurl.model.Request;
import com.tinyurl.repository.UrlRepository;
import com.tinyurl.service.KeyFetchingService;
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
        // TODO : First check if the shortURL is present in Cache. If it is return from Cache
//        log.info("Fetching shortUrl={}", shortUrl);
        // If it's not call the DB and update. Use LRU
        // calls the DB and looks up the value of the short url and returns the response
        String longUrl = urlRepository.incrementAndGetLongUrl(shortUrl);
        if (longUrl == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        // also increment the count
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(Map.of("longUrl", longUrl));
//                .status(HttpStatus.FOUND)
//                .header("Location", longUrl)
//                .build();
    }

}
