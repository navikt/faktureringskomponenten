# Faktureringskomponenten

## Utvikling

### Kjør opp prosjekt lokalt

1. Via melosys-docker-compose, kjør opp `make start-all`
2. Kjør melosys-api i `local-mock` profil
3. Kjør melosys-web `npm start`
4. Kjør faktureringskomponenten i `local` profil

### Feil
#### Feilmelding: 401 UNAUTHORIZED  
##### Årsak
Feil URL (view-localhost i stedet for localhost) ble brukt i Faktureringskomponenten sine tester på Ubuntu-maskiner.  

Det er fordi VMWare Horizon Client kan ha lagt inn `127.0.0.1 view-localhost` automatisk. 
Sørg for at riktig URL brukes og at hosts-filen er korrekt konfigurert for å unngå denne feilen.


##### Løsning
Oppdater hosts-filen på Ubuntu-maskinen ved å flytte `127.0.0.1 localhost` til **øverste linje** i `/etc/hosts`-filen.   

Dette vil se slik ut:
```hosts
    127.0.0.1 localhost
    127.0.0.1 view-localhost
    127.0.1.1 <din-maskin>
```

### Gradle

Vi bruker Gradle i dette prosjektet.

> Hvert gradle prosjekt inkluderer en gradlew / gradlew.bat fil som vi kan bruke for å kjøre gradle kommandoer uten å måtte ha det installert lokalt på vår maskin. 
> Hvis du har egen gradle erstatter du `./gradlew` med `gradle`.

Build: `./gradlew build`
Clean: `./gradlew clean`
Test: `./gradlew test`
Run: `./gradlew run`

## Datastruktur

```json5
{
  "fakturaserieReferanse": "MEL-103-123",
  "fodselsnummer": "1234578911",
  "referanseBruker": "Referanse for bruker",
  "referanseNAV": "Referanse for NAV",
  "fullmektig": {
    "fodselsnummer": "1234578911",
    "orgNr": "123456789",
    "kontaktperson": "Ole Brumm"
  },
  "intervall": "KVARTAL",
  "perioder": [
    {
      "enhetsprisPerManed": 10900,
      "startDato": "01.01.2022",
      "sluttDato": "30.04.2022",
      "beskrivelse": "Inntekt: 50.000, Dekning: Pensjonsdel, Sats: 21.8 %"
    },
    {
      "enhetsprisPerManed": 3400,
      "startDato": "01.05.2022",
      "sluttDato": "31.03.2023",
      "beskrivelse": "Inntekt: 50.000, Dekning: Helsedel med rett til syke-/foreldrepenger, Sats: 6.8 %"
    }
  ]
} 
```


```mermaid
flowchart TB
    melosys-api --> melosys-trygdeavgift
    melosys-api --Send fakturaperioder over REST --> faktureringskomponenten
    melosys-api -- Kanseller faktura over REST --> faktureringskomponenten
    melosys-web -- Hent fakturainfo --> faktureringskomponenten
    faktureringskomponenten -. kafka-ny-faktura -.- oebs-ny-app
    oebs-ny-app -. kafka-faktura-status-endret -.- faktureringskomponenten
    faktureringskomponenten -. kafka-faktura-ikke-betalt -.- melosys-api
    oebs-ny-app<-->OEBS
    OEBS---OEBS-DB[(OEBS-DB)]
```

```mermaid
classDiagram
    Fakturaserie "1" --> "1..*" Faktura
    Fakturaserie "PK_id" --> "FK_fakturaserie_id" Faktura
    Fakturaserie "status" -- "PK_fakturaserie_status" Fakturaserie_status
    Faktura "status" -- "PK_fakutra_status" Faktura_status
    Faktura "1" --> "1..*" Faktura_linje
    
    class Fakturaserie {
        id : LONG
        referanse : VARCHAR
        faktura_gjelder : VARCHAR
        fodselsnummer : NUMERIC
        fullmektig_fodselsnummer : NUMERIC
        fullmektig_organisasjonsnummer : VARCHAR
        fullmektig_kontaktperson : VARCHAR
        referanse_bruker : VARCHAR
        referanse_nav : VARCHAR
        startdato : DATE
        sluttdato : DATE
        status : fakturaserie_status
        intervall : fakturaserie_intervall
        opprettet_Tidspunkt : DATE
    }
    class Faktura {
        id : LONG
        fakturaserie_id : INT
        dato_bestilt : DATE
        status : faktura_status
        beskrivelse : VARCHAR
    }
    class Faktura_linje {
        id : LONG
        faktura_id : INT
        periode_fra : DATE
        periode_til : DATE
        beskrivelse : VARCHAR
        belop : NUMERIC(10, 2)
    }
    class Fakturaserie_status {
        <<Enumeration>>
        OPPRETTET
        UNDER_BESTILLING
        KANSELLERT
        ERSTATTET
        FERDIG
    }
    class Fakturaserie_intervall {
        <<Enumeration>>
        MANEDLIG
        KVARTAL
    }
    class Faktura_status {
        <<Enumeration>>
        OPPRETTET
        BESTILT
        KANSELLERT
    }
```

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen [#teammelosys](https://nav-it.slack.com/archives/C92481HSP).