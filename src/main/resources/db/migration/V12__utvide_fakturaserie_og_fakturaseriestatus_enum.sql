ALTER TYPE fakturaserie_status
    ADD VALUE 'ERSTATTET';

ALTER TABLE fakturaserie
    ADD COLUMN erstattet_med INT;

ALTER TABLE fakturaserie
    ADD CONSTRAINT fk_erstattet_med
    FOREIGN KEY (erstattet_med)
    REFERENCES fakturaserie(id)
    ON DELETE CASCADE;
--
-- ALTER TABLE faktura
--     DROP CONSTRAINT fk_fakturaserie, -- Drop the existing constraint
--     ADD CONSTRAINT fk_fakturaserie
--     FOREIGN KEY (fakturaserie_id)
--     REFERENCES fakturaserie (id)
--     ON DELETE CASCADE; -- Add the new constraint with ON DELETE CASCADE
--
-- ALTER TABLE faktura_linje
--     DROP CONSTRAINT fk_faktura,
--     ADD CONSTRAINT fk_faktura
--     FOREIGN KEY (faktura_id)
--     REFERENCES faktura (id)
--     ON DELETE CASCADE;