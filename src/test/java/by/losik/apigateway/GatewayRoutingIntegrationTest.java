package by.losik.apigateway;

import by.losik.apigateway.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(SecurityConfig.class)
class GatewayRoutingIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Container
    static GenericContainer<?> mockUserService = new GenericContainer<>("mockserver/mockserver:5.15.0")
            .withExposedPorts(8081)
            .withCommand("-serverPort 8081 -logLevel INFO")
            .waitingFor(Wait.forLogMessage(".*8081 started on port: 8081.*", 1));

    @Container
    static GenericContainer<?> mockImageService = new GenericContainer<>("mockserver/mockserver:5.15.0")
            .withExposedPorts(8082)
            .withCommand("-serverPort 8082 -logLevel INFO")
            .waitingFor(Wait.forLogMessage(".*8082 started on port: 8082.*", 1));

    @Container
    static GenericContainer<?> mockCommentLikeService = new GenericContainer<>("mockserver/mockserver:5.15.0")
            .withExposedPorts(8083)
            .withCommand("-serverPort 8083 -logLevel INFO")
            .waitingFor(Wait.forLogMessage(".*8083 started on port: 8083.*", 1));

    @Container
    static GenericContainer<?> mockAuthService = new GenericContainer<>("mockserver/mockserver:5.15.0")
            .withExposedPorts(8080)
            .withCommand("-serverPort 8080 -logLevel INFO")
            .waitingFor(Wait.forLogMessage(".*8080 started on port: 8080.*", 1));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.gateway.discovery.locator.enabled", () -> false);

        registry.add("spring.cloud.gateway.routes[0].id", () -> "auth");
        registry.add("spring.cloud.gateway.routes[0].uri",
                () -> "http://" + mockAuthService.getHost() + ":" + mockAuthService.getFirstMappedPort());
        registry.add("spring.cloud.gateway.routes[0].predicates[0]", () -> "Path=/auth/**");

        registry.add("spring.cloud.gateway.routes[1].id", () -> "user-service");
        registry.add("spring.cloud.gateway.routes[1].uri",
                () -> "http://" + mockUserService.getHost() + ":" + mockUserService.getFirstMappedPort());
        registry.add("spring.cloud.gateway.routes[1].predicates[0]", () -> "Path=/api/users/**");
        registry.add("spring.cloud.gateway.routes[1].filters[0]", () -> "JwtAuthGatewayFilter");

        registry.add("spring.cloud.gateway.routes[2].id", () -> "image-service");
        registry.add("spring.cloud.gateway.routes[2].uri",
                () -> "http://" + mockImageService.getHost() + ":" + mockImageService.getFirstMappedPort());
        registry.add("spring.cloud.gateway.routes[2].predicates[0]", () -> "Path=/api/images/**");
        registry.add("spring.cloud.gateway.routes[2].filters[0]", () -> "JwtAuthGatewayFilter");

        registry.add("spring.cloud.gateway.routes[3].id", () -> "comment-like-service");
        registry.add("spring.cloud.gateway.routes[3].uri",
                () -> "http://" + mockCommentLikeService.getHost() + ":" + mockCommentLikeService.getFirstMappedPort());
        registry.add("spring.cloud.gateway.routes[3].predicates[0]", () -> "Path=/api/comments/**,/api/likes/**");
        registry.add("spring.cloud.gateway.routes[3].filters[0]", () -> "JwtAuthGatewayFilter");
    }

    @Test
    void authEndpoint_ShouldBeAccessible() {
        webTestClient.get()
                .uri("/auth/welcome")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void userServiceRouting_ShouldWork() {
        webTestClient.get()
                .uri("/api/users")
                .header("Authorization", "Bearer mock-jwt-token-for-testing")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void imageServiceRouting_ShouldWork() {
        webTestClient.get()
                .uri("/api/images")
                .header("Authorization", "Bearer mock-jwt-token-for-testing")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void commentServiceRouting_ShouldWork() {
        webTestClient.get()
                .uri("/api/comments")
                .header("Authorization", "Bearer mock-jwt-token-for-testing")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void likeServiceRouting_ShouldWork() {
        webTestClient.get()
                .uri("/api/likes")
                .header("Authorization", "Bearer mock-jwt-token-for-testing")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}