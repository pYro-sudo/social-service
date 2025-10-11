package by.losik.apigateway;

import by.losik.apigateway.config.SecurityConfig;
import by.losik.apigateway.filter.JwtAuthGatewayFilter;
import by.losik.apigateway.service.GatewayJwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Objects;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(SecurityConfig.class)
class JwtAuthGatewayFilterTest {

    @Autowired
    private JwtAuthGatewayFilter jwtAuthGatewayFilter;

    @MockBean
    private GatewayJwtService jwtService;

    @Test
    void apply_WithOpenEndpoint_ShouldNotFilter() {
        GatewayFilter filter = jwtAuthGatewayFilter.apply(new JwtAuthGatewayFilter.Config());

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/auth/welcome")
                .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, __ -> Mono.empty()))
                .verifyComplete();
    }

    @Test
    void apply_WithProtectedEndpointAndValidToken_ShouldAddHeaders() {
        String validToken = "valid.jwt.token";
        String username = "testuser";
        Long userId = 123L;

        when(jwtService.validateToken(validToken))
                .thenReturn(Mono.just(true));
        when(jwtService.extractUsername(validToken))
                .thenReturn(Mono.just(username));
        when(jwtService.extractUserId(validToken))
                .thenReturn(Mono.just(userId));

        GatewayFilter filter = jwtAuthGatewayFilter.apply(new JwtAuthGatewayFilter.Config());

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/images/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, serverWebExchange -> {
                    String xUserName = serverWebExchange.getRequest().getHeaders().getFirst("X-User-Name");
                    String xUserId = serverWebExchange.getRequest().getHeaders().getFirst("X-User-Id");

                    assert Objects.equals(xUserName, username);
                    assert Objects.equals(xUserId, userId.toString());

                    return Mono.empty();
                }))
                .verifyComplete();
    }

    @Test
    void apply_WithProtectedEndpointAndInvalidToken_ShouldReturnUnauthorized() {
        when(jwtService.validateToken(anyString()))
                .thenReturn(Mono.just(false));

        GatewayFilter filter = jwtAuthGatewayFilter.apply(new JwtAuthGatewayFilter.Config());

        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/images/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token")
                .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(filter.filter(exchange, __ -> Mono.empty()))
                .expectComplete()
                .verify();

        assert exchange.getResponse().getStatusCode() == HttpStatus.UNAUTHORIZED;
    }
}