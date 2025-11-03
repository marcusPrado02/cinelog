CREATE SCHEMA IF NOT EXISTS audit DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL ON audit.* TO 'cinelog'@'%';
GRANT TRIGGER ON `cinelog`.* TO 'cinelog'@'%';
FLUSH PRIVILEGES;
