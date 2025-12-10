package com.tinyurl;

public class ApplicationConstants {

    public static final String BASE62_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    public static final int BASE = BASE62_ALPHABET.length();

    public static final String SNOWFLAKE_NEXT_ID_URL = "/api/v1/snowflake/next";
    public static final int MAX_RETRIES = 3;

    public static final String CACHE_KEY_PREFIX = "url:";
    public static final String CLICK_COUNT_KEY_PREFIX = "clicks:";

    // SQL Queries

    // Query to atomically increment the counter.
    public static final String UPDATE_CLICK = "UPDATE tiny_urls SET click_count = click_count + 1 WHERE short_url = ?";

    // Query to retrieve the long_url
    public static final String SELECT_LONG_URL = "SELECT long_url FROM tiny_urls WHERE short_url = ?";

    // Query to insert a new short url mapping - MySQL
    public static final String INSERT_URLS = "INSERT IGNORE INTO tiny_urls (short_url, long_url) VALUES (?, ?)";

    // Query to check if a long url is already present
    public static final String SEARCH_LONG_URL = "SELECT short_url FROM tiny_urls WHERE long_url = ?";
}
