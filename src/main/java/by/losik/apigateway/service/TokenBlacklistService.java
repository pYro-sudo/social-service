package by.losik.apigateway.service;

import by.losik.apigateway.annotation.Loggable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Service
@Loggable(level = Loggable.Level.DEBUG, logResult = true)
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:";

    public Mono<Boolean> isTokenBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return redisTemplate.hasKey(key);
    }

    public Mono<Boolean> addToBlacklist(String token, Duration ttl) {
        String key = BLACKLIST_PREFIX + token;
        return redisTemplate.opsForValue()
                .set(key, "revoked", ttl)
                .then(Mono.just(true));
    }

    public Mono<Boolean> addToBlacklist(String token, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative()) {
            return Mono.just(true);
        }
        return addToBlacklist(token, ttl);
    }
}