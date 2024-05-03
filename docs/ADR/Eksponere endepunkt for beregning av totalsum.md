# Eksponere endepunkt for beregning av totalsum.md

## Status
Akseptert

## Kontekst
Beregningen slik den er nå blir brukt for å beregne basert på fakturalinjer.
Dersom vi kun trenger å komme fram til en totalsum per år trenger vi ikke å ta hensyn til fakturalinjer og oppdeling av faktura.
Dersom vi skal flytte beregning til API vil dette kreve vesentlig refaktorering.
Foreslår derfor å bare sende inn perioder vi skal beregne for, så regner vi ut totalen for en satt tidsperiode.


## Beslutning
Vi endte opp med å lage ett nytt endepunkt for å eksponere lignende logikk som for beregning av trygdeavgift.
ref TotalBeløpController

## Konsekvenser
Konsekvensen slik den er og kommer til å bli er att beregning egentlig ikke nødvendigvis hører til faktureringskomponenten.
Det kan også være litt forvirrende mtp att vi allerede har en beregningsmodul.

## Referanser
https://jira.adeo.no/browse/MELOSYS-6570