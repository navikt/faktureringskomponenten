ALTER TYPE fakturaserie_status
    ADD VALUE 'ERSTATTET';

ALTER TABLE fakturaserie
    ADD COLUMN erstattet_med INT;
