-- LAMPARA User Account Management System
-- Run this in MySQL / phpMyAdmin before starting the server

CREATE DATABASE IF NOT EXISTS lampara_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE lampara_db;

CREATE TABLE IF NOT EXISTS users (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    first_name   VARCHAR(60)  NOT NULL,
    last_name    VARCHAR(60)  NOT NULL,
    email        VARCHAR(120) NOT NULL UNIQUE,
    username     VARCHAR(60)  NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,          -- BCrypt hash
    is_verified  BOOLEAN      DEFAULT FALSE,
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

