package com.tinyurl.repository;

import com.tinyurl.ApplicationConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Slf4j
public class UrlRepository {

    private final JdbcTemplate jdbcTemplate;

    public UrlRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Updates the click count and retrieves the original Long URL.
     * * @param shortUrl The short code (primary key)
     *
     * @return The long_url if found, or null if the short_url does not exist.
     */
    @Transactional
    public String incrementAndGetLongUrl(String shortUrl) {
        // 1. Attempt to increment the click count first.
        // We use the return value to check if the row actually exists.
        int rowsUpdated = jdbcTemplate.update(ApplicationConstants.UPDATE_CLICK_SQL, shortUrl);

        // If no rows were updated, the short_url is invalid/does not exist.
        if (rowsUpdated == 0) {
            return null;
        }

        // 2. Since the row exists, fetch the long_url.
        try {
            return jdbcTemplate.queryForObject(ApplicationConstants.SELECT_URL_SQL, String.class, shortUrl);
        } catch (EmptyResultDataAccessException e) {
            // This is unlikely to happen given the rowsUpdated check, 
            // but good for safety.
            return null;
        }
    }

    /**
     * Saves a new TinyUrl object to the database.
     * Uses DB defaults for created_at and click_count.
     * * @param tinyUrl The object containing short and long url.
     */
    public void save(String shortUrl, String longUrl) {
        log.info("saving shortUrl={}, longUrl={}", shortUrl, longUrl);
        jdbcTemplate.update(ApplicationConstants.INSERT_URL_SQL, shortUrl, longUrl);
    }
}