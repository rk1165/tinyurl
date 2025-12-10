package com.tinyurl.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * <p> Cache Strategy for TinyURL:
 * <p>- TTL: 24 hours (URLs are immutable, but we refresh on access for hot URLs)
 * <p>- Eviction: LRU (Least Recently Used) - configure in Redis with maxmemory-policy
 * <p>- Pattern: Cache-Aside with TTL refresh on read hits
 */
@Configuration
public class RedisConfig {

    @Value("${cache.url.ttl-hours:24}")
    private int ttlHours;

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public Duration cacheTtl() {
        return Duration.ofHours(ttlHours);
    }
}

