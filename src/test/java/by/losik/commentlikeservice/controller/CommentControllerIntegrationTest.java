package by.losik.commentlikeservice.controller;

import by.losik.commentlikeservice.config.TestSecurityConfig;
import by.losik.commentlikeservice.entity.Comment;
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
import java.util.UUID;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebServiceClient
@Testcontainers
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

    private Comment createUniqueComment() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return new Comment(
                null,
                "Test comment content " + uniqueId,
                LocalDateTime.now(),
                1L,
                1L
        );
    }

    private Comment createCommentWithSpecificData(Long userId, Long imageId, String content) {
        return new Comment(
                null,
                content,
                LocalDateTime.now(),
                userId,
                imageId
        );
    }

    @BeforeEach
    void cleanup() {
        List<Comment> comments = webTestClient.get()
                .uri("/api/comments")
                .exchange()
                .expectStatus().isAccepted()
                .returnResult(Comment.class)
                .getResponseBody()
                .collectList()
                .block();

        if (comments != null) {
            for (Comment comment : comments) {
                webTestClient.delete()
                        .uri("/api/comments/{id}", comment.getId())
                        .exchange()
                        .expectStatus().isNoContent();
            }
        }
    }

    @Test
    void getAllComments_ShouldReturnAllComments() {
        Comment comment1 = createUniqueComment();
        Comment comment2 = createUniqueComment();

        webTestClient.post().uri("/api/comments").contentType(MediaType.APPLICATION_JSON).bodyValue(comment1).exchange();
        webTestClient.post().uri("/api/comments").contentType(MediaType.APPLICATION_JSON).bodyValue(comment2).exchange();

        webTestClient.get()
                .uri("/api/comments")
                .exchange()
                .expectStatus().isAccepted()
                .expectBodyList(Comment.class)
                .value(comments -> {
                    assertTrue(comments.size() >= 2);
                    assertTrue(comments.stream().anyMatch(c -> comment1.getContent().equals(c.getContent())));
                    assertTrue(comments.stream().anyMatch(c -> comment2.getContent().equals(c.getContent())));
                });
    }

    @Test
    void getCommentById_WhenCommentExists_ShouldReturnComment() {
        Comment testComment = createUniqueComment();

        Comment createdComment = webTestClient.post()
                .uri("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testComment)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Comment.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(createdComment);
        assertNotNull(createdComment.getId());

        webTestClient.get()
                .uri("/api/comments/{id}", createdComment.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Comment.class)
                .value(comment -> {
                    assertEquals(createdComment.getId(), comment.getId());
                    assertEquals(testComment.getContent(), comment.getContent());
                    assertEquals(testComment.getUserId(), comment.getUserId());
                    assertEquals(testComment.getImageId(), comment.getImageId());
                });
    }

    @Test
    void getCommentById_WhenCommentNotExists_ShouldReturnNotFound() {
        webTestClient.get()
                .uri("/api/comments/999")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void createComment_ShouldCreateCommentSuccessfully() {
        Comment testComment = createUniqueComment();

        webTestClient.post()
                .uri("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testComment)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Comment.class)
                .value(comment -> {
                    assertNotNull(comment.getId());
                    assertEquals(testComment.getContent(), comment.getContent());
                    assertEquals(testComment.getUserId(), comment.getUserId());
                    assertEquals(testComment.getImageId(), comment.getImageId());
                    assertNotNull(comment.getCreatedAt());
                });
    }

    @Test
    void createCommentForImage_ShouldCreateCommentSuccessfully() {
        Long userId = 1L;
        Long imageId = 1L;
        String content = "Test comment content";

        webTestClient.post()
                .uri("/api/comments/user/{userId}/image/{imageId}?content={content}", userId, imageId, content)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Comment.class)
                .value(comment -> {
                    assertNotNull(comment.getId());
                    assertEquals(content, comment.getContent());
                    assertEquals(userId, comment.getUserId());
                    assertEquals(imageId, comment.getImageId());
                    assertNotNull(comment.getCreatedAt());
                });
    }

    @Test
    void updateComment_WhenCommentExists_ShouldUpdateSuccessfully() {
        Comment testComment = createUniqueComment();

        Comment createdComment = webTestClient.post()
                .uri("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testComment)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Comment.class)
                .returnResult()
                .getResponseBody();

        Comment updatedComment = new Comment(
                createdComment.getId(),
                "Updated comment content",
                LocalDateTime.now(),
                2L,
                2L
        );

        webTestClient.put()
                .uri("/api/comments/{id}", createdComment.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updatedComment)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Comment.class)
                .value(comment -> {
                    assertEquals(updatedComment.getContent(), comment.getContent());
                    assertEquals(updatedComment.getUserId(), comment.getUserId());
                    assertEquals(updatedComment.getImageId(), comment.getImageId());
                });
    }

    @Test
    void updateComment_WhenCommentNotExists_ShouldReturnNotFound() {
        Comment nonExistentComment = new Comment(
                999L,
                "Nonexistent comment",
                LocalDateTime.now(),
                1L,
                1L
        );

        webTestClient.put()
                .uri("/api/comments/999")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(nonExistentComment)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteComment_WhenCommentExists_ShouldDeleteSuccessfully() {
        Comment testComment = createUniqueComment();

        Comment createdComment = webTestClient.post()
                .uri("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testComment)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Comment.class)
                .returnResult()
                .getResponseBody();

        webTestClient.delete()
                .uri("/api/comments/{id}", createdComment.getId())
                .exchange()
                .expectStatus().isNoContent();

        webTestClient.get()
                .uri("/api/comments/{id}", createdComment.getId())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteComment_WhenCommentNotExists_ShouldReturnNotFound() {
        webTestClient.delete()
                .uri("/api/comments/999")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getCommentsByUser_ShouldReturnUserComments() {
        Long userId = 1L;
        Comment comment1 = createCommentWithSpecificData(userId, 1L, "Comment 1");
        Comment comment2 = createCommentWithSpecificData(userId, 2L, "Comment 2");

        webTestClient.post().uri("/api/comments").contentType(MediaType.APPLICATION_JSON).bodyValue(comment1).exchange();
        webTestClient.post().uri("/api/comments").contentType(MediaType.APPLICATION_JSON).bodyValue(comment2).exchange();

        webTestClient.get()
                .uri("/api/comments/user/{userId}", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Comment.class)
                .value(comments -> {
                    assertTrue(comments.size() >= 2);
                    assertTrue(comments.stream().allMatch(comment -> userId.equals(comment.getUserId())));
                });
    }

    @Test
    void getCommentsByImage_ShouldReturnImageComments() {
        Long imageId = 1L;
        Comment comment1 = createCommentWithSpecificData(1L, imageId, "Comment 1");
        Comment comment2 = createCommentWithSpecificData(2L, imageId, "Comment 2");

        webTestClient.post().uri("/api/comments").contentType(MediaType.APPLICATION_JSON).bodyValue(comment1).exchange();
        webTestClient.post().uri("/api/comments").contentType(MediaType.APPLICATION_JSON).bodyValue(comment2).exchange();

        webTestClient.get()
                .uri("/api/comments/image/{imageId}", imageId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Comment.class)
                .value(comments -> {
                    assertTrue(comments.size() >= 2);
                    assertTrue(comments.stream().allMatch(comment -> imageId.equals(comment.getImageId())));
                });
    }

    @Test
    void getCommentsByUserAndImage_ShouldReturnFilteredComments() {
        Long userId = 1L;
        Long imageId = 1L;
        Comment comment = createCommentWithSpecificData(userId, imageId, "Specific comment");

        webTestClient.post().uri("/api/comments").contentType(MediaType.APPLICATION_JSON).bodyValue(comment).exchange();

        webTestClient.get()
                .uri("/api/comments/user/{userId}/image/{imageId}", userId, imageId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Comment.class)
                .value(comments -> {
                    assertTrue(comments.size() >= 1);
                    assertTrue(comments.stream().allMatch(c ->
                            userId.equals(c.getUserId()) && imageId.equals(c.getImageId())));
                });
    }

    @Test
    void deleteAllCommentsByImage_ShouldRemoveImageComments() {
        Long imageId = 1L;
        Comment comment1 = createCommentWithSpecificData(1L, imageId, "Comment 1");
        Comment comment2 = createCommentWithSpecificData(2L, imageId, "Comment 2");

        webTestClient.post().uri("/api/comments").contentType(MediaType.APPLICATION_JSON).bodyValue(comment1).exchange();
        webTestClient.post().uri("/api/comments").contentType(MediaType.APPLICATION_JSON).bodyValue(comment2).exchange();

        webTestClient.delete()
                .uri("/api/comments/image/{imageId}", imageId)
                .exchange()
                .expectStatus().isNoContent();

        webTestClient.get()
                .uri("/api/comments/image/{imageId}", imageId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Comment.class)
                .value(comments -> assertTrue(comments.isEmpty()));
    }

    @Test
    void deleteAllCommentsByUser_ShouldRemoveUserComments() {
        Long userId = 1L;
        Comment comment1 = createCommentWithSpecificData(userId, 1L, "Comment 1");
        Comment comment2 = createCommentWithSpecificData(userId, 2L, "Comment 2");

        webTestClient.post().uri("/api/comments").contentType(MediaType.APPLICATION_JSON).bodyValue(comment1).exchange();
        webTestClient.post().uri("/api/comments").contentType(MediaType.APPLICATION_JSON).bodyValue(comment2).exchange();

        webTestClient.delete()
                .uri("/api/comments/user/{userId}", userId)
                .exchange()
                .expectStatus().isNoContent();

        webTestClient.get()
                .uri("/api/comments/user/{userId}", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Comment.class)
                .value(comments -> assertTrue(comments.isEmpty()));
    }

    @Test
    void searchComments_ShouldReturnMatchingComments() {
        String keyword = "searchtest";
        Comment comment = createCommentWithSpecificData(1L, 1L, "This is a " + keyword + " comment");

        webTestClient.post().uri("/api/comments").contentType(MediaType.APPLICATION_JSON).bodyValue(comment).exchange();

        webTestClient.get()
                .uri("/api/comments/search?keyword={keyword}", keyword)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Comment.class)
                .value(comments -> {
                    assertTrue(comments.size() >= 1);
                    assertTrue(comments.stream().anyMatch(c -> c.getContent().contains(keyword)));
                });
    }

    @Test
    void getCommentsAfterDate_ShouldReturnRecentComments() {
        LocalDateTime testDate = LocalDateTime.now().minusDays(1);
        Comment recentComment = createUniqueComment();

        webTestClient.post().uri("/api/comments").contentType(MediaType.APPLICATION_JSON).bodyValue(recentComment).exchange();

        webTestClient.get()
                .uri("/api/comments/after/{date}", testDate)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Comment.class)
                .value(comments -> {
                    assertTrue(comments.size() >= 1);
                    assertTrue(comments.stream().allMatch(comment ->
                            comment.getCreatedAt().isAfter(testDate)));
                });
    }

    @Test
    void getImageCommentCount_ShouldReturnCorrectCount() {
        Long imageId = 1L;
        Comment comment1 = createCommentWithSpecificData(1L, imageId, "Comment 1");
        Comment comment2 = createCommentWithSpecificData(2L, imageId, "Comment 2");

        webTestClient.post().uri("/api/comments").contentType(MediaType.APPLICATION_JSON).bodyValue(comment1).exchange();
        webTestClient.post().uri("/api/comments").contentType(MediaType.APPLICATION_JSON).bodyValue(comment2).exchange();

        webTestClient.get()
                .uri("/api/comments/count/image/{imageId}", imageId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.count").isEqualTo(2);
    }

    @Test
    void getUserCommentCount_ShouldReturnCorrectCount() {
        Long userId = 1L;
        Comment comment1 = createCommentWithSpecificData(userId, 1L, "Comment 1");
        Comment comment2 = createCommentWithSpecificData(userId, 2L, "Comment 2");

        webTestClient.post().uri("/api/comments").contentType(MediaType.APPLICATION_JSON).bodyValue(comment1).exchange();
        webTestClient.post().uri("/api/comments").contentType(MediaType.APPLICATION_JSON).bodyValue(comment2).exchange();

        webTestClient.get()
                .uri("/api/comments/count/user/{userId}", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.count").isEqualTo(2);
    }

    @Test
    void updateCommentContent_WhenCommentExists_ShouldUpdateSuccessfully() {
        Comment testComment = createUniqueComment();

        Comment createdComment = webTestClient.post()
                .uri("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testComment)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Comment.class)
                .returnResult()
                .getResponseBody();

        String newContent = "Updated content";

        webTestClient.patch()
                .uri("/api/comments/{id}/content?content={content}", createdComment.getId(), newContent)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.updated").isEqualTo(true);
    }

    @Test
    void updateCommentContent_WhenCommentNotExists_ShouldReturnNotFound() {
        webTestClient.patch()
                .uri("/api/comments/999/content?content=test")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getUserRecentComments_ShouldReturnLimitedComments() {
        Long userId = 1L;
        Comment comment1 = createCommentWithSpecificData(userId, 1L, "Comment 1");
        Comment comment2 = createCommentWithSpecificData(userId, 2L, "Comment 2");
        Comment comment3 = createCommentWithSpecificData(userId, 3L, "Comment 3");

        webTestClient.post().uri("/api/comments").contentType(MediaType.APPLICATION_JSON).bodyValue(comment1).exchange();
        webTestClient.post().uri("/api/comments").contentType(MediaType.APPLICATION_JSON).bodyValue(comment2).exchange();
        webTestClient.post().uri("/api/comments").contentType(MediaType.APPLICATION_JSON).bodyValue(comment3).exchange();

        webTestClient.get()
                .uri("/api/comments/user/{userId}/recent?limit=2", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Comment.class)
                .value(comments -> assertTrue(comments.size() <= 2));
    }

    @Test
    void createComment_WithInvalidData_ShouldReturnBadRequest() {
        Comment invalidComment = new Comment(
                null,
                "", // Empty content
                null, // Null creation date
                null, // Null user ID
                null  // Null image ID
        );

        webTestClient.post()
                .uri("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidComment)
                .exchange()
                .expectStatus().isBadRequest();
    }
}