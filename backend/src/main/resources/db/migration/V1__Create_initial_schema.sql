-- Initial schema creation for Gemini Web App
-- Creates users and request_logs tables with proper indexes and constraints

-- Create users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE,
    google_id VARCHAR(255) UNIQUE,
    is_anonymous BOOLEAN NOT NULL DEFAULT true,
    fingerprint VARCHAR(255),
    password VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Constraints
    CONSTRAINT chk_user_identification CHECK (
        (is_anonymous = true AND fingerprint IS NOT NULL) OR
        (is_anonymous = false AND (email IS NOT NULL OR google_id IS NOT NULL))
    ),
    CONSTRAINT chk_email_format CHECK (
        email IS NULL OR email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'
    )
);

-- Create request_logs table
CREATE TABLE request_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_prompt TEXT NOT NULL,
    system_prompt TEXT NOT NULL,
    ai_response TEXT,
    status VARCHAR(50) NOT NULL CHECK (status IN ('PENDING', 'SUCCESS', 'ERROR')),
    error_message TEXT,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Create indexes for better query performance
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_google_id ON users(google_id);
CREATE INDEX idx_user_fingerprint ON users(fingerprint);
CREATE INDEX idx_user_is_anonymous ON users(is_anonymous);
CREATE INDEX idx_user_created_at ON users(created_at);

CREATE INDEX idx_request_log_user_id ON request_logs(user_id);
CREATE INDEX idx_request_log_status ON request_logs(status);
CREATE INDEX idx_request_log_created_at ON request_logs(created_at);
CREATE INDEX idx_request_log_ip_address ON request_logs(ip_address);
CREATE INDEX idx_request_log_user_created ON request_logs(user_id, created_at);

-- Create composite indexes for common query patterns
CREATE INDEX idx_user_anonymous_fingerprint ON users(is_anonymous, fingerprint) WHERE is_anonymous = true;
CREATE INDEX idx_user_registered_email ON users(is_anonymous, email) WHERE is_anonymous = false;
CREATE INDEX idx_request_log_user_status ON request_logs(user_id, status);

-- Add comments for documentation
COMMENT ON TABLE users IS 'Stores both anonymous and registered user information';
COMMENT ON COLUMN users.id IS 'Primary key UUID for user identification';
COMMENT ON COLUMN users.email IS 'Email address for registered users (unique)';
COMMENT ON COLUMN users.google_id IS 'Google OAuth ID for OAuth users (unique)';
COMMENT ON COLUMN users.is_anonymous IS 'Flag indicating if user is anonymous or registered';
COMMENT ON COLUMN users.fingerprint IS 'Browser fingerprint for anonymous user tracking';
COMMENT ON COLUMN users.created_at IS 'Timestamp when user record was created';
COMMENT ON COLUMN users.updated_at IS 'Timestamp when user record was last updated';

COMMENT ON TABLE request_logs IS 'Audit log of all AI requests and responses';
COMMENT ON COLUMN request_logs.id IS 'Primary key UUID for request log entry';
COMMENT ON COLUMN request_logs.user_id IS 'Foreign key reference to users table';
COMMENT ON COLUMN request_logs.user_prompt IS 'Original prompt submitted by user';
COMMENT ON COLUMN request_logs.system_prompt IS 'System prompt combined with user prompt';
COMMENT ON COLUMN request_logs.ai_response IS 'Response received from Gemini AI API';
COMMENT ON COLUMN request_logs.status IS 'Status of the request (PENDING, SUCCESS, ERROR)';
COMMENT ON COLUMN request_logs.error_message IS 'Error message if request failed';
COMMENT ON COLUMN request_logs.ip_address IS 'IP address of the requesting client';
COMMENT ON COLUMN request_logs.user_agent IS 'User agent string from client browser';
COMMENT ON COLUMN request_logs.created_at IS 'Timestamp when request was made';