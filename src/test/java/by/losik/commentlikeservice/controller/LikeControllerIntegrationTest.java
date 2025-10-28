package by.losik.commentlikeservice.controller;

import by.losik.commentlikeservice.config.TestSecurityConfig;
import by.losik.commentlikeservice.dto.LikeRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.webservices.client.AutoConfigureWebServiceClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebServiceClient
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class LikeControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.2-alpine")
            .withExposedPorts(6379);

    @Container
    static GenericContainer<?> zookeeperContainer = new GenericContainer<>("confluentinc/cp-zookeeper:7.8.0")
            .withExposedPorts(2181)
            .withEnv("ZOOKEEPER_CLIENT_PORT", "2181")
            .withReuse(true);

    @Container
    static GenericContainer<?> kafkaContainer = new GenericContainer<>(
            DockerImageName.parse("confluentinc/cp-kafka:7.8.0")
    )
            .withExposedPorts(9092, 9093)
            .withEnv("KAFKA_BROKER_ID", "1")
            .withEnv("KAFKA_ZOOKEEPER_CONNECT", "localhost:2181")
            .withEnv("KAFKA_ADVERTISED_LISTENERS", "PLAINTEXT://localhost:9092,PLAINTEXT_HOST://localhost:9093")
            .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT")
            .withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "PLAINTEXT")
            .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
            .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
            .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
            .withReuse(true).dependsOn(zookeeperContainer);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
                String.format("r2dbc:postgresql://%s:%d/%s",
                        postgreSQLContainer.getHost(),
                        postgreSQLContainer.getFirstMappedPort(),
                        postgreSQLContainer.getDatabaseName()));
        registry.add("spring.r2dbc.username", postgreSQLContainer::getUsername);
        registry.add("spring.r2dbc.password", postgreSQLContainer::getPassword);

        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);

        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
        registry.add("spring.data.redis.database", () -> 0);
        registry.add("spring.data.redis.timeout", () -> java.time.Duration.ofSeconds(10));
        registry.add("spring.data.redis.lettuce.pool.max-active", () -> 8);
        registry.add("spring.data.redis.lettuce.pool.max-idle", () -> 8);
        registry.add("spring.data.redis.lettuce.pool.min-idle", () -> 0);

        registry.add("spring.liquibase.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.liquibase.user", postgreSQLContainer::getUsername);
        registry.add("spring.liquibase.password", postgreSQLContainer::getPassword);
        registry.add("spring.liquibase.default-schema", () -> "public");
        registry.add("spring.liquibase.liquibase-schema", () -> "public");

        registry.add("spring.kafka.topic", () -> "activity-events");
        registry.add("spring.kafka.bootstrap-servers", () ->
                String.format("localhost:%d", kafkaContainer.getMappedPort(9093)));
        registry.add("spring.kafka.consumer.group-id", () -> "comment-service-test-group");
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.producer.key-serializer", () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer", () -> "org.springframework.kafka.support.serializer.JsonSerializer");
        registry.add("spring.kafka.properties.spring.json.trusted.packages", () -> "*");
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ObjectMapper objectMapper;

    private LikeRequest createUniqueLikeRequest() {
        return new LikeRequest(
                new Random().nextLong(),
                new Random().nextLong()
        );
    }

    private LikeRequest createLikeRequestWithSpecificData(Long userId, Long imageId) {
        return new LikeRequest(userId, imageId);
    }

    @BeforeEach
    void cleanup() {
        webTestClient.delete()
                .uri("/api/likes")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getAllLikes_ShouldReturnAllLikes() throws Exception {
        LikeRequest like1 = createUniqueLikeRequest();
        LikeRequest like2 = createUniqueLikeRequest();

        webTestClient.post().uri("/api/likes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(like1)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post().uri("/api/likes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(like2)
                .exchange()
                .expectStatus().isCreated();

        String responseBody = webTestClient.get()
                .uri("/api/likes")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertTrue(response.get("success").asBoolean());
    }

    @Test
    void getLikeById_WhenLikeExists_ShouldReturnLike() throws Exception {
        LikeRequest testLike = createUniqueLikeRequest();

        String createResponse = webTestClient.post()
                .uri("/api/likes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testLike)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode createdLike = objectMapper.readTree(createResponse).get("data");
        Long likeId = createdLike.get("id").asLong();

        String responseBody = webTestClient.get()
                .uri("/api/likes/{id}", likeId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertTrue(response.get("success").asBoolean());
        assertEquals(likeId, response.get("data").get("id").asLong());
        assertEquals(testLike.getUserId(), response.get("data").get("userId").asLong());
        assertEquals(testLike.getImageId(), response.get("data").get("imageId").asLong());
    }

    @Test
    void getLikeById_WhenLikeNotExists_ShouldReturnNotFound() throws Exception {
        String responseBody = webTestClient.get()
                .uri("/api/likes/999")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertFalse(response.get("success").asBoolean());
        assertTrue(response.get("message").asText().contains("not found"));
    }

    @Test
    void createLike_ShouldCreateLikeSuccessfully() throws Exception {
        LikeRequest testLike = createUniqueLikeRequest();

        String responseBody = webTestClient.post()
                .uri("/api/likes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testLike)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertTrue(response.get("success").asBoolean());
        assertTrue(response.get("data").get("id").asLong() > 0);
        assertEquals(testLike.getUserId(), response.get("data").get("userId").asLong());
        assertEquals(testLike.getImageId(), response.get("data").get("imageId").asLong());
        assertNotNull(response.get("data").get("createdAt").asText());
    }

    @Test
    void toggleLike_WhenLikeNotExists_ShouldCreateLike() throws Exception {
        Long userId = 1L;
        Long imageId = 1L;

        String responseBody = webTestClient.post()
                .uri("/api/likes/toggle?userId={userId}&imageId={imageId}", userId, imageId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertTrue(response.get("success").asBoolean());
        assertEquals("liked", response.get("data").asText());
    }

    @Test
    void toggleLike_WhenLikeExists_ShouldRemoveLike() throws Exception {
        Long userId = 1L;
        Long imageId = 1L;

        webTestClient.post()
                .uri("/api/likes/toggle?userId={userId}&imageId={imageId}", userId, imageId)
                .exchange();

        String responseBody = webTestClient.post()
                .uri("/api/likes/toggle?userId={userId}&imageId={imageId}", userId, imageId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertTrue(response.get("success").asBoolean());
        assertEquals("unliked", response.get("data").asText());
    }

    @Test
    void deleteLike_WhenLikeExists_ShouldDeleteSuccessfully() throws Exception {
        LikeRequest testLike = createUniqueLikeRequest();

        String createResponse = webTestClient.post()
                .uri("/api/likes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testLike)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode createdLike = objectMapper.readTree(createResponse).get("data");
        Long likeId = createdLike.get("id").asLong();

        String responseBody = webTestClient.delete()
                .uri("/api/likes/{id}", likeId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertTrue(response.get("success").asBoolean());
        assertTrue(response.get("message").asText().contains("deleted"));
    }

    @Test
    void deleteLike_WhenLikeNotExists_ShouldReturnNotFound() throws Exception {
        String responseBody = webTestClient.delete()
                .uri("/api/likes/999")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertFalse(response.get("success").asBoolean());
    }

    @Test
    void deleteLikeByUserAndImage_ShouldDeleteSuccessfully() throws Exception {
        Long userId = 1L;
        Long imageId = 1L;
        LikeRequest testLike = createLikeRequestWithSpecificData(userId, imageId);

        webTestClient.post()
                .uri("/api/likes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testLike)
                .exchange();

        String responseBody = webTestClient.delete()
                .uri("/api/likes/user/{userId}/image/{imageId}", userId, imageId)
                .exchange()
                .expectStatus().isNoContent()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
    }

    @Test
    void getLikesByUser_ShouldReturnUserLikes() throws Exception {
        Long userId = 1L;
        LikeRequest like1 = createLikeRequestWithSpecificData(userId, 1L);
        LikeRequest like2 = createLikeRequestWithSpecificData(userId, 2L);

        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like1).exchange();
        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like2).exchange();

        String responseBody = webTestClient.get()
                .uri("/api/likes/user/{userId}", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertTrue(response.get("success").asBoolean());
    }

    @Test
    void getLikesByImage_ShouldReturnImageLikes() throws Exception {
        Long imageId = 1L;
        LikeRequest like1 = createLikeRequestWithSpecificData(1L, imageId);
        LikeRequest like2 = createLikeRequestWithSpecificData(2L, imageId);

        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like1).exchange();
        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like2).exchange();

        String responseBody = webTestClient.get()
                .uri("/api/likes/image/{imageId}", imageId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertTrue(response.get("success").asBoolean());
    }

    @Test
    void getLikeCountByImage_ShouldReturnCorrectCount() throws Exception {
        Long imageId = 1L;
        LikeRequest like1 = createLikeRequestWithSpecificData(1L, imageId);
        LikeRequest like2 = createLikeRequestWithSpecificData(2L, imageId);

        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like1).exchange();
        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like2).exchange();

        String responseBody = webTestClient.get()
                .uri("/api/likes/image/{imageId}/count", imageId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertTrue(response.get("success").asBoolean());
        assertTrue(response.get("data").get("count").asLong() >= 2);
    }

    @Test
    void getLikeCountByUser_ShouldReturnCorrectCount() throws Exception {
        Long userId = 1L;
        LikeRequest like1 = createLikeRequestWithSpecificData(userId, 1L);
        LikeRequest like2 = createLikeRequestWithSpecificData(userId, 2L);

        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like1).exchange();
        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like2).exchange();

        String responseBody = webTestClient.get()
                .uri("/api/likes/user/{userId}/count", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertTrue(response.get("success").asBoolean());
        assertTrue(response.get("data").get("count").asLong() >= 2);
    }

    @Test
    void checkIfLiked_WhenLiked_ShouldReturnTrue() throws Exception {
        Long userId = 1L;
        Long imageId = 1L;
        LikeRequest like = createLikeRequestWithSpecificData(userId, imageId);

        webTestClient.post()
                .uri("/api/likes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(like)
                .exchange();

        String responseBody = webTestClient.get()
                .uri("/api/likes/check?userId={userId}&imageId={imageId}", userId, imageId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertTrue(response.get("success").asBoolean());
        assertTrue(response.get("data").asBoolean());
    }

    @Test
    void checkIfLiked_WhenNotLiked_ShouldReturnFalse() throws Exception {
        String responseBody = webTestClient.get()
                .uri("/api/likes/check?userId=999&imageId=999")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertTrue(response.get("success").asBoolean());
        assertFalse(response.get("data").asBoolean());
    }

    @Test
    void deleteAllLikesByImage_ShouldRemoveImageLikes() throws Exception {
        Long imageId = 1L;
        LikeRequest like1 = createLikeRequestWithSpecificData(1L, imageId);
        LikeRequest like2 = createLikeRequestWithSpecificData(2L, imageId);

        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like1).exchange();
        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like2).exchange();

        webTestClient.delete()
                .uri("/api/likes/image/{imageId}", imageId)
                .exchange()
                .expectStatus().is2xxSuccessful();

        String responseBody = webTestClient.get()
                .uri("/api/likes/image/{imageId}", imageId)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertTrue(response.get("success").asBoolean());
    }

    @Test
    void deleteAllLikesByUser_ShouldRemoveUserLikes() throws Exception {
        Long userId = 1L;
        LikeRequest like1 = createLikeRequestWithSpecificData(userId, 1L);
        LikeRequest like2 = createLikeRequestWithSpecificData(userId, 2L);

        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like1).exchange();
        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like2).exchange();

        webTestClient.delete()
                .uri("/api/likes/user/{userId}", userId)
                .exchange()
                .expectStatus().is2xxSuccessful();

        String responseBody = webTestClient.get()
                .uri("/api/likes/user/{userId}", userId)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertTrue(response.get("success").asBoolean());
    }

    @Test
    void toggleLikeForImage_WithHeader_ShouldWorkCorrectly() throws Exception {
        long userId = 1L;
        Long imageId = 1L;

        String responseBody1 = webTestClient.post()
                .uri("/api/likes/images/{imageId}/likes", imageId)
                .header("X-User-Id", Long.toString(userId))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response1 = objectMapper.readTree(responseBody1);
        assertTrue(response1.get("success").asBoolean());
        assertEquals("liked", response1.get("data").asText());

        String responseBody2 = webTestClient.post()
                .uri("/api/likes/images/{imageId}/likes", imageId)
                .header("X-User-Id", Long.toString(userId))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response2 = objectMapper.readTree(responseBody2);
        assertTrue(response2.get("success").asBoolean());
        assertEquals("unliked", response2.get("data").asText());
    }

    @Test
    void createLike_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        LikeRequest invalidLike = new LikeRequest(null, null);

        String responseBody = webTestClient.post()
                .uri("/api/likes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidLike)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertFalse(response.get("success").asBoolean());
    }
}