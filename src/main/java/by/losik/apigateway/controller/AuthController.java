package by.losik.apigateway.controller;

import by.losik.apigateway.service.GatewayJwtService;
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
public class AuthController {

    private final GatewayJwtService jwtService;

    @Autowired
    public AuthController(GatewayJwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/welcome")
    public Mono<String> welcome() {
        return Mono.just("Welcome to API Gateway - this endpoint is not secure");
    }

    @GetMapping("/validate-token")
    public Mono<ResponseEntity<Map<String, Boolean>>> validateToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.ok(Map.of("valid", false)));
        }

        String token = authHeader.substring(7);
        return jwtService.validateToken(token)
                .map(valid -> ResponseEntity.ok(Map.of("valid", valid)))
                .defaultIfEmpty(ResponseEntity.ok(Map.of("valid", false)));
    }

    @GetMapping("/health")
    public Mono<Map<String, String>> healthCheck() {
        return Mono.just(Map.of(
            "status", "API Gateway is running", 
            "service", "api-gateway",
            "timestamp", Instant.now().toString()
        ));
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Map<String, String>>> logout(@NonNull ServerHttpResponse response) {
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

    @GetMapping("/user-info")
    public Mono<ResponseEntity<Map<String, Object>>> getUserInfo(@RequestHeader(value = "Authorization", required = false) String authHeader) {
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