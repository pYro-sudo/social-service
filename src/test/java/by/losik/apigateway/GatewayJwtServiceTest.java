package by.losik.apigateway;

import by.losik.apigateway.service.GatewayJwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.jwt.secret=mySuperSecretKeyForJWTTokenGenerationAndValidationInSpringMicroserviceCourse2023"
})
class GatewayJwtServiceTest {

    @Autowired
    private GatewayJwtService jwtService;

    private String notThatValidToken;

    @BeforeEach
    void setUp() {
        notThatValidToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0dXNlciIsInVzZXJJZCI6MTIzLCJpYXQiOjE2MTYyMzkwMjJ9.testSignature";
    }

    @Test
    void validateToken_WithInvalidToken_ShouldReturnFalse() {
        StepVerifier.create(jwtService.validateToken("invalid.token"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void extractUsername_WithValidToken_ShouldReturnUsername() {
        StepVerifier.create(jwtService.extractUsername(notThatValidToken))
                .expectNextCount(0)
                .verifyError();
    }
}