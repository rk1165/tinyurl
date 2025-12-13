package com.tinyurl.service;

import com.tinyurl.metrics.TimedOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;

import static com.tinyurl.ApplicationConstants.CLICK_COUNT_KEY_PREFIX;

/**
 * Service for tracking URL click counts
 * <ul>
 *     <li>Clicks are accumulated in Redis and periodically flushed to DB</li>
 *     <li>Eventually consistent</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ClickTrackingService {

    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Counts are batched and periodically flushed to DB.
     */
    @TimedOperation("incrementInRedis")
    public void incrementInRedis(String shortUrl) {
        String key = CLICK_COUNT_KEY_PREFIX + shortUrl;
        redisTemplate.opsForValue().increment(key);
        log.debug("Incremented Redis click count for shortUrl={}", shortUrl);
    }

    /**
     * Scheduled task: Flush accumulated click counts from Redis to DB.
     * Runs every 60 seconds by default.
     * <p>
     * This batches potentially thousands of individual clicks into
     * a single DB update per URL, dramatically reducing DB load.
     */
    @TimedOperation("flushClicksToDB")
    @Scheduled(fixedRateString = "${click.flush.interval-ms:60000}")
    public void flushClicksToDB() {
        Set<String> keys = redisTemplate.keys(CLICK_COUNT_KEY_PREFIX + "*");

        if (keys == null || keys.isEmpty()) {
            log.warn("No pending click counts to flush");
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

