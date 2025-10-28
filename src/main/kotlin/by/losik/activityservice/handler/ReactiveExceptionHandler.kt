package by.losik.activityservice.handler

import by.losik.activityservice.exception.BaseException
import by.losik.activityservice.exception.NotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class ReactiveExceptionHandler {

    companion object {
        private val log = LoggerFactory.getLogger(ReactiveExceptionHandler::class.java)
    }

    fun <T> wrapMono(mono: Mono<T>): Mono<T> {
        return mono.onErrorResume { error ->
            logError(error)
            Mono.error(error)
        }
    }

    fun <T> wrapFlux(flux: Flux<T>): Flux<T> {
        return flux.onErrorResume { error ->
            logError(error)
            Flux.error(error)
        }
    }

    private fun logError(error: Throwable) {
        when (error) {
            is NotFoundException -> log.warn("Resource not found: {}", error.message)
            is BaseException -> log.warn("Business error: {}", error.message)
            is IllegalArgumentException -> log.warn("Validation error: {}", error.message)
            else -> log.error("Technical error: {}", error.message, error)
        }
    }
}