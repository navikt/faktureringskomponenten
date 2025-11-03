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

## Migreringsstatus og neste steg

### Status per 2025-11-03

#### ✅ Fullført
- **Test DSL infrastruktur** implementert og testet
  - `TestDsl.kt` - DslMarker annotation
  - `FakturaLinjeTestFactory.kt` - Med ergonomiske aliaser
  - `FakturaTestFactory.kt` - Med nested builder support
  - `FakturaserieTestFactory.kt` - Med automatisk relationship wiring
  - `TestDslExampleTest.kt` - 6 demonstrasjonstester
  - `docs/TEST_DSL.md` - Denne dokumentasjonen

- **AvregningBehandlerTest** - 100% migrert
  - 28 Faktura-objekter bruker forTest DSL
  - 0 gamle Faktura(...) konstruksjoner igjen
  - 136 linjer spart (8% reduksjon)
  - Alle 14 tester passerer

#### 🔄 Påbegynt men ikke fullført
Ingen - tidligere arbeid er fullført

#### 📋 Gjenstående filer for migrering

**Høy prioritet (mest boilerplate):**
1. **AvregningIT.kt** (~482 linjer)
   - Estimat: ~100+ linjer kan spares
   - Mange inline Faktura og Fakturaserie konstruksjoner
   - Bruker også DTO-er (FakturaserieRequestDto, FakturaseriePeriodeDto)
   - Kommando: `grep -c "= Faktura(" src/test/kotlin/.../AvregningIT.kt`

2. **FakturaserieGeneratorTest.kt**
   - Har mange private val fixtures
   - Bruker både Faktura og FakturaseriePeriode
   - Parametriserte tester med mange test cases

3. **FakturaserieControllerIT.kt** / **FakturaserieControllerTest.kt**
   - Bruker DTOer (FakturaserieRequestDto)
   - Kan ha nytte av DTO factories

**Middels prioritet:**
4. **FakturaBestillCronjobTest.kt**
5. **FakturaIntervallPeriodiseringTest.kt**
6. Andre service/controller tester

**Lav prioritet:**
- Små unittester med minimal boilerplate

### Neste steg for ny session

#### Steg 1: Identifiser måltest-fil
```bash
# Finn filer med gamle Faktura-konstruksjoner
grep -r "= Faktura(" src/test/kotlin/ --include="*.kt" -l

# Tell antall per fil
for file in $(grep -r "= Faktura(" src/test/kotlin/ --include="*.kt" -l); do
  echo "$file: $(grep -c "= Faktura(" $file)"
done | sort -t: -k2 -rn
```

#### Steg 2: Analyser fil
```bash
# Åpne filen og se på strukturen
# - Hvor mange Faktura-objekter?
# - Er det private val fixtures eller inline konstruksjoner?
# - Brukes også Fakturaserie eller kun Faktura?
# - Brukes DTOer?
```

#### Steg 3: Migrer systematisk
1. **Legg til import:**
   ```kotlin
   import no.nav.faktureringskomponenten.domain.models.forTest
   ```

2. **Migrer private val fixtures først**
   - Start med enkleste (1 fakturalinje)
   - Deretter mer komplekse (flere linjer)

3. **Migrer inline konstruksjoner i tester**
   - Bruk samme pattern som fixtures

4. **Kjør tester underveis**
   ```bash
   ./gradlew test --tests "TestClassName"
   ```

#### Steg 4: Verifiser og commit
```bash
# Sjekk at ingen gamle konstruksjoner gjenstår
grep -c "= Faktura(" src/test/kotlin/.../TestFile.kt

# Kjør alle tester
./gradlew test

# Commit med norsk melding
git add <files>
git commit -m "Migrer <TestClassName> til test DSL"
```

### Tips og fallgruver

#### ⚠️ BigDecimal presisjon
DSL setter automatisk `.setScale(2)` på alle BigDecimal-verdier for konsistens.
Husk å oppdatere assertions:
```kotlin
// Før
totalbeløp() shouldBe BigDecimal(3000)

// Etter
totalbeløp() shouldBe BigDecimal("3000.00")
```

#### ⚠️ Companion object
Sørg for at Companion object eksisterer på domain-klassene:
```kotlin
companion object
```
Dette trengs for å bruke `DomainClass.forTest { }` syntax.

#### ⚠️ Imports
Ikke glem å importere `forTest` extension function:
```kotlin
import no.nav.faktureringskomponenten.domain.models.forTest
```

#### 💡 Ergonomiske aliaser
Bruk alltid de ergonomiske aliasene når mulig:
- `fra`/`til` i stedet for `periodeFra`/`periodeTil`
- `månedspris` i stedet for `enhetsprisPerManed`
- `bestilt` i stedet for `datoBestilt`

#### 💡 Kun overstyr nødvendig
Ikke spesifiser standardverdier med mindre de er relevante for testen:
```kotlin
// Godt
val faktura = Faktura.forTest {
    status = BESTILT
}

// Unødvendig verbose
val faktura = Faktura.forTest {
    id = null
    status = BESTILT
    referanseNr = ULID.randomULID()
    eksternFakturaNummer = "TEST-123"
    // ...alle defaultverdier...
}
```

### Kommandoer for statussjekk

```bash
# Tell totalt antall forTest bruk i prosjektet
grep -r "\.forTest {" src/test/kotlin/ | wc -l

# Tell gamle Faktura-konstruksjoner
grep -r "= Faktura(" src/test/kotlin/ | wc -l

# Finn alle TestFactory filer
find src/test/kotlin -name "*TestFactory.kt"

# Sjekk testresultater
./gradlew test --console=plain | grep -E "tests completed|FAILED"
```

### Fremtidige utvidelser

#### FakturaseriePeriodeTestFactory
Ikke implementert ennå, men kan være nyttig for tester som lager mange perioder:
```kotlin
// Fremtidig API
val periode = FakturaseriePeriode.forTest {
    fra = "2024-01-01"
    til = "2024-03-31"
    månedspris = 1000
    beskrivelse = "Test periode"
}
```

#### DTO Factories
For controller/IT-tester som bruker DTOer:
```kotlin
// Fremtidig API
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

### Branch og commits

**Branch:** `feature/legg-til-forTest-dsl`

**Commits:**
```
1932b56 Fikse BigDecimal presisjon i TestDslExampleTest
3eabf3e Fullfør migrering av AvregningBehandlerTest til test DSL
d3e546e Migrer AvregningBehandlerTest til test DSL
0fa27c8 Legg til companion object så vi kan bruke Companion.forTest
733c567 Legg til test DSL for enklere opprettelse av testdata
```

**For merge til master:**
```bash
git checkout master
git merge feature/legg-til-forTest-dsl
git push
```

## Support

Ved spørsmål eller ønsker om nye features, kontakt teamet på Slack: #teammelosys
