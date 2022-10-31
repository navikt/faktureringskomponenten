# melosys-fakturering

```mermaid
classDiagram
class faktura {
    id INT
    saksnummer VARCHAR
    belop BIGINT
    opprettetDato DATE
    beskrivelse VARCHAR 240
    sluttDato DATE
    antall_perioder INT
    status VARCHAR
    (PK) PK_id
}

class faktura_periode {
    id INT
    faktura_id INT
    belop BIGINT
    periodeFra DATE
    periodeTil DATE
    beskrivelse VARCHAR 240
    datoOpprettet DATE
    datoSendt DATE
    frist DATE
    status VARCHAR
    datoBetalt DATE
    (PK) id
    (FK) FK_faktura_id
    (FK) FK_status
}

class faktura_status {
    <<enum>>
    OPPRETTET
    ??
    FERDIG
} 
class faktura_periode_status {
    <<enum>>
    OPPRETTET
    SENDT
    PURRET
    BETALT
}

faktura "1" -->  "1..*" faktura_periode
faktura "PK_id" --> "FK_faktura_id" faktura_periode
faktura "FK_status" -- "PK_faktura_status" faktura_status
faktura_periode "FK_status" -- "PK_fakutra_periode_status" faktura_periode_status

```