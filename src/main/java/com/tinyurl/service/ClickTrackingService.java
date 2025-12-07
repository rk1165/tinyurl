package com.tinyurl.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Service for tracking URL click counts.
 * 
 * Two strategies available:
 * 1. incrementAsync() - Immediate async DB update (simple, moderate traffic)
 * 2. incrementInRedis() + flushToDB() - Batch updates via Redis (high traffic)
 * 
 * For high-traffic TinyURL services, use Redis batching:
 * - Clicks are accumulated in Redis (very fast)
 * - Periodically flushed to DB (reduces DB writes by 100x+)
 * - Eventually consistent (acceptable for analytics)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ClickTrackingService {

    private static final String CLICK_COUNT_KEY_PREFIX = "clicks:";
    
    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Option 2: Increment count in Redis (very fast).
     * Counts are batched and periodically flushed to DB.
     * Best for high traffic - reduces DB writes significantly.
     */
    public void incrementInRedis(String shortUrl) {
        String key = CLICK_COUNT_KEY_PREFIX + shortUrl;
        redisTemplate.opsForValue().increment(key);
        log.debug("Incremented Redis click count for shortUrl={}", shortUrl);
    }

    /**
     * Scheduled task: Flush accumulated click counts from Redis to DB.
     * Runs every 60 seconds by default.
     * 
     * This batches potentially thousands of individual clicks into
     * a single DB update per URL, dramatically reducing DB load.
     */
    @Scheduled(fixedRateString = "${click.flush.interval-ms:60000}")
    public void flushClicksToDB() {
        Set<String> keys = redisTemplate.keys(CLICK_COUNT_KEY_PREFIX + "*");
        
        if (keys == null || keys.isEmpty()) {
            return;
        }

        log.info("Flushing {} click counts to DB", keys.size());
        
        for (String key : keys) {
            try {
                // Atomically get and delete the count
                String countStr = redisTemplate.opsForValue().getAndDelete(key);
                
                if (countStr != null) {
                    long count = Long.parseLong(countStr);
                    String shortUrl = key.substring(CLICK_COUNT_KEY_PREFIX.length());
                    
                    // Batch update: add accumulated count
                    jdbcTemplate.update(
                        "UPDATE tiny_urls SET click_count = click_count + ? WHERE short_url = ?",
                        count, shortUrl
                    );
                    
                    log.debug("Flushed {} clicks for shortUrl={}", count, shortUrl);
                }
            } catch (Exception e) {
                log.warn("Failed to flush clicks for key={}: {}", key, e.getMessage());
            }
        }
    }
}

