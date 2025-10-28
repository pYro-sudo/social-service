package by.losik.apigateway;

import by.losik.apigateway.config.SecurityConfig;
import by.losik.apigateway.controller.AuthController;
import by.losik.apigateway.service.GatewayJwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@WebFluxTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private GatewayJwtService jwtService;

    @Test
    void welcome_ShouldReturnWelcomeMessage() {
        webTestClient.get()
                .uri("/auth/welcome")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Welcome to API Gateway - this endpoint is not secure");
    }

    @Test
    void validateToken_WithValidToken_ShouldReturnValidTrue() {
        String validToken = "valid.jwt.token";

        when(jwtService.validateToken(validToken))
                .thenReturn(Mono.just(true));

        webTestClient.get()
                .uri("/auth/validate-token")
                .header("Authorization", "Bearer " + validToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.valid").isEqualTo(true);
    }

    @Test
    void validateToken_WithInvalidToken_ShouldReturnValidFalse() {
        when(jwtService.validateToken(anyString()))
                .thenReturn(Mono.just(false));

        webTestClient.get()
                .uri("/auth/validate-token")
                .header("Authorization", "Bearer invalid.token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.valid").isEqualTo(false);
    }

    @Test
    void validateToken_WithoutToken_ShouldReturnValidFalse() {
        webTestClient.get()
                .uri("/auth/validate-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.valid").isEqualTo(false);
    }

    @Test
    void healthCheck_ShouldReturnServiceStatus() {
        webTestClient.get()
                .uri("/auth/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("API Gateway is running")
                .jsonPath("$.service").isEqualTo("api-gateway")
                .jsonPath("$.timestamp").exists();
    }

    @Test
    void getUserInfo_WithValidToken_ShouldReturnUserInfo() {
        String validToken = "valid.jwt.token";

        when(jwtService.validateToken(validToken))
                .thenReturn(Mono.just(true));
        when(jwtService.extractUsername(validToken))
                .thenReturn(Mono.just("testuser"));
        when(jwtService.extractUserId(validToken))
                .thenReturn(Mono.just(123L));

        webTestClient.get()
                .uri("/auth/user-info")
                .header("Authorization", "Bearer " + validToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.username").isEqualTo("testuser")
                .jsonPath("$.userId").isEqualTo(123)
                .jsonPath("$.authenticated").isEqualTo(true);
    }
}