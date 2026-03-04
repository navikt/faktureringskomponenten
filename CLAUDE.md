# CLAUDE.md

Denne filen gir veiledning til Claude Code (claude.ai/code) når den jobber med kode i dette repositoriet.

## Utviklingskommandoer

### Pakkehåndtering
- Bruker kun pnpm: `pnpm install`, `pnpm start`
- Krever GitHub PAT for pakkeregister-tilgang (se README.md for oppsett)

### Utviklingsserver
- `pnpm start` - Lokal utvikling med proxy til localhost:8080
- `pnpm start:q1` - Utvikling mot q1-miljø med Azure AD-autentisering
- `pnpm start:q2` - Utvikling mot q2-miljø med Azure AD-autentisering

### Bygg & Linting
- `pnpm build` - Full produksjonsbygg (CSS + JS)
- `pnpm run eslint` - Kjør ESLint på src/
- `npx tsc` - TypeScript typekontroll
- `pnpm run prettier:check` - Sjekk kodeformatering
- `pnpm run prettier:write` - Formater kode

### Testing
- `npm test` - Kjør Vitest enhetstester
- `npm run test:coverage` - Enhetstester med dekningsrapport
- `npm run test:e2e` - Kjør Playwright e2e-tester
- `npm run test:e2e:ui` - Playwright med UI-modus
- Enkelttest: `npx playwright test tests/e2e/specs/filnavn.spec.ts`

### GraphQL-kodegenerering
- `npm run generate-graphql` - Generer TypeScript-typer og React hooks fra .gql-filer
- Krever melosys-api kjørende på localhost:8080 med autentiserings-cookie i codegen.yml

## Arkitektur

### Kjerneteknologier
- **Frontend**: React 19 + Redux + redux-form for tilstandshåndtering
- **Bygg**: Vite med TypeScript, Less for styling
- **Testing**: Vitest (enhetstest) + Playwright (e2e) + Axe (tilgjengelighet)
- **API**: GraphQL + REST APIer med apollo-client

### Tilstandshåndtering
- **Redux med Ducks-mønster**: Hvert domene har actions, operations, reducers, selectors, types i `/src/ducks/`
- **Redux-form**: Skjematilstandshåndtering med yup valideringsskjemaer
- **RootState**: Definert i global.d.ts som `AppTypes` modul, importert som `import { RootState } from "AppTypes"`

### Prosjektstruktur
- **`/src/sider/`**: Helside-komponenter (ruter)
- **`/src/felleskomponenter/`**: Gjenbrukbare komponenter
- **`/src/ducks/`**: Redux-moduler (actions, reducers, selectors, operations)
- **`/src/services/api.ts`**: API-lag med moduler i `/src/services/modules/`
- **`/src/navFrontend/`**: Wrapper-komponenter for NAV Design System
- **`/src/kodeverk/`**: Applikasjonskonstanter og skjemanavn

### Komponentmønstre

#### NAV Frontend-komponenter
- Alltid importer med star-import: `import * as Nav from "../../../navFrontend"`
- Bruk som `<Nav.Button>`, `<Nav.CheckboxGroup>`, osv.
- Egne wrapper-komponenter i `/src/navFrontend/` for NAV Design System

#### Skjemakomponenter
- Redux-form-integrasjon via `/src/felleskomponenter/skjema/` (importert som `* as Skjema`)
- Valideringsskjemaer i filer som slutter med `Schema.ts` med yup
- Feilhåndtering med `unwrapMelding()` verktøy fra `/src/felleskomponenter/skjema/utils`

#### Skjematilstandsoppdateringer
- Bruk `changeField` prop for direkte skjematilstandsoppdateringer (omgår Field wrapper)
- Mønster: `changeField("fieldName", value)` i useEffect
- Påkrevd for komponenter som må registrere seg med redux-form uten Field wrapper

### Kodestilkonvensjoner
- **TypeScript foretrukket** for ny kode (legacy JavaScript migreres gradvis)
- **Ingen explicit any**: Bruk ordentlig typing (`@typescript-eslint/no-explicit-any`)
- **Filnavngiving**: kebab-case for filer, PascalCase for komponenter
- **Imports**: Organisert med star-imports for interne moduler

### Valideringsmønstre
- **Skjemavalidering**: Bruk yup-skjemaer med egne testfunksjoner
- **Feilvisning**: Sjekk `formValues?.showFieldErrors` og bruk `unwrapMelding()`
- **Skjemafeltregistrering**: Felt oppdatert via `changeField` blir automatisk berørt av `touchAllFields()`

### API-integrasjon
- **REST**: `/src/services/api.ts` med domenemoduler
- **GraphQL**: Genererte hooks fra .gql-filer, manuelle skjemaoppdateringer påkrevd
- **Autentisering**: Azure AD-integrasjon for miljøspesifikk utvikling

### Teststrategi
- **Enhetstester**: Vitest med @testing-library/react
- **E2E-tester**: Playwright med tilgjengelighetstesting via @axe-core/playwright
- **Testfiler**: Samlokalisert med kildefiler (.test.ts/.spec.ts)

## Viktige notater
- Applikasjonen er opprinnelig javascript - foretrekk TypeScript for ny kode
- NAV Design System-komponenter må importeres via star-import-mønster
- Skjemavalidering krever forståelse av redux-form + yup-integrasjon
- Lokal utvikling krever melosys-api backend kjørende på port 8080
