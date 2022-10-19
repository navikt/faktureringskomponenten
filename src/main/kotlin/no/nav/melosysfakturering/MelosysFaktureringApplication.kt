package no.nav.melosysfakturering

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MelosysFaktureringApplication

fun main(args: Array<String>) {
	runApplication<MelosysFaktureringApplication>(*args)
}
