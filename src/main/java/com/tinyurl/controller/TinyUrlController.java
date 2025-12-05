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
        // TODO : Create workers which will validate the long url?
        // TODO : Create workers which will generate Base62Encoders
        log.info("Received request for longUrl={}", request.getLongUrl());
        // calls the KGS nextId endpoint and get an ID
        long id = keyFetchingService.getNextId().getId();
        // calls base62encoder's encode method to get a short url
        String shortUrl = base62Encoder.encode(id);
        // save the url in the db
        urlRepository.save(shortUrl, request.getLongUrl());
        // return the shortened url to the caller
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("shortUrl", shortUrl));
    }

    @GetMapping("/{shortUrl}")
    public ResponseEntity<?> get(@PathVariable("shortUrl") String shortUrl) {
        // calls the DB and looks up the value of the short url and returns the response
        String longUrl = urlRepository.incrementAndGetLongUrl(shortUrl);
        if (longUrl == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        // also increment the count
        return ResponseEntity
                .status(HttpStatus.FOUND)
                .header("Location", longUrl)
                .build();
    }

}
