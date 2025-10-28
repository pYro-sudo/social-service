package by.losik.apigateway.service;

import by.losik.apigateway.annotation.Loggable;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@Loggable(level = Loggable.Level.DEBUG, logResult = true)
@RequiredArgsConstructor
public class UserStatusService {

    private final WebClient.Builder webClientBuilder;

    @Value("${user.service.url:http://localhost:8081}")
    private String userServiceUrl;

    public Mono<Boolean> isUserActive(Long userId) {
        return webClientBuilder.build()
                .get()
                .uri(userServiceUrl + "/api/users/exists/{id}", userId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        Mono.error(new RuntimeException("User service unavailable")))
                .bodyToMono(Map.class)
                .map(response -> {
                    Boolean exists = (Boolean) response.get("exists");
                    return exists != null && exists;
                })
                .onErrorReturn(false);
    }

    public Mono<Boolean> isUserEnabled(Long userId) {
        return webClientBuilder.build()
                .get()
                .uri(userServiceUrl + "/api/users/{id}/status", userId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        Mono.error(new RuntimeException("Failed to get user status")))
                .bodyToMono(Map.class)
                .map(response -> {
                    Boolean enabled = (Boolean) response.get("enabled");
                    return enabled == null || enabled;
                })
                .onErrorReturn(true);
    }
}