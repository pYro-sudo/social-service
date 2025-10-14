package by.losik.commentlikeservice.controller;

import by.losik.commentlikeservice.config.TestSecurityConfig;
import by.losik.commentlikeservice.dto.CommentRequest;
import by.losik.commentlikeservice.dto.UpdateContentRequest;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebServiceClient
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class CommentControllerIntegrationTest {

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

    private CommentRequest createUniqueCommentRequest() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return new CommentRequest(
                "Test comment content " + uniqueId,
                1L,
                1L
        );
    }

    private CommentRequest createCommentRequestWithSpecificData(Long userId, Long imageId, String content) {
        return new CommentRequest(
                content,
                userId,
                imageId
        );
    }

    @BeforeEach
    void cleanup() {
        webTestClient.delete()
                .uri("/api/comments")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getAllComments_ShouldReturnAllComments() throws Exception {
        CommentRequest comment1 = createUniqueCommentRequest();
        CommentRequest comment2 = createUniqueCommentRequest();

        webTestClient.post().uri("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(comment1)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post().uri("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(comment2)
                .exchange()
                .expectStatus().isCreated();

        String responseBody = webTestClient.get()
                .uri("/api/comments")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertTrue(response.get("success").asBoolean());
        assertTrue(response.get("data").isArray());
        assertTrue(response.get("data").size() >= 2);
    }

    @Test
    void getCommentById_WhenCommentExists_ShouldReturnComment() throws Exception {
        CommentRequest testComment = createUniqueCommentRequest();

        String createResponse = webTestClient.post()
                .uri("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testComment)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode createdComment = objectMapper.readTree(createResponse).get("data");
        Long commentId = createdComment.get("id").asLong();

        String responseBody = webTestClient.get()
                .uri("/api/comments/{id}", commentId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertTrue(response.get("success").asBoolean());
        assertEquals(commentId, response.get("data").get("id").asLong());
        assertEquals(testComment.getContent(), response.get("data").get("content").asText());
        assertEquals(testComment.getUserId(), response.get("data").get("userId").asLong());
        assertEquals(testComment.getImageId(), response.get("data").get("imageId").asLong());
    }

    @Test
    void getCommentById_WhenCommentNotExists_ShouldReturnNotFound() throws Exception {
        String responseBody = webTestClient.get()
                .uri("/api/comments/999")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertFalse(response.get("success").asBoolean());
        assertTrue(response.get("message").asText().contains("not found"));
    }

    @Test
    void createComment_ShouldCreateCommentSuccessfully() throws Exception {
        CommentRequest testComment = createUniqueCommentRequest();

        String responseBody = webTestClient.post()
                .uri("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testComment)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertTrue(response.get("success").asBoolean());
        assertTrue(response.get("data").get("id").asLong() > 0);
        assertEquals(testComment.getContent(), response.get("data").get("content").asText());
        assertEquals(testComment.getUserId(), response.get("data").get("userId").asLong());
        assertEquals(testComment.getImageId(), response.get("data").get("imageId").asLong());
        assertNotNull(response.get("data").get("createdAt").asText());
    }

    @Test
    void createCommentForImage_ShouldCreateCommentSuccessfully() throws Exception {
        Long userId = 1L;
        Long imageId = 1L;
        String content = "Test comment content";

        String responseBody = webTestClient.post()
                .uri("/api/comments/user/{userId}/image/{imageId}?content={content}", userId, imageId, content)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertTrue(response.get("success").asBoolean());
        assertTrue(response.get("data").get("id").asLong() > 0);
        assertEquals(content, response.get("data").get("content").asText());
        assertEquals(userId, response.get("data").get("userId").asLong());
        assertEquals(imageId, response.get("data").get("imageId").asLong());
    }

    @Test
    void updateComment_WhenCommentExists_ShouldUpdateSuccessfully() throws Exception {
        CommentRequest testComment = createUniqueCommentRequest();

        String createResponse = webTestClient.post()
                .uri("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testComment)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode createdComment = objectMapper.readTree(createResponse).get("data");
        Long commentId = createdComment.get("id").asLong();

        CommentRequest updatedComment = new CommentRequest(
                "Updated comment content",
                2L,
                2L
        );

        String responseBody = webTestClient.put()
                .uri("/api/comments/{id}", commentId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updatedComment)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertTrue(response.get("success").asBoolean());
        assertEquals(updatedComment.getContent(), response.get("data").get("content").asText());
        assertEquals(updatedComment.getUserId(), response.get("data").get("userId").asLong());
        assertEquals(updatedComment.getImageId(), response.get("data").get("imageId").asLong());
    }

    @Test
    void updateComment_WhenCommentNotExists_ShouldReturnNotFound() throws Exception {
        CommentRequest nonExistentComment = createUniqueCommentRequest();

        String responseBody = webTestClient.put()
                .uri("/api/comments/999")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(nonExistentComment)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertFalse(response.get("success").asBoolean());
    }

    @Test
    void deleteComment_WhenCommentExists_ShouldDeleteSuccessfully() throws Exception {
        CommentRequest testComment = createUniqueCommentRequest();

        String createResponse = webTestClient.post()
                .uri("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testComment)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode createdComment = objectMapper.readTree(createResponse).get("data");
        Long commentId = createdComment.get("id").asLong();

        String responseBody = webTestClient.delete()
                .uri("/api/comments/{id}", commentId)
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
    void deleteComment_WhenCommentNotExists_ShouldReturnNotFound() throws Exception {
        String responseBody = webTestClient.delete()
                .uri("/api/comments/999")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertFalse(response.get("success").asBoolean());
    }

    @Test
    void getCommentsByUser_ShouldReturnUserComments() throws Exception {
        Long userId = 1L;
        CommentRequest comment1 = createCommentRequestWithSpecificData(userId, 1L, "Comment 1");
        CommentRequest comment2 = createCommentRequestWithSpecificData(userId, 2L, "Comment 2");

        webTestClient.post().uri("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(comment1)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post().uri("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(comment2)
                .exchange()
                .expectStatus().isCreated();

        String responseBody = webTestClient.get()
                .uri("/api/comments/user/{userId}", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertTrue(response.get("success").asBoolean());
        assertTrue(response.get("data").isArray());
        assertTrue(response.get("data").size() >= 2);
    }

    @Test
    void getCommentsByImage_ShouldReturnImageComments() throws Exception {
        Long imageId = 1L;
        CommentRequest comment1 = createCommentRequestWithSpecificData(1L, imageId, "Comment 1");
        CommentRequest comment2 = createCommentRequestWithSpecificData(2L, imageId, "Comment 2");

        webTestClient.post().uri("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(comment1)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post().uri("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(comment2)
                .exchange()
                .expectStatus().isCreated();

        String responseBody = webTestClient.get()
                .uri("/api/comments/image/{imageId}", imageId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertTrue(response.get("success").asBoolean());
        assertTrue(response.get("data").isArray());
        assertTrue(response.get("data").size() >= 2);
    }

    @Test
    void deleteAllCommentsByImage_ShouldRemoveImageComments() throws Exception {
        Long imageId = 1L;
        CommentRequest comment1 = createCommentRequestWithSpecificData(1L, imageId, "Comment 1");
        CommentRequest comment2 = createCommentRequestWithSpecificData(2L, imageId, "Comment 2");

        webTestClient.post().uri("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(comment1)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post().uri("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(comment2)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.delete()
                .uri("/api/comments/image/{imageId}", imageId)
                .exchange()
                .expectStatus().isOk();

        String responseBody = webTestClient.get()
                .uri("/api/comments/image/{imageId}", imageId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertTrue(response.get("success").asBoolean());
        assertTrue(response.get("data").isArray());
        assertEquals(0, response.get("data").size());
    }

    @Test
    void searchComments_ShouldReturnMatchingComments() throws Exception {
        String keyword = "searchtest";
        CommentRequest comment = createCommentRequestWithSpecificData(1L, 1L, "This is a " + keyword + " comment");

        webTestClient.post().uri("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(comment)
                .exchange()
                .expectStatus().isCreated();

        String responseBody = webTestClient.get()
                .uri("/api/comments/search?keyword={keyword}", keyword)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertTrue(response.get("success").asBoolean());
        assertTrue(response.get("data").isArray());
        assertTrue(response.get("data").size() >= 1);
    }

    @Test
    void getImageCommentCount_ShouldReturnCorrectCount() throws Exception {
        Long imageId = 1L;
        CommentRequest comment1 = createCommentRequestWithSpecificData(1L, imageId, "Comment 1");
        CommentRequest comment2 = createCommentRequestWithSpecificData(2L, imageId, "Comment 2");

        webTestClient.post().uri("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(comment1)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post().uri("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(comment2)
                .exchange()
                .expectStatus().isCreated();

        String responseBody = webTestClient.get()
                .uri("/api/comments/count/image/{imageId}", imageId)
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
    void updateCommentContent_WhenCommentExists_ShouldUpdateSuccessfully() throws Exception {
        CommentRequest testComment = createUniqueCommentRequest();

        String createResponse = webTestClient.post()
                .uri("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testComment)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode createdComment = objectMapper.readTree(createResponse).get("data");
        Long commentId = createdComment.get("id").asLong();

        UpdateContentRequest updateRequest = new UpdateContentRequest("Updated content");

        webTestClient.patch()
                .uri("/api/comments/{id}/content", commentId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
    }

    @Test
    void createComment_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        CommentRequest invalidComment = new CommentRequest(
                "",
                null,
                null
        );

        String responseBody = webTestClient.post()
                .uri("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidComment)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode response = objectMapper.readTree(responseBody);
        assertFalse(response.get("success").asBoolean());
    }
}