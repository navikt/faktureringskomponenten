ALTER TABLE faktura_mottatt
    ALTER COLUMN faktura_referanse_nr TYPE INT
        USING faktura_referanse_nr::INT;

ALTER TABLE faktura_mottatt
    ADD CONSTRAINT fk_faktura_referanse_nr
        FOREIGN KEY (faktura_referanse_nr) REFERENCES faktura(id);