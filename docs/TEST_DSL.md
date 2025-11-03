# Test DSL for Faktureringskomponenten

## Oversikt

Dette dokumentet beskriver test DSL-en (Domain Specific Language) for faktureringskomponenten. DSL-en gjør det enklere å lage testdata ved å redusere boilerplate og gjøre testene mer lesbare.

## Motivasjon

### Før DSL
```kotlin
val faktura = Faktura(
    id = 1,
    datoBestilt = LocalDate.of(2024, 3, 19),
    status = FakturaStatus.BESTILT,
    eksternFakturaNummer = "123",
    referanseNr = "REF-123",
    fakturaLinje = listOf(
        FakturaLinje(
            id = 3,
            periodeFra = LocalDate.of(2024, 1, 1),
            periodeTil = LocalDate.of(2024, 3, 31),
            beskrivelse = "Test linje",
            antall = BigDecimal(3),
            enhetsprisPerManed = BigDecimal(1000),
            belop = BigDecimal("3000.00"),
        )
    ),
    eksternFakturaStatus = mutableListOf(),
    krediteringFakturaRef = "",
    referertFakturaVedAvregning = null
)
```
**18 linjer, mange irrelevante detaljer for testen**

### Etter DSL
```kotlin
val faktura = Faktura.forTest {
    status = FakturaStatus.BESTILT
    fakturaLinje {
        fra = "2024-01-01"
        til = "2024-03-31"
        månedspris = 1000
    }
}
```
**7 linjer, kun det som er relevant for testen**

## Arkitektur

DSL-en består av fire hovedkomponenter:

### 1. TestDsl.kt
Inneholder `@FaktureringsTestDsl` annotasjonen som hindrer scope-lekkasje i nested builders.

### 2. FakturaLinjeTestFactory.kt
Factory for å lage FakturaLinje med:
- Fornuftige standardverdier
- Ergonomiske aliaser (`fra`, `til`, `månedspris`)
- Automatisk beregning av beløp

### 3. FakturaTestFactory.kt
Factory for å lage Faktura med:
- Builder-pattern for nested fakturalinjer
- Hjelpemetoder som `lagBestiltFaktura()`

### 4. FakturaserieTestFactory.kt
Factory for aggregate root med:
- Full støtte for nested struktur (fakturaserie → faktura → fakturalinje)
- Automatisk wiring av relasjoner
- Hjelpemetoder som `lagFakturaserieMedBestilteFakturaer()`

## Brukerveiledning

### Grunnleggende bruk

#### Enkel entitet
```kotlin
val fakturaLinje = FakturaLinje.forTest {
    fra = "2024-01-01"
    til = "2024-03-31"
    månedspris = 1000
}
```

#### Med nested struktur
```kotlin
val faktura = Faktura.forTest {
    status = FakturaStatus.BESTILT
    fakturaLinje {
        fra = "2024-01-01"
        til = "2024-03-31"
        månedspris = 1000
    }
    fakturaLinje {
        fra = "2024-04-01"
        til = "2024-06-30"
        månedspris = 2000
    }
}
```

#### Komplett aggregat
```kotlin
val fakturaserie = Fakturaserie.forTest {
    intervall = FakturaserieIntervall.KVARTAL
    fra = "2024-01-01"
    til = "2024-12-31"

    faktura {
        status = FakturaStatus.BESTILT
        fakturaLinje {
            fra = "2024-01-01"
            til = "2024-03-31"
            månedspris = 1000
        }
    }

    faktura {
        status = FakturaStatus.OPPRETTET
        fakturaLinje {
            fra = "2024-04-01"
            til = "2024-06-30"
            månedspris = 2000
        }
    }
}
```

### Ergonomiske aliaser

DSL-en tilbyr ergonomiske aliaser for vanlige operasjoner:

| Felt | Alias | Beskrivelse |
|------|-------|-------------|
| `periodeFra` | `fra` | String-basert dato (yyyy-MM-dd) |
| `periodeTil` | `til` | String-basert dato (yyyy-MM-dd) |
| `enhetsprisPerManed` | `månedspris` | Int i stedet for BigDecimal |
| `datoBestilt` | `bestilt` | String-basert dato (yyyy-MM-dd) |
| `startdato` | `fra` | String-basert dato (yyyy-MM-dd) |
| `sluttdato` | `til` | String-basert dato (yyyy-MM-dd) |

