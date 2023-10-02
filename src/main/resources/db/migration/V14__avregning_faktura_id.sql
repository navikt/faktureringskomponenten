ALTER TABLE faktura_linje
    ADD COLUMN avregning_faktura_id INT,
    ADD CONSTRAINT fk_avregning_faktura
        FOREIGN KEY (avregning_faktura_id) REFERENCES faktura (id);
