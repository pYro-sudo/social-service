package by.losik.apigateway.service;

import by.losik.apigateway.annotation.Loggable;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.function.Function;

@Service
@Loggable(logResult = true, level = Loggable.Level.DEBUG)
@RequiredArgsConstructor
public class GatewayJwtService {

    @Value("${spring.jwt.secret}")
    private String SECRET;

    private final TokenBlacklistService tokenBlacklistService;
    private final UserStatusService userStatusService;

    private Key signingKey;

    @PostConstruct
    public void init() {
        if (SECRET == null || SECRET.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret is not configured");
        }
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public Mono<Boolean> validateToken(String token) {
        return Mono.fromCallable(() -> {
                    try {
                        Claims claims = extractAllClaims(token);
                        return claims;
                    } catch (Exception e) {
                        return null;
                    }
                })
                .flatMap(claims -> {
                    if (claims == null) {
                        return Mono.just(false);
                    }

                    return tokenBlacklistService.isTokenBlacklisted(token)
                            .flatMap(isBlacklisted -> {
                                if (isBlacklisted) {
                                    return Mono.just(false);
                                }

                                Long userId = claims.get("userId", Long.class);
                                if (userId != null) {
                                    return userStatusService.isUserActive(userId)
                                            .flatMap(isActive -> {
                                                if (!isActive) {
                                                    return Mono.just(false);
                                                }
                                                return userStatusService.isUserEnabled(userId);
                                            });
                                }

                                return Mono.just(true);
                            });
                })
                .onErrorReturn(false);
    }

    public Mono<String> extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Mono<Long> extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    public Mono<Date> extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public Mono<Instant> extractExpirationAsInstant(String token) {
        return extractExpiration(token)
                .map(Date::toInstant);
    }

    private <T> Mono<T> extractClaim(String token, Function<Claims, T> claimsResolver) {
        return Mono.fromCallable(() -> {
            Claims claims = extractAllClaims(token);
            return claimsResolver.apply(claims);
        });
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Mono<Boolean> revokeToken(String token) {
        return extractExpirationAsInstant(token)
                .flatMap(expiresAt ->
                        tokenBlacklistService.addToBlacklist(token, expiresAt)
                )
                .onErrorReturn(false);
    }
}