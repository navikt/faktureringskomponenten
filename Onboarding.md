# Onbarding.md

This file provides guidance to devs and agents when working with code in this repository.

## Project Overview

Faktureringskomponenten is a Spring Boot application written in Kotlin that manages recurring invoice orders by creating invoice series (fakturaserier). It integrates with OEBS (Oracle E-Business Suite) via Kafka for invoice processing and payment tracking.

## Development Commands

### Building and Testing
```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "ClassName"

# Run a single test method
./gradlew test --tests "ClassName.methodName"

# Clean build
./gradlew clean build

# Run the application
./gradlew bootRun
```

### Running Locally
1. Start dependencies via melosys-docker-compose: `make start-all`
2. Run melosys-api with `local-mock` profile
3. Run melosys-web with `npm start`
4. Run faktureringskomponenten with `local` profile

## Architecture

### Domain Model

The core domain revolves around three main entities:

- **Fakturaserie** (Invoice Series): The aggregate root containing metadata about a recurring invoice series, including:
  - Reference (referanse): Unique identifier for the series
  - Invoice recipient information (fodselsnummer, fullmektig)
  - Billing interval (MANEDLIG or KVARTAL)
  - Status lifecycle (OPPRETTET → UNDER_BESTILLING → FERDIG/KANSELLERT/ERSTATTET)
  - Collection of Faktura entities

- **Faktura** (Invoice): Individual invoices within a series with:
  - Status (OPPRETTET, BESTILT, KANSELLERT)
  - Collection of FakturaLinje (invoice lines)
  - External status tracking (EksternFakturaStatus)

- **FakturaLinje** (Invoice Line): Line items representing specific billing periods with amounts

### Key Business Logic

**Invoice Series Generation** (`FakturaserieGenerator`):
- Creates new invoice series with periodization based on billing intervals
- Handles invoice series modifications by generating settlement invoices (avregningsfakturaer)
- Generates settlement invoices when periods change to credit/debit differences
- Uses `AvregningBehandler` to calculate adjustments against already ordered invoices

**Periodization** (`FakturaIntervallPeriodisering`):
- Splits date ranges into billing periods based on interval (monthly/quarterly)
- Ensures invoices cover complete billing cycles

**Invoice Ordering** (`FakturaBestillCronjob`):
- Scheduled job that processes invoice series in UNDER_BESTILLING status
- Orders invoices 3 days before their period starts (configurable via `faktura.forste-faktura-offsett-dager`)
- Publishes `FakturaBestiltDto` to Kafka for OEBS processing
- Uses ShedLock for distributed locking

### Integration Points

**Kafka Producers**:
- `kafka-ny-faktura`: Sends ordered invoices to OEBS (oebs-ny-app)
- `kafka-faktura-ikke-betalt`: Notifies melosys-api of unpaid invoices

**Kafka Consumers**:
- `kafka-faktura-status-endret`: Receives invoice status updates from OEBS (handled by `EksternFakturaStatusConsumer`)

**REST APIs**:
- `FakturaserieController`: Create, update, cancel invoice series
- `FakturaController`: Query invoices and status
- `AdminController`: Administrative operations

### Package Structure

- `domain/models`: JPA entities (Fakturaserie, Faktura, FakturaLinje, etc.)
- `domain/repositories`: Spring Data JPA repositories
- `service`: Core business logic
  - `service/avregning`: Settlement calculation logic
  - `service/beregning`: Amount and period calculations
  - `service/cronjob`: Scheduled invoice ordering job
  - `service/integration/kafka`: Kafka producers/consumers
- `controller`: REST API endpoints
  - `controller/dto`: Request/response DTOs
  - `controller/mapper`: DTO to domain mappers
- `config`: Spring configuration (security, JPA, scheduler)

## Technology Stack

- **Framework**: Spring Boot 3.3.13
- **Language**: Kotlin 1.9.10 targeting JVM 17
- **Database**: PostgreSQL 15 with Flyway migrations
- **Messaging**: Kafka (Aiven)
- **Testing**: JUnit 5, Kotest, MockK, Testcontainers
- **Observability**: Micrometer Prometheus, Logstash encoder
- **Security**: NAV token-validation-spring (Azure AD)
- **Scheduling**: ShedLock for distributed task locking
- **Feature Toggles**: Unleash

## Database

- Flyway migrations located in `src/main/resources/db/migration`
- Uses JPA with Hibernate (ddl-auto: none)
- Audit logging enabled (PGAudit) in GCP CloudSQL
- Entity auditing configured via `PersistenceConfig` with `@CreatedDate`, `@LastModifiedDate`

## Testing

- Integration tests use Testcontainers for PostgreSQL and Kafka
- Tests extend base classes that set up test infrastructure
- Use Kotest assertions for fluent assertions
- Architecture tests via ArchUnit in `architecture/ArkitekturTest`

## Deployment

- Deployed to GCP via NAIS
- Configuration in `nais/nais.yml`
- Uses GCP CloudSQL PostgreSQL
- Kafka pool configured per environment
- Azure AD authentication enabled
- 2 replicas in production
