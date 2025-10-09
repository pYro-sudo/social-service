package by.losik.commentlikeservice.controller;

import by.losik.commentlikeservice.config.TestSecurityConfig;
import by.losik.commentlikeservice.entity.Comment;
import by.losik.commentlikeservice.entity.Like;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebServiceClient
@Testcontainers
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
    }

    @Autowired
    private WebTestClient webTestClient;

    private Like createUniqueLike() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return new Like(
                null,
                Long.parseLong(uniqueId.substring(0, 4)),
                Long.parseLong(uniqueId.substring(4, 8)),
                LocalDateTime.now()
        );
    }

    private Like createLikeWithSpecificData(Long userId, Long imageId) {
        return new Like(
                null,
                userId,
                imageId,
                LocalDateTime.now()
        );
    }

    @BeforeEach
    void cleanup() {
        List<Comment> comments = webTestClient.get()
                .uri("/api/likes")
                .exchange()
                .expectStatus().isOk()
                .returnResult(Comment.class)
                .getResponseBody()
                .collectList()
                .block();

        if (comments != null) {
            for (Comment comment : comments) {
                webTestClient.delete()
                        .uri("/api/likes/{id}", comment.getId())
                        .exchange()
                        .expectStatus().isNoContent();
            }
        }
    }

    @Test
    void getAllLikes_ShouldReturnAllLikes() {
        Like like1 = createUniqueLike();
        Like like2 = createUniqueLike();

        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like1).exchange();
        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like2).exchange();

        webTestClient.get()
                .uri("/api/likes")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Like.class)
                .value(likes -> {
                    assertTrue(likes.size() >= 2);
                    assertTrue(likes.stream().anyMatch(l -> like1.getUserId().equals(l.getUserId())));
                    assertTrue(likes.stream().anyMatch(l -> like2.getUserId().equals(l.getUserId())));
                });
    }

    @Test
    void getLikeById_WhenLikeExists_ShouldReturnLike() {
        Like testLike = createUniqueLike();

        Like createdLike = webTestClient.post()
                .uri("/api/likes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testLike)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Like.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(createdLike);
        assertNotNull(createdLike.getId());

        webTestClient.get()
                .uri("/api/likes/{id}", createdLike.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Like.class)
                .value(like -> {
                    assertEquals(createdLike.getId(), like.getId());
                    assertEquals(testLike.getUserId(), like.getUserId());
                    assertEquals(testLike.getImageId(), like.getImageId());
                });
    }

    @Test
    void getLikeById_WhenLikeNotExists_ShouldReturnNotFound() {
        webTestClient.get()
                .uri("/api/likes/999")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void createLike_ShouldCreateLikeSuccessfully() {
        Like testLike = createUniqueLike();

        webTestClient.post()
                .uri("/api/likes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testLike)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Like.class)
                .value(like -> {
                    assertNotNull(like.getId());
                    assertEquals(testLike.getUserId(), like.getUserId());
                    assertEquals(testLike.getImageId(), like.getImageId());
                    assertNotNull(like.getCreatedAt());
                });
    }

    @Test
    void toggleLike_WhenLikeNotExists_ShouldCreateLike() {
        Long userId = 1L;
        Long imageId = 1L;

        webTestClient.post()
                .uri("/api/likes/toggle?userId={userId}&imageId={imageId}", userId, imageId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.action").isEqualTo("liked");

        // Verify like was created
        webTestClient.get()
                .uri("/api/likes/check?userId={userId}&imageId={imageId}", userId, imageId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.liked").isEqualTo(true);
    }

    @Test
    void toggleLike_WhenLikeExists_ShouldRemoveLike() {
        Long userId = 1L;
        Long imageId = 1L;

        // First toggle to create like
        webTestClient.post()
                .uri("/api/likes/toggle?userId={userId}&imageId={imageId}", userId, imageId)
                .exchange();

        // Second toggle to remove like
        webTestClient.post()
                .uri("/api/likes/toggle?userId={userId}&imageId={imageId}", userId, imageId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.action").isEqualTo("unliked");

        // Verify like was removed
        webTestClient.get()
                .uri("/api/likes/check?userId={userId}&imageId={imageId}", userId, imageId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.liked").isEqualTo(false);
    }

    @Test
    void deleteLike_WhenLikeExists_ShouldDeleteSuccessfully() {
        Like testLike = createUniqueLike();

        Like createdLike = webTestClient.post()
                .uri("/api/likes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testLike)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Like.class)
                .returnResult()
                .getResponseBody();

        webTestClient.delete()
                .uri("/api/likes/{id}", createdLike.getId())
                .exchange()
                .expectStatus().isNoContent();

        webTestClient.get()
                .uri("/api/likes/{id}", createdLike.getId())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteLike_WhenLikeNotExists_ShouldReturnNotFound() {
        webTestClient.delete()
                .uri("/api/likes/999")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteLikeByUserAndImage_ShouldDeleteSuccessfully() {
        Long userId = 1L;
        Long imageId = 1L;
        Like testLike = createLikeWithSpecificData(userId, imageId);

        webTestClient.post()
                .uri("/api/likes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testLike)
                .exchange();

        webTestClient.delete()
                .uri("/api/likes?userId={userId}&imageId={imageId}", userId, imageId)
                .exchange()
                .expectStatus().isNoContent();

        // Verify like was deleted
        webTestClient.get()
                .uri("/api/likes/check?userId={userId}&imageId={imageId}", userId, imageId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.liked").isEqualTo(false);
    }

    @Test
    void getLikesByUser_ShouldReturnUserLikes() {
        Long userId = 1L;
        Like like1 = createLikeWithSpecificData(userId, 1L);
        Like like2 = createLikeWithSpecificData(userId, 2L);

        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like1).exchange();
        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like2).exchange();

        webTestClient.get()
                .uri("/api/likes/user/{userId}", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Like.class)
                .value(likes -> {
                    assertTrue(likes.size() >= 2);
                    assertTrue(likes.stream().allMatch(like -> userId.equals(like.getUserId())));
                });
    }

    @Test
    void getLikesByImage_ShouldReturnImageLikes() {
        Long imageId = 1L;
        Like like1 = createLikeWithSpecificData(1L, imageId);
        Like like2 = createLikeWithSpecificData(2L, imageId);

        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like1).exchange();
        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like2).exchange();

        webTestClient.get()
                .uri("/api/likes/image/{imageId}", imageId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Like.class)
                .value(likes -> {
                    assertTrue(likes.size() >= 2);
                    assertTrue(likes.stream().allMatch(like -> imageId.equals(like.getImageId())));
                });
    }

    @Test
    void getLikeCountByImage_ShouldReturnCorrectCount() {
        Long imageId = 1L;
        Like like1 = createLikeWithSpecificData(1L, imageId);
        Like like2 = createLikeWithSpecificData(2L, imageId);

        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like1).exchange();
        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like2).exchange();

        webTestClient.get()
                .uri("/api/likes/image/{imageId}/count", imageId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.count").isEqualTo(2);
    }

    @Test
    void getLikeCountByUser_ShouldReturnCorrectCount() {
        Long userId = 1L;
        Like like1 = createLikeWithSpecificData(userId, 1L);
        Like like2 = createLikeWithSpecificData(userId, 2L);

        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like1).exchange();
        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like2).exchange();

        webTestClient.get()
                .uri("/api/likes/user/{userId}/count", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.count").isEqualTo(2);
    }

    @Test
    void checkIfLiked_WhenLiked_ShouldReturnTrue() {
        Long userId = 1L;
        Long imageId = 1L;
        Like like = createLikeWithSpecificData(userId, imageId);

        webTestClient.post()
                .uri("/api/likes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(like)
                .exchange();

        webTestClient.get()
                .uri("/api/likes/check?userId={userId}&imageId={imageId}", userId, imageId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.liked").isEqualTo(true);
    }

    @Test
    void checkIfLiked_WhenNotLiked_ShouldReturnFalse() {
        webTestClient.get()
                .uri("/api/likes/check?userId=999&imageId=999")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.liked").isEqualTo(false);
    }

    @Test
    void deleteAllLikesByImage_ShouldRemoveImageLikes() {
        Long imageId = 1L;
        Like like1 = createLikeWithSpecificData(1L, imageId);
        Like like2 = createLikeWithSpecificData(2L, imageId);

        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like1).exchange();
        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like2).exchange();

        webTestClient.delete()
                .uri("/api/likes/image/{imageId}", imageId)
                .exchange()
                .expectStatus().isNoContent();

        webTestClient.get()
                .uri("/api/likes/image/{imageId}", imageId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Like.class)
                .value(likes -> assertTrue(likes.isEmpty()));
    }

    @Test
    void deleteAllLikesByUser_ShouldRemoveUserLikes() {
        Long userId = 1L;
        Like like1 = createLikeWithSpecificData(userId, 1L);
        Like like2 = createLikeWithSpecificData(userId, 2L);

        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like1).exchange();
        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like2).exchange();

        webTestClient.delete()
                .uri("/api/likes/user/{userId}", userId)
                .exchange()
                .expectStatus().isNoContent();

        webTestClient.get()
                .uri("/api/likes/user/{userId}", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Like.class)
                .value(likes -> assertTrue(likes.isEmpty()));
    }

    @Test
    void getLikesAfterDate_ShouldReturnRecentLikes() {
        LocalDateTime testDate = LocalDateTime.now().minusDays(1);
        Like recentLike = createUniqueLike();

        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(recentLike).exchange();

        webTestClient.get()
                .uri("/api/likes/after/{date}", testDate)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Like.class)
                .value(likes -> {
                    assertTrue(likes.size() >= 1);
                    assertTrue(likes.stream().allMatch(like ->
                            like.getCreatedAt().isAfter(testDate)));
                });
    }

    @Test
    void getLikesBeforeDate_ShouldReturnOlderLikes() {
        LocalDateTime testDate = LocalDateTime.now().plusDays(1);
        Like olderLike = createLikeWithSpecificData(1L, 1L);

        // Manually set older creation date
        olderLike.setCreatedAt(LocalDateTime.now().minusDays(2));

        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(olderLike).exchange();

        webTestClient.get()
                .uri("/api/likes/before/{date}", testDate)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Like.class)
                .value(likes -> {
                    assertTrue(likes.size() >= 1);
                    assertTrue(likes.stream().allMatch(like ->
                            like.getCreatedAt().isBefore(testDate)));
                });
    }

    @Test
    void toggleLikeForImage_WithHeader_ShouldWorkCorrectly() {
        long userId = 1L;
        Long imageId = 1L;

        // First call should like
        webTestClient.post()
                .uri("/api/likes/images/{imageId}/likes", imageId)
                .header("X-User-Id", Long.toString(userId))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.action").isEqualTo("liked");

        // Second call should unlike
        webTestClient.post()
                .uri("/api/likes/images/{imageId}/likes", imageId)
                .header("X-User-Id", Long.toString(userId))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.action").isEqualTo("unliked");
    }

    @Test
    void getLikesForImage_ShouldReturnImageLikes() {
        Long imageId = 1L;
        Like like1 = createLikeWithSpecificData(1L, imageId);
        Like like2 = createLikeWithSpecificData(2L, imageId);

        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like1).exchange();
        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like2).exchange();

        webTestClient.get()
                .uri("/api/likes/images/{imageId}/likes", imageId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Like.class)
                .value(likes -> {
                    assertTrue(likes.size() >= 2);
                    assertTrue(likes.stream().allMatch(like -> imageId.equals(like.getImageId())));
                });
    }

    @Test
    void getLikeCountForImage_ShouldReturnCorrectCount() {
        Long imageId = 1L;
        Like like1 = createLikeWithSpecificData(1L, imageId);
        Like like2 = createLikeWithSpecificData(2L, imageId);

        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like1).exchange();
        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(like2).exchange();

        webTestClient.get()
                .uri("/api/likes/images/{imageId}/likes/count", imageId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.count").isEqualTo(2);
    }

    @Test
    void createLike_WithInvalidData_ShouldReturnBadRequest() {
        Like invalidLike = new Like(
                null,
                null, // Null user ID
                null, // Null image ID
                null  // Null creation date
        );

        webTestClient.post()
                .uri("/api/likes")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidLike)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void getLikesBetweenDates_ShouldReturnFilteredLikes() {
        LocalDateTime startDate = LocalDateTime.now().minusDays(2);
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);
        Like likeInRange = createUniqueLike();

        webTestClient.post().uri("/api/likes").contentType(MediaType.APPLICATION_JSON).bodyValue(likeInRange).exchange();

        webTestClient.get()
                .uri("/api/likes/between?start={start}&end={end}", startDate, endDate)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Like.class)
                .value(likes -> {
                    assertTrue(likes.size() >= 1);
                    assertTrue(likes.stream().allMatch(like ->
                            !like.getCreatedAt().isBefore(startDate) &&
                                    !like.getCreatedAt().isAfter(endDate)));
                });
    }
}