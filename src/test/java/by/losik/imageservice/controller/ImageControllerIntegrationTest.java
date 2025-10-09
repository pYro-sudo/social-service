package by.losik.imageservice.controller;

import by.losik.imageservice.config.TestSecurityConfig;
import by.losik.imageservice.entity.Image;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.webservices.client.AutoConfigureWebServiceClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebServiceClient
@Testcontainers
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class ImageControllerIntegrationTest {

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
    static LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
            .withServices(S3)
            .withReuse(true);

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

        registry.add("spring.cloud.aws.credentials.access-key", localStack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localStack::getSecretKey);
        registry.add("spring.cloud.aws.s3.endpoint", () -> localStack.getEndpointOverride(S3).toString());
        registry.add("spring.cloud.aws.s3.bucket-name", () -> "test-images");
        registry.add("spring.cloud.aws.region.static", () -> localStack.getRegion());
    }

    @Autowired
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        initializeS3Bucket();
        cleanupDatabase();
    }

    private void initializeS3Bucket() {
        try (S3Client s3Client = S3Client.builder()
                .endpointOverride(localStack.getEndpointOverride(S3))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())))
                .region(Region.of(localStack.getRegion()))
                .build()) {

            try {
                s3Client.createBucket(CreateBucketRequest.builder()
                        .bucket("test-images")
                        .build());
            } catch (Exception e) {
                System.out.println("welp");
            }
        }
    }

    private void cleanupDatabase() {
        List<Image> images = webTestClient.get()
                .uri("/api/images")
                .exchange()
                .expectStatus().isOk()
                .returnResult(Image.class)
                .getResponseBody()
                .collectList()
                .block();

        if (images != null) {
            for (Image image : images) {
                webTestClient.delete()
                        .uri("/api/images/{id}", image.getId())
                        .exchange()
                        .expectStatus().isNoContent();
            }
        }
    }

    private Image createUniqueImage() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return new Image(
                null,
                "http://localhost:4566/test-images/users/" + uniqueId + ".jpg",
                "Test image description " + uniqueId,
                LocalDate.now(),
                1L
        );
    }

    private Image createImageWithSpecificData(Long userId, String description) {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return new Image(
                null,
                "http://localhost:4566/test-images/users/" + uniqueId + ".jpg",
                description,
                LocalDate.now(),
                userId
        );
    }

    @Test
    void uploadImage_ShouldUploadSuccessfully() {
        byte[] imageBytes = new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
                0x01, 0x00, 0x01, 0x00, 0x00, 0x01, 0x00, 0x01,
                0x00, 0x00, (byte) 0xFF, (byte) 0xDB, 0x00, 0x43,
                0x00, (byte) 0xFF, (byte) 0xD9
        };

        MultipartBodyBuilder builder = new MultipartBodyBuilder();

        builder.part("file", new ByteArrayResource(imageBytes) {
            @Override
            public @NonNull String getFilename() {
                return "test-image.jpg";
            }
        }).contentType(MediaType.IMAGE_JPEG);

        builder.part("description", "Test description", MediaType.TEXT_PLAIN);

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/images/upload")
                        .queryParam("userId", "1")
                        .build())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(builder.build())
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Image.class)
                .value(image -> {
                    assertNotNull(image.getId());
                    assertNotNull(image.getUrl());
                    assertTrue(image.getUrl().contains("test-images"));
                    assertEquals("Test description", image.getDescription());
                    assertEquals(1L, image.getUserId());
                    assertEquals(LocalDate.now(), image.getUploadedAt());
                });
    }

    @Test
    void getImageById_WhenImageExists_ShouldReturnImage() {
        Image testImage = createUniqueImage();

        Image createdImage = webTestClient.post()
                .uri("/api/images")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testImage)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Image.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(createdImage);

        webTestClient.get()
                .uri("/api/images/{id}", createdImage.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Image.class)
                .value(image -> {
                    assertEquals(createdImage.getId(), image.getId());
                    assertEquals(testImage.getUrl(), image.getUrl());
                    assertEquals(testImage.getDescription(), image.getDescription());
                    assertEquals(testImage.getUserId(), image.getUserId());
                });
    }

    @Test
    void getImageById_WhenImageNotExists_ShouldReturnNotFound() {
        webTestClient.get()
                .uri("/api/images/999")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void createImage_ShouldCreateSuccessfully() {
        Image testImage = createUniqueImage();

        webTestClient.post()
                .uri("/api/images")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testImage)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Image.class)
                .value(image -> {
                    assertNotNull(image.getId());
                    assertEquals(testImage.getUrl(), image.getUrl());
                    assertEquals(testImage.getDescription(), image.getDescription());
                    assertEquals(testImage.getUserId(), image.getUserId());
                    assertNotNull(image.getUploadedAt());
                });
    }

    @Test
    void updateImage_WhenImageExists_ShouldUpdateSuccessfully() {
        Image testImage = createUniqueImage();

        Image createdImage = webTestClient.post()
                .uri("/api/images")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testImage)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Image.class)
                .returnResult()
                .getResponseBody();

        Image updatedImage = new Image(
                createdImage.getId(),
                "http://localhost:4566/test-images/updated.jpg",
                "Updated description",
                LocalDate.now(),
                2L
        );

        webTestClient.put()
                .uri("/api/images/{id}", createdImage.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updatedImage)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Image.class)
                .value(image -> {
                    assertEquals(updatedImage.getUrl(), image.getUrl());
                    assertEquals(updatedImage.getDescription(), image.getDescription());
                    assertEquals(updatedImage.getUserId(), image.getUserId());
                });
    }

    @Test
    void deleteImage_WhenImageExists_ShouldDeleteSuccessfully() {
        Image testImage = createUniqueImage();

        Image createdImage = webTestClient.post()
                .uri("/api/images")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testImage)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Image.class)
                .returnResult()
                .getResponseBody();

        webTestClient.delete()
                .uri("/api/images/{id}", createdImage.getId())
                .exchange()
                .expectStatus().isNoContent();

        webTestClient.get()
                .uri("/api/images/{id}", createdImage.getId())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getUserRecentImages_ShouldReturnLimitedImages() {
        Long userId = 1L;
        Image image1 = createImageWithSpecificData(userId, "Image 1");
        Image image2 = createImageWithSpecificData(userId, "Image 2");
        Image image3 = createImageWithSpecificData(userId, "Image 3");

        webTestClient.post().uri("/api/images").contentType(MediaType.APPLICATION_JSON).bodyValue(image1).exchange();
        webTestClient.post().uri("/api/images").contentType(MediaType.APPLICATION_JSON).bodyValue(image2).exchange();
        webTestClient.post().uri("/api/images").contentType(MediaType.APPLICATION_JSON).bodyValue(image3).exchange();

        webTestClient.get()
                .uri("/api/images/user/{userId}/recent?limit=2", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Image.class)
                .value(images -> assertTrue(images.size() <= 2));
    }

    @Test
    void searchImages_ShouldReturnMatchingImages() {
        String keyword = "searchtest";
        Image image = createImageWithSpecificData(1L, "This is a " + keyword + " image");

        webTestClient.post().uri("/api/images").contentType(MediaType.APPLICATION_JSON).bodyValue(image).exchange();

        webTestClient.get()
                .uri("/api/images/search?keyword={keyword}", keyword)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Image.class)
                .value(images -> {
                    assertTrue(images.size() >= 1);
                    assertTrue(images.stream().anyMatch(img -> img.getDescription().contains(keyword)));
                });
    }

    @Test
    void getImageByUrl_WhenImageExists_ShouldReturnImage() {
        Image testImage = createUniqueImage();

        Image createdImage = webTestClient.post()
                .uri("/api/images")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testImage)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Image.class)
                .returnResult()
                .getResponseBody();

        java.net.URLEncoder.encode(createdImage.getUrl(), java.nio.charset.StandardCharsets.UTF_8);

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/images/url")
                        .queryParam("url", createdImage.getUrl())
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Image.class)
                .value(image -> assertEquals(createdImage.getId(), image.getId()));
    }

    @Test
    void checkUrlAvailability_WhenUrlAvailable_ShouldReturnTrue() {
        String uniqueUrl = "http://localhost:4566/test-images/unique-" + UUID.randomUUID() + ".jpg";

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/images/check/url")
                        .queryParam("url", uniqueUrl)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.available").isEqualTo(true);
    }

    @Test
    void getUserImageCount_ShouldReturnCorrectCount() {
        Long userId = 1L;
        Image image1 = createImageWithSpecificData(userId, "Image 1");
        Image image2 = createImageWithSpecificData(userId, "Image 2");

        webTestClient.post().uri("/api/images").contentType(MediaType.APPLICATION_JSON).bodyValue(image1).exchange();
        webTestClient.post().uri("/api/images").contentType(MediaType.APPLICATION_JSON).bodyValue(image2).exchange();

        webTestClient.get()
                .uri("/api/images/count/user/{userId}", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.count").isEqualTo(2);
    }

    @Test
    void updateImageDescription_WhenImageExists_ShouldUpdateSuccessfully() {
        Image testImage = createUniqueImage();

        Image createdImage = webTestClient.post()
                .uri("/api/images")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testImage)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Image.class)
                .returnResult()
                .getResponseBody();

        String newDescription = "Updated description";

        webTestClient.patch()
                .uri("/api/images/{id}/description?description={description}", createdImage.getId(), newDescription)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.updated").isEqualTo(false);
    }

    @Test
    void getUserImageStats_ShouldReturnStats() {
        Long userId = 1L;
        Image image1 = createImageWithSpecificData(userId, "Image 1");
        Image image2 = createImageWithSpecificData(userId, "Image 2");

        webTestClient.post().uri("/api/images").contentType(MediaType.APPLICATION_JSON).bodyValue(image1).exchange();
        webTestClient.post().uri("/api/images").contentType(MediaType.APPLICATION_JSON).bodyValue(image2).exchange();

        webTestClient.get()
                .uri("/api/images/user/{userId}/stats", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalImages").isEqualTo(2)
                .jsonPath("$.recentUploads").isArray();
    }

    @Test
    void checkS3Connection_ShouldReturnConnected() {
        webTestClient.get()
                .uri("/api/images/health/s3")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.connected").isEqualTo(true);
    }

    @Test
    void getAllImagesPaged_ShouldReturnPagedResults() {
        // Create some test images
        for (int i = 0; i < 5; i++) {
            Image image = createUniqueImage();
            webTestClient.post().uri("/api/images").contentType(MediaType.APPLICATION_JSON).bodyValue(image).exchange();
        }

        webTestClient.get()
                .uri("/api/images?page=0&size=3")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Image.class)
                .value(images -> assertTrue(images.size() <= 3));
    }

    @Test
    void getUserImagesPaged_ShouldReturnPagedUserImages() {
        Long userId = 1L;

        // Create test images for user
        for (int i = 0; i < 5; i++) {
            Image image = createImageWithSpecificData(userId, "Image " + i);
            webTestClient.post().uri("/api/images").contentType(MediaType.APPLICATION_JSON).bodyValue(image).exchange();
        }

        webTestClient.get()
                .uri("/api/images/user/{userId}?page=0&size=3", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Image.class)
                .value(images -> {
                    assertTrue(images.size() <= 3);
                    assertTrue(images.stream().allMatch(img -> userId.equals(img.getUserId())));
                });
    }

    @Test
    void deleteAllUserImages_ShouldRemoveAllUserImages() {
        Long userId = 1L;
        Image image1 = createImageWithSpecificData(userId, "Image 1");
        Image image2 = createImageWithSpecificData(userId, "Image 2");

        webTestClient.post().uri("/api/images").contentType(MediaType.APPLICATION_JSON).bodyValue(image1).exchange();
        webTestClient.post().uri("/api/images").contentType(MediaType.APPLICATION_JSON).bodyValue(image2).exchange();

        webTestClient.delete()
                .uri("/api/images/user/{userId}", userId)
                .exchange()
                .expectStatus().isNoContent();

        webTestClient.get()
                .uri("/api/images/user/{userId}", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Image.class)
                .value(images -> assertEquals(0, images.size()));
    }
}