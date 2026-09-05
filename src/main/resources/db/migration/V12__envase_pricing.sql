-- Replaces the per-combo "amount_to_go" (a separate value that had to be hand-set on
-- every single combo code, and drifted inconsistent — e.g. desayuno_completo only
-- charged +1000 to go despite needing two containers, same as bandeja) with a
-- compositional model: the "para llevar" surcharge is the sum of whichever containers
-- ("envases") the order actually needs — one for the soup, one for the tray — priced
-- once, globally, regardless of meal type. See PricingCalculator.
ALTER TABLE price_settings RENAME COLUMN amount_in_situ TO amount;
ALTER TABLE price_settings DROP COLUMN amount_to_go;

INSERT INTO price_settings (code, label, amount, confirmed) VALUES
    ('ENVASE_SOPA',    'Envase para sopa/caldo (para llevar)', 1000, true),
    ('ENVASE_BANDEJA', 'Envase para bandeja (para llevar)',    1000, true);
