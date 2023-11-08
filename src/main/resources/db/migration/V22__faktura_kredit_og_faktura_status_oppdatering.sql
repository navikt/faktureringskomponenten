ALTER TABLE faktura ADD COLUMN kredit_referanse_nr varchar(40);

ALTER TYPE faktura_status
    ADD VALUE 'KLAR_TIL_KREDITERING';

ALTER TYPE faktura_status
    ADD VALUE 'BESTILT_KREDITERING';
