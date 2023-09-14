ALTER TYPE fakturaserie_status
    ADD VALUE 'ERSTATTET';

ALTER TABLE fakturaserie
    ADD COLUMN erstattet_med INT;

ALTER TABLE fakturaserie
    ADD CONSTRAINT fk_erstattet_med
        FOREIGN KEY (erstattet_med)
            REFERENCES fakturaserie(id)
            ON DELETE CASCADE;
