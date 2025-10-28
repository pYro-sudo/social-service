package by.losik.activityservice.controller

import by.losik.activityservice.config.AbstractIntegrationTest
import by.losik.activityservice.entity.ActivityEvent
import by.losik.activityservice.entity.ActivityEventType
import by.losik.activityservice.entity.ActivityStatus
import by.losik.activityservice.service.ActivityEventService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.LocalDateTime

class ActivityEventControllerTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var activityEventService: ActivityEventService

    private val testEvent = ActivityEvent(
        userId = 1L,
        imageId = 100L,
        type = ActivityEventType.ADD_LIKE,
        status = ActivityStatus.PROCESSED,
        content = "Test content"
    )

    @BeforeEach
    fun setup() {
        webTestClient = WebTestClient.bindToController(ActivityEventController(activityEventService)).build()

        activityEventService.findAll()
            .flatMap { activityEventService.deleteByUserId(it.userId) }
    }

    @Test
    fun `should get all activities`() {
        activityEventService.save(testEvent).block()

        webTestClient.get()
            .uri("/api/activity")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(ActivityEvent::class.java)
            .hasSize(1)
    }

    @Test
    fun `should get activity by id`() {
        val savedEvent = activityEventService.save(testEvent).block()!!

        webTestClient.get()
            .uri("/api/activity/{id}", savedEvent.id)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(savedEvent.id)
            .jsonPath("$.userId").isEqualTo(testEvent.userId)
    }

    @Test
    fun `should return 404 when activity not found`() {
        webTestClient.get()
            .uri("/api/activity/non-existent-id")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `should get activities by user id`() {
        activityEventService.save(testEvent).block()

        webTestClient.get()
            .uri("/api/activity/user/{userId}", 1L)
            .exchange()
            .expectStatus().isOk
            .expectBodyList(ActivityEvent::class.java)
            .hasSize(1)
    }

    @Test
    fun `should get activities by image id`() {
        activityEventService.save(testEvent).block()

        webTestClient.get()
            .uri("/api/activity/image/{imageId}", 100L)
            .exchange()
            .expectStatus().isOk
            .expectBodyList(ActivityEvent::class.java)
            .hasSize(1)
    }

    @Test
    fun `should get activities by user and image`() {
        activityEventService.save(testEvent).block()

        webTestClient.get()
            .uri("/api/activity/user/{userId}/image/{imageId}", 1L, 100L)
            .exchange()
            .expectStatus().isOk
            .expectBodyList(ActivityEvent::class.java)
            .hasSize(1)
    }

    @Test
    fun `should get activities by type`() {
        activityEventService.save(testEvent).block()

        webTestClient.get()
            .uri("/api/activity/type/{type}", ActivityEventType.ADD_LIKE)
            .exchange()
            .expectStatus().isOk
            .expectBodyList(ActivityEvent::class.java)
            .hasSize(1)
    }

    @Test
    fun `should get user stats`() {
        activityEventService.save(testEvent).block()

        webTestClient.get()
            .uri("/api/activity/stats/user/{userId}", 1L)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.ADD_LIKE").isEqualTo(1)
    }

    @Test
    fun `should get image stats`() {
        activityEventService.save(testEvent).block()

        webTestClient.get()
            .uri("/api/activity/stats/image/{imageId}", 100L)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.ADD_LIKE").isEqualTo(1)
    }

    @Test
    fun `should get user recent activities`() {
        activityEventService.save(testEvent).block()

        webTestClient.get()
            .uri("/api/activity/user/{userId}/recent?limit=5", 1L)
            .exchange()
            .expectStatus().isOk
            .expectBodyList(ActivityEvent::class.java)
            .hasSize(1)
    }

    @Test
    fun `should get activities by period`() {
        val startDate = LocalDateTime.now().minusDays(1)
        val endDate = LocalDateTime.now().plusDays(1)

        activityEventService.save(testEvent).block()

        webTestClient.get()
            .uri("/api/activity/period?startDate={startDate}&endDate={endDate}",
                startDate.toString(), endDate.toString())
            .exchange()
            .expectStatus().isOk
            .expectBodyList(ActivityEvent::class.java)
            .hasSize(1)
    }
}