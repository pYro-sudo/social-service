package by.losik.apigateway.service;

import by.losik.apigateway.annotation.Loggable;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.function.Function;

@Service
@Loggable(logResult = true, level = Loggable.Level.DEBUG)
public class GatewayJwtService {

    @Value("${spring.jwt.secret}")
    private String SECRET;

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
                Jwts.parserBuilder()
                        .setSigningKey(signingKey)
                        .build()
                        .parseClaimsJws(token);
                return true;
            } catch (Exception e) {
                return false;
            }
        });
    }

    public Mono<String> extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Mono<Long> extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
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

}