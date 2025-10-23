package by.losik.userservice.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class PasswordEncoder {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public Mono<String> encode(String rawPassword) {
        return Mono.fromCallable(() -> encoder.encode(rawPassword))
                .onErrorMap(e -> new RuntimeException("Password encoding failed", e));
    }

    public Mono<Boolean> matches(String rawPassword, String encodedPassword) {
        return Mono.fromCallable(() -> encoder.matches(rawPassword, encodedPassword))
                .onErrorReturn(false);
    }
}