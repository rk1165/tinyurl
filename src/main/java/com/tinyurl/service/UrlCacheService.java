package com.tinyurl.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Caching service for TinyURL mappings using Redis.
 * <p>
 * Strategy:
 * - Cache key format: "url:{shortUrl}" → longUrl
 * - TTL: 24 hours (configurable)
 * - On cache hit: Refresh TTL (keeps hot URLs in cache longer)
 * - On cache miss: Fetch from DB, populate cache
 * <p>
 * Why this strategy works for TinyURL:
 * 1. URL mappings are IMMUTABLE - once created, shortUrl→longUrl never changes
 * 2. Read-heavy workload - lookups far exceed writes (typically 100:1 or more)
 * 3. Long-tail distribution - some URLs are very hot, most are rarely accessed
 * 4. TTL refresh on access ensures popular URLs stay cached indefinitely
 * 5. Cold URLs naturally expire, freeing memory for hot ones
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UrlCacheService {

    private static final String CACHE_KEY_PREFIX = "url:";

    private final StringRedisTemplate redisTemplate;
    private final Duration cacheTtl;

    /**
     * Get longUrl from cache.
     * If found, refreshes TTL to keep hot URLs cached longer.
     *
     * @param shortUrl the short URL code
     * @return the long URL if cached, null otherwise
     */
    public String get(String shortUrl) {
        String key = CACHE_KEY_PREFIX + shortUrl;
        String longUrl = redisTemplate.opsForValue().get(key);

        if (longUrl != null) {
            // Refresh TTL on access - hot URLs stay in cache
            redisTemplate.expire(key, cacheTtl);
            log.debug("Cache HIT for shortUrl={}", shortUrl);
        } else {
            log.debug("Cache MISS for shortUrl={}", shortUrl);
        }

        return longUrl;
    }

    /**
     * Put shortUrl→longUrl mapping in cache with TTL.
     *
     * @param shortUrl the short URL code
     * @param longUrl  the original long URL
     */
    public void put(String shortUrl, String longUrl) {
        String key = CACHE_KEY_PREFIX + shortUrl;
        redisTemplate.opsForValue().set(key, longUrl, cacheTtl);
        log.debug("Cached shortUrl={} with TTL={}", shortUrl, cacheTtl);
    }

    /**
     * Evict a URL from cache (rarely needed since URLs are immutable).
     * Useful for admin operations or testing.
     *
     * @param shortUrl the short URL code to evict
     */
    public void evict(String shortUrl) {
        String key = CACHE_KEY_PREFIX + shortUrl;
        redisTemplate.delete(key);
        log.debug("Evicted shortUrl={} from cache", shortUrl);
    }
}

