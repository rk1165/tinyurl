package com.tinyurl;

public class ApplicationConstants {

    // The character set for Base62 encoding.
    // Order: 0-9, A-Z, a-z
    public static final String BASE62_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    public static final int BASE = BASE62_ALPHABET.length();

    // SQL Queries

    // SQL to atomic increments the counter.
    // This is thread-safe at the database level.
    public static final String UPDATE_CLICK = "UPDATE tiny_urls SET click_count = click_count + 1 WHERE short_url = ?";

    // SQL to retrieve the long_url
    public static final String SELECT_LONG_URL = "SELECT long_url FROM tiny_urls WHERE short_url = ?";

    // SQL to insert a new short url mapping - MySQL
    public static final String INSERT_URLS = "INSERT IGNORE INTO tiny_urls (short_url, long_url) VALUES (?, ?)";

    // SQL to check if a long url is already present
    public static final String SEARCH_LONG_URL = "SELECT short_url FROM tiny_urls WHERE long_url = ?";
}
