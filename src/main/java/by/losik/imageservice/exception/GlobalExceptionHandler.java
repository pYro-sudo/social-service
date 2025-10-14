package by.losik.imageservice.exception;

import by.losik.imageservice.annotation.Loggable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@Loggable(level = Loggable.Level.ERROR)
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleWebExchangeBindException(
            WebExchangeBindException ex, ServerWebExchange exchange) {

        List<ErrorResponse.ValidationError> validationErrors = ex.getFieldErrors()
                .stream()
                .map(this::mapToValidationError)
                .collect(Collectors.toList());

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "Request validation failed",
                exchange.getRequest().getPath().value()
        );
        errorResponse.setValidationErrors(validationErrors);

        log.debug("Validation failed: {}", validationErrors);

        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }

    @ExceptionHandler(ImageNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleImageNotFoundException(
            ImageNotFoundException ex, ServerWebExchange exchange) {

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );

        log.debug("Image not found: {}", ex.getMessage());

        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse));
    }

    @ExceptionHandler(S3OperationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleS3OperationException(
            S3OperationException ex, ServerWebExchange exchange) {

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "S3 Service Error",
                "File storage service is temporarily unavailable",
                exchange.getRequest().getPath().value()
        );

        log.error("S3 operation failed: {}", ex.getMessage(), ex);

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse));
    }

    @ExceptionHandler(FileUploadException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleFileUploadException(
            FileUploadException ex, ServerWebExchange exchange) {

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "File Upload Error",
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );

        log.error("File upload failed: {}", ex.getMessage(), ex);

        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }

    @ExceptionHandler(ValidationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(
            ValidationException ex, ServerWebExchange exchange) {

        List<ErrorResponse.ValidationError> validationErrors = ex.getErrors()
                .stream()
                .map(error -> new ErrorResponse.ValidationError(null, error, null))
                .collect(Collectors.toList());

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Business Validation Failed",
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );
        errorResponse.setValidationErrors(validationErrors);

        log.debug("Business validation failed: {}", validationErrors);

        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }

    @ExceptionHandler(DataAccessException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleDataAccessException(
            DataAccessException ex, ServerWebExchange exchange) {

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Database Error",
                "Database operation failed",
                exchange.getRequest().getPath().value()
        );

        log.error("Database error: {}", ex.getMessage(), ex);

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(
            Exception ex, ServerWebExchange exchange) {

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred",
                exchange.getRequest().getPath().value()
        );

        log.error("Unexpected error: {}", ex.getMessage(), ex);

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
    }

    private ErrorResponse.ValidationError mapToValidationError(FieldError fieldError) {
        return new ErrorResponse.ValidationError(
                fieldError.getField(),
                fieldError.getDefaultMessage(),
                fieldError.getRejectedValue()
        );
    }
}