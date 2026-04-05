-- Default test user for development
-- Email: test@example.com
-- Password: password123
-- BCrypt encoded password for "password123"

INSERT INTO users (id, name, email, password, role, created_at) 
VALUES (
    gen_random_uuid(),
    'Test User',
    'test@example.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',  -- BCrypt hash for "password123"
    'ROLE_USER',
    CURRENT_TIMESTAMP
);
