package com.tinyurl.repository;

import com.tinyurl.ApplicationConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import static com.tinyurl.ApplicationConstants.SEARCH_LONG_URL;

@Repository
@Slf4j
public class UrlRepository {

    private final JdbcTemplate jdbcTemplate;

    public UrlRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Updates the click count and retrieves the original Long URL.
     *
     * @param shortUrl The short code (primary key)
     * @return The long_url if found, or null if the short_url does not exist.
     */
    @Transactional
    public String incrementAndGetLongUrl(String shortUrl) {
        // 1. Attempt to increment the click count first.
        // We use the return value to check if the row actually exists.
        int rowsUpdated = jdbcTemplate.update(ApplicationConstants.UPDATE_CLICK, shortUrl);

        // If no rows were updated, the short_url is invalid/does not exist.
        if (rowsUpdated == 0) {
            return null;
        }

        // 2. Since the row exists, fetch the long_url.
        try {
            return jdbcTemplate.queryForObject(ApplicationConstants.SELECT_LONG_URL, String.class, shortUrl);
        } catch (EmptyResultDataAccessException e) {
            // This is unlikely to happen given the rowsUpdated check, 
            // but good for safety.
            return null;
        }
    }

    /**
     * Saves a new shortUrl to the database.
     * Uses DB defaults for created_at and click_count.
     *
     * @param shortUrl encoded url
     * @param longUrl  url for which the encoding was done
     */
    public boolean save(String shortUrl, String longUrl) {
        log.debug("saving shortUrl={}, longUrl={}", shortUrl, longUrl);
        int rowsAffected = jdbcTemplate.update(ApplicationConstants.INSERT_URLS, shortUrl, longUrl);
        return rowsAffected == 1;
    }

    /**
     * Checks if a given long_url is already present in the tiny_urls table.
     *
     * @param longUrl The long URL string to check.
     * @return shortUrl if the long URL is present, null otherwise.
     * @throws DataAccessException if any Spring-related database error occurs.
     */
    public String findShortUrlByLongUrl(String longUrl) throws DataAccessException {
        log.debug("Checking if longUrl={} is already present", longUrl);
        try {
            return jdbcTemplate.queryForObject(SEARCH_LONG_URL, String.class, longUrl);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

}
