package by.losik.commentlikeservice.exception;

import by.losik.commentlikeservice.annotation.Loggable;
import by.losik.commentlikeservice.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
@Loggable(level = Loggable.Level.ERROR)
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Mono<ResponseEntity<ApiResponse<String>>> handleBusinessException(BusinessException ex) {
        log.warn("Business exception occurred: {} - {}", ex.getErrorCode(), ex.getMessage());

        ApiResponse<String> response = ApiResponse.error(ex.getMessage());
        return Mono.just(ResponseEntity.status(ex.getStatus()).body(response));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ResponseEntity<ApiResponse<String>>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {} - {}", ex.getErrorCode(), ex.getMessage());

        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage())));
    }

    @ExceptionHandler(ValidationException.class)
    public Mono<ResponseEntity<ApiResponse<String>>> handleValidationException(ValidationException ex) {
        log.warn("Validation exception: {} - {}", ex.getErrorCode(), ex.getMessage());

        return Mono.just(ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage())));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public Mono<ResponseEntity<ApiResponse<String>>> handleDuplicateResourceException(DuplicateResourceException ex) {
        log.warn("Duplicate resource: {} - {}", ex.getErrorCode(), ex.getMessage());

        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage())));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Mono<ResponseEntity<ApiResponse<String>>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {} - {}", ex.getErrorCode(), ex.getMessage());

        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage())));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ApiResponse<Map<String, String>>>> handleValidationException(WebExchangeBindException ex) {
        log.warn("Validation error occurred: {}", ex.getMessage());

        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null ?
                                fieldError.getDefaultMessage() : "Validation error",
                        (existing, replacement) -> existing
                ));

        ApiResponse<Map<String, String>> response = ApiResponse.error("Validation failed");
        response.setData(errors);

        return Mono.just(ResponseEntity.badRequest().body(response));
    }

    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<ApiResponse<String>>> handleServerWebInputException(ServerWebInputException ex) {
        log.warn("Input error occurred: {}", ex.getMessage());

        String errorMessage = "Invalid input data";
        if (ex.getReason() != null) {
            errorMessage = ex.getReason();
        }

        return Mono.just(ResponseEntity.badRequest()
                .body(ApiResponse.error(errorMessage)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ApiResponse<String>>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument error: {}", ex.getMessage());

        return Mono.just(ResponseEntity.badRequest()
                .body(ApiResponse.error(ex.getMessage())));
    }

    @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class)
    public Mono<ResponseEntity<ApiResponse<String>>> handleDuplicateKeyException(
            org.springframework.dao.DuplicateKeyException ex) {
        log.warn("Duplicate key error: {}", ex.getMessage());

        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Resource already exists")));
    }

    @ExceptionHandler(org.springframework.dao.EmptyResultDataAccessException.class)
    public Mono<ResponseEntity<ApiResponse<String>>> handleEmptyResultDataAccessException(
            org.springframework.dao.EmptyResultDataAccessException ex) {
        log.warn("Empty result error: {}", ex.getMessage());

        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Requested resource not found")));
    }

    @ExceptionHandler(org.springframework.r2dbc.BadSqlGrammarException.class)
    public Mono<ResponseEntity<ApiResponse<String>>> handleBadSqlGrammarException(
            org.springframework.r2dbc.BadSqlGrammarException ex) {
        log.error("SQL grammar error: {}", ex.getMessage());

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Database error occurred")));
    }

    @ExceptionHandler(io.r2dbc.spi.R2dbcException.class)
    public Mono<ResponseEntity<ApiResponse<String>>> handleR2dbcException(
            io.r2dbc.spi.R2dbcException ex) {
        log.error("R2DBC database error: {}", ex.getMessage());

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Database operation failed")));
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public Mono<ResponseEntity<ApiResponse<String>>> handleResponseStatusException(
            org.springframework.web.server.ResponseStatusException ex) {
        log.warn("Response status exception: {}", ex.getMessage());

        String errorMessage = ex.getReason() != null ? ex.getReason() : "Request failed";
        return Mono.just(ResponseEntity.status(ex.getStatusCode())
                .body(ApiResponse.error(errorMessage)));
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public Mono<ResponseEntity<ApiResponse<Map<String, String>>>> handleConstraintViolationException(
            jakarta.validation.ConstraintViolationException ex) {
        log.warn("Constraint violation error: {}", ex.getMessage());

        ApiResponse<Map<String, String>> response = ApiResponse.error("Constraint validation failed");

        return Mono.just(ResponseEntity.badRequest().body(response));
    }

    @ExceptionHandler(org.springframework.transaction.TransactionException.class)
    public Mono<ResponseEntity<ApiResponse<String>>> handleTransactionException(
            org.springframework.transaction.TransactionException ex) {
        log.error("Transaction error: {}", ex.getMessage());

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Transaction processing failed")));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiResponse<String>>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred")));
    }
}