### Hjelpemetoder

#### FakturaTestFactory
```kotlin
// Lager en bestilt faktura med én linje
val faktura = FakturaTestFactory.lagBestiltFaktura(
    periodeFra = LocalDate.of(2024, 1, 1),
    periodeTil = LocalDate.of(2024, 3, 31),
    enhetspris = 1000
)
```

#### FakturaserieTestFactory
```kotlin
// Lager en fakturaserie med to bestilte fakturaer
val fakturaserie = FakturaserieTestFactory.lagFakturaserieMedBestilteFakturaer()
```

### Automatisk beregning

DSL-en beregner automatisk felter når det gir mening:

```kotlin
val linje = FakturaLinje.forTest {
    månedspris = 1000
    antall = BigDecimal(3)
    // belop = 3000 beregnes automatisk
}

linje.belop shouldBe BigDecimal(3000)
```

### Automatisk relationship wiring

Relasjoner kobles automatisk:

```kotlin
val fakturaserie = Fakturaserie.forTest {
    faktura {
        // faktura.fakturaserie settes automatisk til parent
    }
}

fakturaserie.faktura[0].fakturaserie shouldBe fakturaserie
```

## Standardverdier

Alle factories har sensible standardverdier som kan overstyres:

### FakturaLinjeTestFactory
- `periodeFra`: 2024-01-01
- `periodeTil`: 2024-03-31
- `enhetsprisPerManed`: 1000
- `antall`: 1
- `beskrivelse`: "Test fakturalinje"

### FakturaTestFactory
- `datoBestilt`: 2024-01-01
- `status`: OPPRETTET
- `referanseNr`: Tilfeldig ULID
- `eksternFakturaNummer`: "TEST-123"

### FakturaserieTestFactory
- `referanse`: "MEL-TEST-123-{random}"
- `fodselsnummer`: "12345678901"
- `startdato`: 2024-01-01
- `sluttdato`: 2024-12-31
- `status`: OPPRETTET
- `intervall`: KVARTAL
- `fakturaGjelderInnbetalingstype`: TRYGDEAVGIFT

## Avanserte bruksmønstre

### Gjenbruke konfigurasjoner
```kotlin
// Definer en konfigurasjon
val q1Config: FakturaLinjeTestFactory.Builder.() -> Unit = {
    fra = "2024-01-01"
    til = "2024-03-31"
}

// Bruk den flere ganger
val linje1 = FakturaLinje.forTest {
    q1Config()
    månedspris = 1000
}

val linje2 = FakturaLinje.forTest {
    q1Config()
    månedspris = 2000
}
```

### Kombinere eksisterende entiteter
```kotlin
val eksisterendeFaktura = Faktura.forTest { /* ... */ }

val fakturaserie = Fakturaserie.forTest {
    leggTilFaktura(eksisterendeFaktura)
    faktura {
        // Ny faktura
    }
}
```

### Avregningsscenarier
```kotlin
val opprinneligFaktura = Faktura.forTest {
    id = 1
    status = FakturaStatus.BESTILT
    eksternFakturaNummer = "123"
    fakturaLinje {
        fra = "2024-01-01"
        til = "2024-03-31"
        månedspris = 3000
    }
}

val avregningsfaktura = Faktura.forTest {
    referertFakturaVedAvregning = opprinneligFaktura
    krediteringFakturaRef = opprinneligFaktura.referanseNr
    fakturaLinje {
        fra = "2024-01-01"
        til = "2024-03-31"
        månedspris = 1000
        avregningForrigeBeloep = BigDecimal(3000)
        avregningNyttBeloep = BigDecimal(4000)
    }
}
```

## Migrering av eksisterende tester

### Før og etter eksempel fra AvregningBehandlerTest

