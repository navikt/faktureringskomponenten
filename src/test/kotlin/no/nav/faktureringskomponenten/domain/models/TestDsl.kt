package no.nav.faktureringskomponenten.domain.models

/**
 * DSL marker for å hindre at properties fra ytre scopes blir tilgjengelige i indre scopes.
 *
 * Eksempel på hva dette forhindrer:
 * ```
 * Fakturaserie.forTest {
 *     referanse = "MEL-123"
 *     faktura {
 *         // Uten @FaktureringsTestDsl ville man kunne sette 'referanse' her også,
 *         // noe som ville være en feil da referanse hører til Fakturaserie, ikke Faktura
 *         status = BESTILT
 *     }
 * }
 * ```
 */
@DslMarker
annotation class FaktureringsTestDsl
