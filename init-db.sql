-- Database initialization script for PostgreSQL
-- This script runs when the PostgreSQL container starts for the first time

-- Create the main database if it doesn't exist
-- (This is handled by POSTGRES_DB environment variable)

-- Create additional databases for different environments if needed
CREATE DATABASE IF NOT EXISTS gemini_web_app_test;

-- Grant permissions to the user
GRANT ALL PRIVILEGES ON DATABASE gemini_web_app_dev TO gemini_user;
GRANT ALL PRIVILEGES ON DATABASE gemini_web_app_test TO gemini_user;

-- Enable UUID extension for the databases
\c gemini_web_app_dev;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

\c gemini_web_app_test;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";