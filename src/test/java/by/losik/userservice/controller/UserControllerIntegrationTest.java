package by.losik.userservice.controller;

import by.losik.userservice.config.TestSecurityConfig;
import by.losik.userservice.entity.Role;
import by.losik.userservice.entity.User;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebServiceClient
@Testcontainers
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class UserControllerIntegrationTest {

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
        registry.add("spring.data.redis.lettuce.pool.min-idle", () -> 0);;

        registry.add("spring.liquibase.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.liquibase.user", postgreSQLContainer::getUsername);
        registry.add("spring.liquibase.password", postgreSQLContainer::getPassword);
        registry.add("spring.liquibase.default-schema", () -> "public");
        registry.add("spring.liquibase.liquibase-schema", () -> "public");

    }

    @Autowired
    private WebTestClient webTestClient;

    private User createUniqueUser() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return User.builder()
                .username("user_" + uniqueId)
                .email("user_" + uniqueId + "@example.com")
                .password("password")
                .userRole(Role.USER)
                .enabled(true)
                .build();
    }

    @BeforeEach
    void cleanup() {
        webTestClient.delete()
                .uri("/api/users")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void getAllUsers_ShouldReturnAllUsers() {
        User user1 = createUniqueUser();
        User user2 = createUniqueUser();

        webTestClient.post().uri("/api/users").contentType(MediaType.APPLICATION_JSON).bodyValue(user1).exchange();
        webTestClient.post().uri("/api/users").contentType(MediaType.APPLICATION_JSON).bodyValue(user2).exchange();

        webTestClient.get()
                .uri("/api/users")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(User.class)
                .value(users -> {
                    assertTrue(users.size() >= 2);
                    assertTrue(users.stream().anyMatch(u -> user1.getUsername().equals(u.getUsername())));
                    assertTrue(users.stream().anyMatch(u -> user2.getUsername().equals(u.getUsername())));
                });
    }

    @Test
    void getUserById_WhenUserExists_ShouldReturnUser() {
        User testUser = createUniqueUser();

        User createdUser = webTestClient.post()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testUser)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(User.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(createdUser);

        webTestClient.get()
                .uri("/api/users/{id}", createdUser.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(User.class)
                .value(user -> {
                    assertEquals(createdUser.getId(), user.getId());
                    assertEquals(testUser.getUsername(), user.getUsername());
                    assertEquals(testUser.getEmail(), user.getEmail());
                });
    }

    @Test
    void getUserById_WhenUserNotExists_ShouldReturnNotFound() {
        webTestClient.get()
                .uri("/api/users/999")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void createUser_ShouldCreateUserSuccessfully() {
        User testUser = createUniqueUser();

        webTestClient.post()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testUser)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(User.class)
                .value(user -> {
                    assertNotNull(user.getId());
                    assertEquals(testUser.getUsername(), user.getUsername());
                    assertEquals(testUser.getEmail(), user.getEmail());
                    assertEquals(testUser.getUserRole(), user.getUserRole());
                });
    }

    @Test
    void updateUser_WhenUserExists_ShouldUpdateSuccessfully() {
        User testUser = createUniqueUser();

        User createdUser = webTestClient.post()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testUser)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(User.class)
                .returnResult()
                .getResponseBody();

        User updatedUser = User.builder()
                .id(createdUser.getId())
                .username("updated_" + UUID.randomUUID().toString().substring(0, 8))
                .email("updated_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com")
                .password("newpassword")
                .userRole(Role.ADMIN)
                .enabled(false)
                .build();

        webTestClient.put()
                .uri("/api/users/{id}", createdUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updatedUser)
                .exchange()
                .expectStatus().isOk()
                .expectBody(User.class)
                .value(user -> {
                    assertEquals(updatedUser.getUsername(), user.getUsername());
                    assertEquals(updatedUser.getEmail(), user.getEmail());
                    assertEquals(Role.ADMIN, user.getUserRole());
                    assertEquals(updatedUser.getEnabled(), user.getEnabled());
                });
    }

    @Test
    void updateUser_WhenUserNotExists_ShouldReturnNotFound() {
        User nonExistentUser = User.builder()
                .id(999L)
                .username("nonexistent")
                .email("nonexistent@example.com")
                .password("password")
                .userRole(Role.USER)
                .enabled(true)
                .build();

        webTestClient.put()
                .uri("/api/users/999")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(nonExistentUser)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteUser_WhenUserExists_ShouldDeleteSuccessfully() {
        User testUser = createUniqueUser();

        User createdUser = webTestClient.post()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testUser)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(User.class)
                .returnResult()
                .getResponseBody();

        webTestClient.delete()
                .uri("/api/users/{id}", createdUser.getId())
                .exchange()
                .expectStatus().isNoContent();

        webTestClient.get()
                .uri("/api/users/{id}", createdUser.getId())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteUser_WhenUserNotExists_ShouldReturnNotFound() {
        webTestClient.delete()
                .uri("/api/users/999")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getUserByUsername_WhenUserExists_ShouldReturnUser() {
        User testUser = createUniqueUser();

        User createdUser = webTestClient.post()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testUser)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(User.class)
                .returnResult()
                .getResponseBody();

        webTestClient.get()
                .uri("/api/users/username/{username}", createdUser.getUsername())
                .exchange()
                .expectStatus().isOk()
                .expectBody(User.class)
                .value(user -> {
                    assertEquals(createdUser.getId(), user.getId());
                    assertEquals(createdUser.getUsername(), user.getUsername());
                    assertEquals(createdUser.getEmail(), user.getEmail());
                });
    }

    @Test
    void getUserByUsername_WhenUserNotExists_ShouldReturnNotFound() {
        webTestClient.get()
                .uri("/api/users/username/nonexistentuser")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getUserByEmail_WhenUserExists_ShouldReturnUser() {
        User testUser = createUniqueUser();

        User createdUser = webTestClient.post()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testUser)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(User.class)
                .returnResult()
                .getResponseBody();

        webTestClient.get()
                .uri("/api/users/email/{email}", createdUser.getEmail())
                .exchange()
                .expectStatus().isOk()
                .expectBody(User.class)
                .value(user -> {
                    assertEquals(createdUser.getId(), user.getId());
                    assertEquals(createdUser.getEmail(), user.getEmail());
                });
    }

    @Test
    void getUserByEmail_WhenUserNotExists_ShouldReturnNotFound() {
        webTestClient.get()
                .uri("/api/users/email/nonexistent@example.com")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void checkUsernameAvailability_WhenUsernameAvailable_ShouldReturnTrue() {
        String uniqueUsername = "available_" + UUID.randomUUID().toString().substring(0, 8);

        webTestClient.get()
                .uri("/api/users/check/username/{username}", uniqueUsername)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.available").isEqualTo(true);
    }

    @Test
    void checkUsernameAvailability_WhenUsernameTaken_ShouldReturnFalse() {
        User testUser = createUniqueUser();

        webTestClient.post()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testUser)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.get()
                .uri("/api/users/check/username/{username}", testUser.getUsername())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.available").isEqualTo(false);
    }

    @Test
    void checkEmailAvailability_WhenEmailAvailable_ShouldReturnTrue() {
        String uniqueEmail = "available_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";

        webTestClient.get()
                .uri("/api/users/check/email/{email}", uniqueEmail)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.available").isEqualTo(true);
    }

    @Test
    void checkEmailAvailability_WhenEmailTaken_ShouldReturnFalse() {
        User testUser = createUniqueUser();

        webTestClient.post()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testUser)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.get()
                .uri("/api/users/check/email/{email}", testUser.getEmail())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.available").isEqualTo(false);
    }

    @Test
    void checkUsernameExists_WhenUsernameExists_ShouldReturnTrue() {
        User testUser = createUniqueUser();

        webTestClient.post()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testUser)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.get()
                .uri("/api/users/exists/username/{username}", testUser.getUsername())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.exists").isEqualTo(true);
    }

    @Test
    void checkUsernameExists_WhenUsernameNotExists_ShouldReturnFalse() {
        webTestClient.get()
                .uri("/api/users/exists/username/nonexistentuser")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.exists").isEqualTo(false);
    }

    @Test
    void checkEmailExists_WhenEmailExists_ShouldReturnTrue() {
        User testUser = createUniqueUser();

        webTestClient.post()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testUser)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.get()
                .uri("/api/users/exists/email/{email}", testUser.getEmail())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.exists").isEqualTo(true);
    }

    @Test
    void checkEmailExists_WhenEmailNotExists_ShouldReturnFalse() {
        webTestClient.get()
                .uri("/api/users/exists/email/nonexistent@example.com")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.exists").isEqualTo(false);
    }

    @Test
    void getUsersByRole_ShouldReturnUsersWithSpecificRole() {
        User adminUser = User.builder()
                .username("admin_" + UUID.randomUUID().toString().substring(0, 8))
                .email("admin_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com")
                .password("password")
                .userRole(Role.ADMIN)
                .enabled(true)
                .build();

        User regularUser = createUniqueUser();

        webTestClient.post().uri("/api/users").contentType(MediaType.APPLICATION_JSON).bodyValue(adminUser).exchange();
        webTestClient.post().uri("/api/users").contentType(MediaType.APPLICATION_JSON).bodyValue(regularUser).exchange();

        webTestClient.get()
                .uri("/api/users/role/{role}", "ADMIN")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(User.class)
                .value(users -> {
                    assertTrue(users.size() >= 1);
                    assertTrue(users.stream().allMatch(user -> user.getUserRole() == Role.ADMIN));
                });
    }

    @Test
    void getUsersByRole_WhenNoUsersWithRole_ShouldReturnEmptyList() {
        webTestClient.get()
                .uri("/api/users/role/{role}", "ADMIN")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(User.class)
                .value(users -> assertTrue(users.isEmpty()));
    }

    @Test
    void checkUserExists_WhenUserExists_ShouldReturnTrue() {
        User testUser = createUniqueUser();

        User createdUser = webTestClient.post()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testUser)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(User.class)
                .returnResult()
                .getResponseBody();

        webTestClient.get()
                .uri("/api/users/exists/{id}", createdUser.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.exists").isEqualTo(true);
    }

    @Test
    void checkUserExists_WhenUserNotExists_ShouldReturnFalse() {
        webTestClient.get()
                .uri("/api/users/exists/999")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.exists").isEqualTo(false);
    }

    @Test
    void deleteAllUsers_ShouldRemoveAllUsers() {
        User user1 = createUniqueUser();
        User user2 = createUniqueUser();

        webTestClient.post().uri("/api/users").contentType(MediaType.APPLICATION_JSON).bodyValue(user1).exchange();
        webTestClient.post().uri("/api/users").contentType(MediaType.APPLICATION_JSON).bodyValue(user2).exchange();

        webTestClient.delete()
                .uri("/api/users")
                .exchange()
                .expectStatus().isNoContent();

        webTestClient.get()
                .uri("/api/users/count")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalUsers").isEqualTo(0);
    }

    @Test
    void createUser_WithInvalidData_ShouldReturnBadRequest() {
        User invalidUser = User.builder()
                .username("")
                .email("invalid-email")
                .password("")
                .userRole(null)
                .build();

        webTestClient.post()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidUser)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void createUser_WithDuplicateUsername_ShouldWork() {
        User user1 = createUniqueUser();
        User user2 = User.builder()
                .username(user1.getUsername())
                .email("different@example.com")
                .password("password")
                .userRole(Role.USER)
                .enabled(true)
                .build();

        webTestClient.post()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user1)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post()
                .uri("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user2)
                .exchange();
    }
}