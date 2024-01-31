CREATE INDEX fakturaserie_referanse_idx ON fakturaserie (referanse);
CREATE INDEX fakturaserie_erstattet_med_idx ON fakturaserie (erstattet_med);
CREATE INDEX fakturaserie_status_idx ON fakturaserie (status);

CREATE INDEX faktura_referanse_nr_idx ON faktura (referanse_nr);
CREATE INDEX faktura_status_idx ON faktura (status);

CREATE INDEX faktura_linje_faktura_id_idx ON faktura_linje (faktura_id);