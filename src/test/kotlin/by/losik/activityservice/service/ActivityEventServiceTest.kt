package by.losik.activityservice.service

import by.losik.activityservice.config.AbstractIntegrationTest
import by.losik.activityservice.entity.ActivityEvent
import by.losik.activityservice.entity.ActivityEventType
import by.losik.activityservice.entity.ActivityStatus
import by.losik.activityservice.exception.NotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.test.StepVerifier
import java.time.LocalDateTime

class ActivityEventServiceTest : AbstractIntegrationTest() {

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
    fun cleanup() {
        activityEventService.findAll()
            .flatMap { activityEventService.deleteByUserId(it.userId) }
    }

    @Test
    fun `should save activity event`() {
        StepVerifier.create(activityEventService.save(testEvent))
            .expectNextMatches { savedEvent ->
                savedEvent.id != null &&
                        savedEvent.userId == testEvent.userId &&
                        savedEvent.imageId == testEvent.imageId &&
                        savedEvent.type == testEvent.type
            }
            .verifyComplete()
    }

    @Test
    fun `should find activity by id`() {
        val savedEvent = activityEventService.save(testEvent).block()!!

        StepVerifier.create(activityEventService.findById(savedEvent.id!!))
            .expectNextMatches { foundEvent ->
                foundEvent.id == savedEvent.id &&
                        foundEvent.userId == testEvent.userId
            }
            .verifyComplete()
    }

    @Test
    fun `should throw exception when activity not found`() {
        StepVerifier.create(activityEventService.findById("non-existent-id"))
            .expectError(NotFoundException::class.java)
            .verify()
    }

    @Test
    fun `should find activities by user id`() {
        activityEventService.save(testEvent).block()
        activityEventService.save(testEvent.copy(userId = 2L)).block()

        StepVerifier.create(activityEventService.findByUserId(1L))
            .expectNextCount(1)
            .verifyComplete()
    }

    @Test
    fun `should find activities by image id`() {
        activityEventService.save(testEvent).block()
        activityEventService.save(testEvent.copy(imageId = 200L)).block()

        StepVerifier.create(activityEventService.findByImageId(100L))
            .expectNextCount(1)
            .verifyComplete()
    }

    @Test
    fun `should find activities by type`() {
        activityEventService.save(testEvent).block()
        activityEventService.save(testEvent.copy(type = ActivityEventType.CREATE_COMMENT)).block()

        StepVerifier.create(activityEventService.findByType(ActivityEventType.ADD_LIKE))
            .expectNextCount(1)
            .verifyComplete()
    }

    @Test
    fun `should find activities by user and image`() {
        activityEventService.save(testEvent).block()
        activityEventService.save(testEvent.copy(userId = 2L)).block()

        StepVerifier.create(activityEventService.findByUserIdAndImageId(1L, 100L))
            .expectNextCount(1)
            .verifyComplete()
    }

    @Test
    fun `should get user activity stats`() {
        activityEventService.save(testEvent).block()
        activityEventService.save(testEvent.copy(type = ActivityEventType.CREATE_COMMENT)).block()

        StepVerifier.create(activityEventService.getActivityStatsByUser(1L))
            .expectNextMatches { stats ->
                stats[ActivityEventType.ADD_LIKE] == 1L &&
                        stats[ActivityEventType.CREATE_COMMENT] == 1L
            }
            .verifyComplete()
    }

    @Test
    fun `should get image activity stats`() {
        activityEventService.save(testEvent).block()
        activityEventService.save(testEvent.copy(type = ActivityEventType.REMOVE_LIKE)).block()

        StepVerifier.create(activityEventService.getActivityStatsByImage(100L))
            .expectNextMatches { stats ->
                stats[ActivityEventType.ADD_LIKE] == 1L &&
                        stats[ActivityEventType.REMOVE_LIKE] == 1L
            }
            .verifyComplete()
    }

    @Test
    fun `should find user recent activities with limit`() {
        repeat(5) {
            activityEventService.save(testEvent).block()
        }

        StepVerifier.create(activityEventService.findUserRecentActivity(1L, 3))
            .expectNextCount(3)
            .verifyComplete()
    }

    @Test
    fun `should find activities by date period`() {
        val now = LocalDateTime.now()
        val yesterday = now.minusDays(1)
        now.plusDays(1)

        activityEventService.save(testEvent.copy(createdAt = yesterday)).block()
        activityEventService.save(testEvent.copy(createdAt = now)).block()

        StepVerifier.create(activityEventService.findByCreatedAtBetween(yesterday, now))
            .expectNextCount(2)
            .verifyComplete()
    }

    @Test
    fun `should delete activities by user id`() {
        activityEventService.save(testEvent).block()
        activityEventService.save(testEvent.copy(userId = 2L)).block()

        StepVerifier.create(activityEventService.deleteByUserId(1L))
            .verifyComplete()

        StepVerifier.create(activityEventService.findByUserId(1L))
            .expectNextCount(0)
            .verifyComplete()

        StepVerifier.create(activityEventService.findByUserId(2L))
            .expectNextCount(1)
            .verifyComplete()
    }

    @Test
    fun `should delete activities by image id`() {
        activityEventService.save(testEvent).block()
        activityEventService.save(testEvent.copy(imageId = 200L)).block()

        StepVerifier.create(activityEventService.deleteByImageId(100L))
            .verifyComplete()

        StepVerifier.create(activityEventService.findByImageId(100L))
            .expectNextCount(0)
            .verifyComplete()

        StepVerifier.create(activityEventService.findByImageId(200L))
            .expectNextCount(1)
            .verifyComplete()
    }
}