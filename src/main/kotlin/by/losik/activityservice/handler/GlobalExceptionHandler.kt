package by.losik.activityservice.handler

import by.losik.activityservice.dto.ErrorResponse
import by.losik.activityservice.exception.ActivityNotFoundException
import by.losik.activityservice.exception.NotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ServerWebInputException
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import org.springframework.web.server.ServerWebExchange
import java.time.format.DateTimeParseException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = ex.message ?: "Invalid argument provided",
            path = exchange.request.path.value()
        )
        return Mono.just(ResponseEntity.badRequest().body(errorResponse))
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(
        ex: NotFoundException,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.NOT_FOUND.value(),
            error = HttpStatus.NOT_FOUND.reasonPhrase,
            message = ex.message ?: "Resource not found",
            path = exchange.request.path.value()
        )
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse))
    }

    @ExceptionHandler(ActivityNotFoundException::class)
    fun handleActivityNotFoundException(
        ex: ActivityNotFoundException,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.NOT_FOUND.value(),
            error = HttpStatus.NOT_FOUND.reasonPhrase,
            message = ex.message ?: "Activity not found",
            path = exchange.request.path.value()
        )
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse))
    }

    @ExceptionHandler(WebExchangeBindException::class)
    fun handleWebExchangeBindException(
        ex: WebExchangeBindException,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> {
        val errors = ex.bindingResult.fieldErrors.joinToString { error ->
            "${error.field}: ${error.defaultMessage}"
        }

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = "Validation failed: $errors",
            path = exchange.request.path.value()
        )
        return Mono.just(ResponseEntity.badRequest().body(errorResponse))
    }

    @ExceptionHandler(ServerWebInputException::class)
    fun handleServerWebInputException(
        ex: ServerWebInputException,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = "Invalid input: ${ex.reason ?: "Malformed request"}",
            path = exchange.request.path.value()
        )
        return Mono.just(ResponseEntity.badRequest().body(errorResponse))
    }

    @ExceptionHandler(DateTimeParseException::class)
    fun handleDateTimeParseException(
        ex: DateTimeParseException,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = "Invalid date format. Please use ISO format (e.g., 2023-12-31T10:30:00)",
            path = exchange.request.path.value()
        )
        return Mono.just(ResponseEntity.badRequest().body(errorResponse))
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<ErrorResponse>> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
            message = "An unexpected error occurred",
            path = exchange.request.path.value()
        )
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse))
    }
}