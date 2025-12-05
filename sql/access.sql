-- Drop existing user if any (ignore error if it doesn't exist)
DROP USER IF EXISTS 'tinyurl'@'%';

-- Create for all access patterns
CREATE USER 'tinyurl'@'%' IDENTIFIED BY 'password';

-- Grant privileges
GRANT ALL PRIVILEGES ON tinyurl_db.* TO 'tinyurl'@'%';

FLUSH PRIVILEGES;