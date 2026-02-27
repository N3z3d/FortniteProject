-- V37: Add soft delete support to users table
-- Enables GDPR-compliant account deletion (NFR-S04)

ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- Partial index for efficient queries on active (non-deleted) users
CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users(deleted_at);
CREATE INDEX IF NOT EXISTS idx_users_active ON users(id) WHERE deleted_at IS NULL;

COMMENT ON COLUMN users.deleted_at IS 'Timestamp when the account was soft-deleted, NULL if active';
