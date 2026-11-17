-- V3__seed_admin.sql
-- Development only: inserts one ADMIN user.
-- Password: Admin@123  (BCrypt hash below)
INSERT INTO users (id, full_name, email, password_hash, role, status)
VALUES (
    gen_random_uuid(),
    'System Admin',
    'admin@example.com',
    '$2b$12$8VcxZGl3UhIqb/5r8NsJuOeh.R/VRHqaBqQpPQlCzQ6ZgSfLASGya',
    'ADMIN',
    'ACTIVE'
)
ON CONFLICT (email) DO NOTHING;

