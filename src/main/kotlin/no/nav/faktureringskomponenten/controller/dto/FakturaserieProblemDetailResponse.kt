package no.nav.faktureringskomponenten.controller.dto

import org.springframework.http.ProblemDetail

data class ProblemDetailResponse (
    val referanseId: String?
): ProblemDetail()
