package by.losik.apigateway.filter;

import by.losik.apigateway.annotation.Loggable;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
@Loggable(level = Loggable.Level.INFO, logResult = true)
public class RateLimiterGatewayFilter extends AbstractGatewayFilterFactory<RateLimiterGatewayFilter.Config> {

    private final RateLimiter<?> defaultRateLimiter;

    @Autowired
    public RateLimiterGatewayFilter(RateLimiter<?> redisRateLimiter) {
        super(Config.class);
        this.defaultRateLimiter = redisRateLimiter;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            String routeId = route != null ? route.getId() : "default";
            String key = config.getKey(exchange);

            RateLimiter<?> rateLimiter = config.getRateLimiter() != null ?
                    config.getRateLimiter() : defaultRateLimiter;

            return rateLimiter.isAllowed(routeId, key)
                    .flatMap(response -> {
                        if (!response.isAllowed()) {
                            return handleRateLimitExceeded(exchange, config, response);
                        }
                        addRateLimitHeaders(exchange, response, config);
                        return chain.filter(exchange);
                    })
                    .onErrorResume(e -> chain.filter(exchange));
        };
    }

    private void addRateLimitHeaders(ServerWebExchange exchange,
                                     RateLimiter.Response response,
                                     Config config) {
        exchange.getResponse().getHeaders().add("X-RateLimit-Limit",
                String.valueOf(config.getReplenishRate()));

        String remaining = response.getHeaders().get("X-RateLimit-Remaining");
        if (remaining != null) {
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", remaining);
        }

        String reset = response.getHeaders().get("X-RateLimit-Reset");
        if (reset != null) {
            exchange.getResponse().getHeaders().add("X-RateLimit-Reset", reset);
        }
    }

    private Mono<Void> handleRateLimitExceeded(ServerWebExchange exchange, Config config,
                                               RateLimiter.Response response) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String responseBody = String.format(
                "{\"error\": \"Rate limit exceeded\", \"message\": \"%s\", \"timestamp\": \"%s\"}",
                config.getErrorMessage(),
                Instant.now()
        );

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(responseBody.getBytes());
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
    @Getter
    @Setter
    public static class Config {
        private int replenishRate = 10;
        private int burstCapacity = 20;
        private int requestedTokens = 1;
        private String keyResolver = "remoteAddress";
        private String rateLimiterBean = "redisRateLimiter";
        private RateLimiter<?> rateLimiter;

        public String getKey(ServerWebExchange exchange) {
            switch (keyResolver) {
                case "user"-> {
                    String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
                    return userId != null ? "user:" + userId : "user:anonymous";
                }
                case "apiKey" -> {
                    String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
                    return apiKey != null ? "apikey:" + apiKey : "apikey:no-key";
                }
                case "path" -> {
                    return "path:" + exchange.getRequest().getPath().value();
                }
                default -> {
                    String remoteAddress = exchange.getRequest().getRemoteAddress() != null ?
                            exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() :
                            "unknown";
                    return "ip:" + remoteAddress;
                }
            }
        }

        public String getErrorMessage() {
            return String.format("Rate limit exceeded. Limit: %d requests per second", replenishRate);
        }
    }
}