**Før:**
```kotlin
private val faktura2024ForsteKvartal = Faktura(
    id = 1,
    datoBestilt = LocalDate.of(2024, 3, 19),
    status = BESTILT,
    eksternFakturaNummer = "123",
    referanseNr = "REF-123",
    fakturaLinje = listOf(
        FakturaLinje(
            id = 3,
            periodeFra = LocalDate.of(2024, 1, 1),
            periodeTil = LocalDate.of(2024, 3, 31),
            beskrivelse = "Inntekt: X, Dekning: Y, Sats: Z",
            antall = BigDecimal(3),
            enhetsprisPerManed = BigDecimal(1000),
            belop = BigDecimal("3000.00"),
        ),
        FakturaLinje(
            id = 4,
            periodeFra = LocalDate.of(2024, 1, 1),
            periodeTil = LocalDate.of(2024, 3, 31),
            beskrivelse = "Inntekt: X, Dekning: Y, Sats: Z",
            antall = BigDecimal(3),
            enhetsprisPerManed = BigDecimal(2000),
            belop = BigDecimal("6000.00"),
        ),
    ),
    eksternFakturaStatus = mutableListOf(),
    krediteringFakturaRef = "",
    referertFakturaVedAvregning = null,
    fakturaserie = null
)
```

**Etter:**
```kotlin
private val faktura2024ForsteKvartal = Faktura.forTest {
    id = 1
    bestilt = "2024-03-19"
    status = BESTILT
    eksternFakturaNummer = "123"
    fakturaLinje {
        fra = "2024-01-01"
        til = "2024-03-31"
        månedspris = 1000
        antall = BigDecimal(3)
    }
    fakturaLinje {
        fra = "2024-01-01"
        til = "2024-03-31"
        månedspris = 2000
        antall = BigDecimal(3)
    }
}
```

**Resultat: 27 linjer → 16 linjer (40% reduksjon)**

## Beste praksis

### 1. Kun overstyr det som er viktig for testen
```kotlin
// Godt: Tydelig hva som testes
val faktura = Faktura.forTest {
    status = BESTILT
}

// Dårlig: Unødvendige detaljer
val faktura = Faktura.forTest {
    status = BESTILT
    referanseNr = "REF-123"
    eksternFakturaNummer = "EXT-123"
    krediteringFakturaRef = ""
    // ...masse andre felter som ikke påvirker testen
}
```

### 2. Bruk ergonomiske aliaser
```kotlin
// Godt: Lesbart
val linje = FakturaLinje.forTest {
    fra = "2024-01-01"
    til = "2024-03-31"
    månedspris = 1000
}

// Dårlig: Verbose
val linje = FakturaLinje.forTest {
    periodeFra = LocalDate.of(2024, 1, 1)
    periodeTil = LocalDate.of(2024, 3, 31)
    enhetsprisPerManed = BigDecimal(1000)
}
```

### 3. Bruk hjelpemetoder for vanlige scenarioer
```kotlin
// Godt: Gjenbruk
val faktura = FakturaTestFactory.lagBestiltFaktura(
    periodeFra = LocalDate.of(2024, 1, 1),
    periodeTil = LocalDate.of(2024, 3, 31),
    enhetspris = 1000
)

// Dårlig: Duplisering
val faktura = Faktura.forTest {
    status = BESTILT
    fakturaLinje {
        fra = "2024-01-01"
        til = "2024-03-31"
        månedspris = 1000
    }
}
```

### 4. Grupper relatert konfigurasjon
```kotlin
// Godt: Tydelig struktur
val fakturaserie = Fakturaserie.forTest {
    // Serie-konfigurasjon
    intervall = KVARTAL
    fra = "2024-01-01"
    til = "2024-12-31"

    // Faktura 1
    faktura {
        status = BESTILT
        fakturaLinje { /* ... */ }
    }

    // Faktura 2
    faktura {
        status = OPPRETTET
        fakturaLinje { /* ... */ }
    }
}
```

## Videre utvikling

### Prioriterte forbedringer
1. **FakturaseriePeriodeTestFactory** - For å lage perioder enkelt
2. **Flere hjelpemetoder** basert på vanlige testmønstre
3. **Builder for DTOer** - FakturaserieRequestDto, FakturaseriePeriodeDto osv.

### Eksempel på DTO-builder (fremtidig)
```kotlin
val request = FakturaserieRequestDto.forTest {
    fodselsnummer = "12345678901"
    intervall = KVARTAL
    periode {
        fra = "2024-01-01"
        til = "2024-12-31"
        månedspris = 1000
    }
}
```

## Referanser

- Inspirert av test DSL i melosys-api
- Se `TestDslExampleTest.kt` for flere eksempler
- Pattern: Builder pattern med Kotlin DSL

## Support

Ved spørsmål eller ønsker om nye features, kontakt teamet på Slack: #teammelosys
