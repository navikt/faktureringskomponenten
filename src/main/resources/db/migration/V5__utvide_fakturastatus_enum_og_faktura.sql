ALTER TYPE faktura_status
    ADD VALUE 'BETALT';
ALTER TYPE faktura_status
    ADD VALUE 'DELVIS_BETALT';

ALTER TABLE faktura
    ADD COLUMN innbetalt_belop NUMERIC(10, 2);
