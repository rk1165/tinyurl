CREATE TABLE IF NOT EXISTS tiny_urls
(
    short_url  VARCHAR(12) PRIMARY KEY, -- (short url code)
    long_url    VARCHAR(2048) NOT NULL,  --
    created_at  TIMESTAMP(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    click_count INT           NOT NULL DEFAULT 0

) ENGINE=InnoDB;

