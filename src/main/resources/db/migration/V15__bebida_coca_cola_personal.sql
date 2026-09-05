-- Real business rule clarified: personal drinks are 3000, EXCEPT a personal Coca-Cola,
-- which is 4000. The old BEBIDA_COCA_COLA_1_5 (matched by an exact "coca-cola-1.5" name
-- that never actually matched the seeded "CocalCola" product) is retired — size-based
-- drinks (1.5L soda, limonada, jugo, etc.) all fall to BEBIDA_DEFAULT now.
INSERT INTO price_settings (code, label, amount, confirmed) VALUES
    ('BEBIDA_COCA_COLA_PERSONAL', 'Coca-Cola personal', 4000, true);

-- Fixes the label an admin edit left on BEBIDA_DEFAULT ("bebida 1.5L") that made it read
-- like a size-specific price when it's actually the catch-all for everything non-personal.
UPDATE price_settings SET label = 'Bebida grande / otras (gaseosa 1.5L, limonada, jugo, etc.)'
WHERE code = 'BEBIDA_DEFAULT';
