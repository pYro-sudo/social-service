package by.losik.apigateway.exception;

import by.losik.apigateway.annotation.Loggable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
@Loggable(level = Loggable.Level.ERROR)
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleGenericException(Exception ex) {
        Map<String, Object> errorBody = Map.of(
                "error", "Internal Server Error",
                "message", ex.getMessage(),
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.INTERNAL_SERVER_ERROR.value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody));
    }

    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleBadRequest(ServerWebInputException ex) {
        Map<String, Object> errorBody = Map.of(
                "error", "Bad Request",
                "message", ex.getReason() != null ? ex.getReason() : "Invalid request parameters",
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.BAD_REQUEST.value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, Object> errorBody = Map.of(
                "error", "Bad Request",
                "message", ex.getMessage(),
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.BAD_REQUEST.value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody));
    }

    @ExceptionHandler(IllegalStateException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleIllegalStateException(IllegalStateException ex) {
        Map<String, Object> errorBody = Map.of(
                "error", "Service Unavailable",
                "message", ex.getMessage(),
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.SERVICE_UNAVAILABLE.value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorBody));
    }

    @ExceptionHandler(RuntimeException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> errorBody = Map.of(
                "error", "Internal Server Error",
                "message", ex.getMessage(),
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.INTERNAL_SERVER_ERROR.value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleRateLimitExceeded(RateLimitExceededException ex) {
        Map<String, Object> errorBody = Map.of(
                "error", "Too Many Requests",
                "message", ex.getMessage(),
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.TOO_MANY_REQUESTS.value()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorBody));
    }
}