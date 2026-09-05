-- Introduces the OWNER role, above ADMIN: only an OWNER can create/promote to
-- ADMIN or OWNER, and only an OWNER can modify or delete another OWNER account.
-- Per explicit instruction, all existing users are cleared and a single fresh
-- OWNER account is seeded.
DELETE FROM users;

-- fabian.owner@miresta.com / MirestaOwner2026! — log in once, then change the password.
INSERT INTO users (email, password_hash, role, active, created_at) VALUES
    ('fabian.owner@miresta.com', '$2b$10$WRe.lkqO7g40rRWB2ppfR.giG.hQBOEGouu2gKWElXJlhIAqfRXBK', 'OWNER', true, now());
