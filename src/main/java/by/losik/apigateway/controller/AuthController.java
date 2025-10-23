package by.losik.apigateway.controller;

import by.losik.apigateway.service.GatewayJwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "API для аутентификации и управления токенами")
public class AuthController {

    private final GatewayJwtService jwtService;

    @Autowired
    public AuthController(GatewayJwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Operation(
            summary = "Приветственное сообщение",
            description = "Публичный endpoint, не требующий аутентификации"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Успешное приветствие",
            content = @Content(mediaType = "text/plain")
    )
    @GetMapping("/welcome")
    public Mono<String> welcome() {
        return Mono.just("Welcome to API Gateway - this endpoint is not secure");
    }

    @Operation(
            summary = "Проверка валидности токена",
            description = "Проверяет валидность JWT токена"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Результат проверки токена",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"valid\": true}"))
            )
    })
    @GetMapping("/validate-token")
    public Mono<ResponseEntity<Map<String, Boolean>>> validateToken(
            @Parameter(
                    description = "JWT токен в формате Bearer token",
                    required = true,
                    example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                    in = ParameterIn.HEADER
            )
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.ok(Map.of("valid", false)));
        }

        String token = authHeader.substring(7);
        return jwtService.validateToken(token)
                .map(valid -> ResponseEntity.ok(Map.of("valid", valid)))
                .defaultIfEmpty(ResponseEntity.ok(Map.of("valid", false)));
    }

    @Operation(
            summary = "Проверка здоровья сервиса",
            description = "Возвращает статус работы API Gateway"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Сервис работает нормально",
            content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"status\": \"API Gateway is running\", \"service\": \"api-gateway\", \"timestamp\": \"2024-01-01T12:00:00Z\"}"))
    )
    @GetMapping("/health")
    public Mono<Map<String, String>> healthCheck() {
        return Mono.just(Map.of(
                "status", "API Gateway is running",
                "service", "api-gateway",
                "timestamp", Instant.now().toString()
        ));
    }

    @Operation(
            summary = "Выход из системы",
            description = "Выполняет logout пользователя, отзывая токен и очищая cookie"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешный выход из системы",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"message\": \"Logout successful\"}"))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public Mono<ResponseEntity<Map<String, String>>> logout(
            @Parameter(
                    description = "JWT токен для отзыва",
                    required = true,
                    example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                    in = ParameterIn.HEADER
            )
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @NonNull ServerHttpResponse response) {

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            jwtService.revokeToken(token);
        }

        ResponseCookie cookie = ResponseCookie.from("JWT", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ZERO)
                .build();

        response.addCookie(cookie);

        return Mono.just(ResponseEntity.ok()
                .body(Map.of("message", "Logout successful")));
    }

    @Operation(
            summary = "Получение информации о пользователе",
            description = "Возвращает информацию о пользователе из JWT токена"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешное получение информации о пользователе",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"username\": \"john_doe\", \"userId\": 123, \"authenticated\": true}"))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Невалидный или отсутствующий токен",
                    content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"error\": \"Missing or invalid authorization header\"}"))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/user-info")
    public Mono<ResponseEntity<Map<String, Object>>> getUserInfo(
            @Parameter(
                    description = "JWT токен в формате Bearer token",
                    required = true,
                    example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                    in = ParameterIn.HEADER
            )
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            Map<String, Object> errorBody = Map.of("error", "Missing or invalid authorization header");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody));
        }

        String token = authHeader.substring(7);

        return jwtService.validateToken(token)
                .flatMap(isValid -> {
                    if (!isValid) {
                        Map<String, Object> errorBody = Map.of("error", "Invalid token");
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody));
                    }

                    return jwtService.extractUsername(token)
                            .zipWith(jwtService.extractUserId(token))
                            .map(tuple -> {
                                String username = tuple.getT1();
                                Long userId = tuple.getT2();

                                Map<String, Object> successBody = Map.of(
                                        "username", username,
                                        "userId", userId,
                                        "authenticated", true
                                );
                                return ResponseEntity.ok(successBody);
                            });
                })
                .onErrorResume(e -> {
                    Map<String, Object> errorBody = Map.of("error", "Token validation failed: " + e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody));
                });
    }
}