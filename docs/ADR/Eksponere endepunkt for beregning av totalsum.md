# Eksponere endepunkt for beregning av totalsum.md

## Status
Foreslått

## Kontekst
Beregningen slik den er nå blir brukt for å beregne basert på fakturalinjer.
Dersom vi kun trenger å komme fram til en totalsum per år trenger vi ikke å ta hensyn til fakturalinjer og oppdeling av faktura.
Dersom vi skal flytte beregning til API vil dette kreve vesentlig refaktorering.
Foreslår derfor å bare sende inn perioder vi skal beregne for, så regner vi ut totalen for en satt tidsperiode.


## Beslutning
[Beskriv beslutningen som ble tatt, inkludert eventuelle alternativer som ble vurdert og årsakene bak beslutningen]

## Konsekvenser
[Beskriv de forventede konsekvensene av beslutningen, inkludert eventuelle potensielle risikoer eller ulemper]
Konsekvensen slik den er og kommer til å bli er att beregning egentlig ikke nødvendigvis hører til faktureringskomponenten.
Det kan også være litt forvirrende mtp att vi allerede har en beregningsmodul.

## Referanser
[Listen over relevante kilder, lenker eller dokumentasjon]

## Relaterte ADR-er
[Liste over eventuelle relaterte ADR-er, hvis aktuelt]