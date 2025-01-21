SELECT DISTINCT fs.fodselsnummer,
                fs.fullmektig_organisasjonsnummer,
                f.dato_bestilt,
                fl.totalbelop,
                f.referanse_nr,
                fs.referanse as fakturaserieReferanse
FROM faktura f
         JOIN fakturaserie fs ON f.fakturaserie_id = fs.id
         JOIN (select sum (belop) as totalbelop, faktura_id from faktura_linje group by faktura_id) fl on f.id = fl.faktura_id
WHERE f.status = 'BESTILT'
  AND f.dato_bestilt >= '2023-12-01 00:00:00'
  AND f.dato_bestilt <= '2024-08-31 23:59:59';