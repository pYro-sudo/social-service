package by.losik.apigateway;

import by.losik.apigateway.filter.RateLimiterGatewayFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class RateLimiterGatewayFilterTest {

    @Autowired
    private RateLimiterGatewayFilter rateLimiterGatewayFilter;

    @MockBean
    private RateLimiter<?> rateLimiter;

    @BeforeEach
    void setUp() {
        setupRateLimiterMock(true, 10, 9);
    }

    private void setupRateLimiterMock(boolean allowed, int limit, int remaining) {
        RateLimiter.Response response = new RateLimiter.Response(
                allowed,
                createRateLimitHeaders(limit, remaining)
        );
        when(rateLimiter.isAllowed(anyString(), anyString()))
                .thenReturn(Mono.just(response));
    }

    private Map<String, String> createRateLimitHeaders(int limit, int remaining) {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-RateLimit-Limit", String.valueOf(limit));
        headers.put("X-RateLimit-Remaining", String.valueOf(remaining));
        headers.put("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() / 1000 + 60));
        return headers;
    }

    @Test
    void apply_WithRateLimitNotExceeded_ShouldAllowRequest() {
        setupRateLimiterMock(true, 10, 5);

        RateLimiterGatewayFilter.Config config = new RateLimiterGatewayFilter.Config();
        config.setReplenishRate(10);
        config.setBurstCapacity(20);
        config.setKeyResolver("remoteAddress");

        GatewayFilter filter = rateLimiterGatewayFilter.apply(config);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, __ -> Mono.empty()))
                .expectComplete()
                .verify();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void apply_WithRateLimitExceeded_ShouldReturnTooManyRequests() {
        setupRateLimiterMock(false, 1, 0);

        RateLimiterGatewayFilter.Config config = new RateLimiterGatewayFilter.Config();
        config.setReplenishRate(1);
        config.setBurstCapacity(1);
        config.setKeyResolver("remoteAddress");

        GatewayFilter filter = rateLimiterGatewayFilter.apply(config);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, __ -> Mono.empty()))
                .expectComplete()
                .verify();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void apply_WithRateLimitHeaders_ShouldAddHeaders() {
        setupRateLimiterMock(true, 10, 8);

        RateLimiterGatewayFilter.Config config = new RateLimiterGatewayFilter.Config();
        config.setReplenishRate(10);
        config.setBurstCapacity(20);
        config.setKeyResolver("remoteAddress");

        GatewayFilter filter = rateLimiterGatewayFilter.apply(config);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, __ -> Mono.empty()))
                .expectComplete()
                .verify();

        assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit")).isEqualTo("10");
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void apply_WithDifferentKeyResolvers_ShouldUseCorrectKey() {
        testKeyResolver("remoteAddress");
        testKeyResolver("user");
        testKeyResolver("apiKey");
        testKeyResolver("path");
    }

    private void testKeyResolver(String resolverType) {
        setupRateLimiterMock(true, 10, 5);

        RateLimiterGatewayFilter.Config config = new RateLimiterGatewayFilter.Config();
        config.setKeyResolver(resolverType);

        GatewayFilter filter = rateLimiterGatewayFilter.apply(config);

        MockServerHttpRequest.BaseBuilder<?> requestBuilder = MockServerHttpRequest
                .get("/api/test")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8080));

        switch (resolverType) {
            case "user" -> requestBuilder.header("X-User-Id", "testuser");
            case "apiKey" -> requestBuilder.header("X-API-Key", "testkey");
        }

        ServerWebExchange exchange = MockServerWebExchange.from(requestBuilder.build());

        StepVerifier.create(filter.filter(exchange, __ -> Mono.empty()))
                .expectComplete()
                .verify();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void apply_WithRateLimiterError_ShouldAllowRequest() {
        when(rateLimiter.isAllowed(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Redis connection failed")));

        RateLimiterGatewayFilter.Config config = new RateLimiterGatewayFilter.Config();
        config.setReplenishRate(10);
        config.setBurstCapacity(20);
        config.setKeyResolver("remoteAddress");

        GatewayFilter filter = rateLimiterGatewayFilter.apply(config);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, __ -> Mono.empty()))
                .expectComplete()
                .verify();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void apply_WithCustomRateLimiter_ShouldUseCustomLimiter() {
        RateLimiter<?> customRateLimiter = new RateLimiter<>() {
            @Override
            public Mono<Response> isAllowed(String routeId, String id) {
                return Mono.just(new Response(false, createRateLimitHeaders(5, 0)));
            }

            @Override
            public Map<String, Object> getConfig() {
                return Map.of();
            }

            @Override
            public Class<Object> getConfigClass() {
                return Object.class;
            }

            @Override
            public Object newConfig() {
                return new Object();
            }
        };

        RateLimiterGatewayFilter.Config config = new RateLimiterGatewayFilter.Config();
        config.setRateLimiter(customRateLimiter);
        config.setKeyResolver("remoteAddress");

        GatewayFilter filter = rateLimiterGatewayFilter.apply(config);

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/test")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, __ -> Mono.empty()))
                .expectComplete()
                .verify();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}