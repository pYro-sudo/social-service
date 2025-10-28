package by.losik.apigateway.filter;

import by.losik.apigateway.annotation.Loggable;
import by.losik.apigateway.service.GatewayJwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Loggable(level = Loggable.Level.DEBUG, logResult = true)
public class JwtAuthGatewayFilter extends AbstractGatewayFilterFactory<JwtAuthGatewayFilter.Config> {

    private final GatewayJwtService jwtService;

    private final List<String> openEndpoints = List.of(
            "/auth/welcome",
            "/auth/add-new-user",
            "/auth/generate-token",
            "/auth/refresh-token",
            "/auth/validate-token",
            "/auth/logout",
            "/auth/health",
            "/auth/user-info"
    );

    @Autowired
    public JwtAuthGatewayFilter(GatewayJwtService jwtService) {
        super(Config.class);
        this.jwtService = jwtService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();

            if (isOpenEndpoint(path)) {
                return chain.filter(exchange);
            }

            String token = extractToken(request);

            if (token == null) {
                return onError(exchange);
            }

            return jwtService.validateToken(token)
                    .flatMap(isValid -> {
                        if (!isValid) {
                            return onError(exchange);
                        }

                        return jwtService.extractUsername(token)
                                .zipWith(jwtService.extractUserId(token))
                                .flatMap(tuple -> {
                                    String username = tuple.getT1();
                                    Long userId = tuple.getT2();

                                    ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                                            .header("X-User-Name", username)
                                            .header("X-User-Id", userId.toString())
                                            .build();

                                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                                });
                    })
                    .onErrorResume(e -> onError(exchange));
        };
    }

    private boolean isOpenEndpoint(@NonNull String path) {
        return openEndpoints.stream().anyMatch(path::startsWith);
    }

    private @Nullable String extractToken(@NonNull ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        assert authHeader != null;
        if (authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        var cookies = request.getCookies().get("JWT");
        if (cookies != null && !cookies.isEmpty()) {
            return cookies.get(0).getValue();
        }

        return null;
    }

    private @NonNull Mono<Void> onError(@NonNull ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
    }
}