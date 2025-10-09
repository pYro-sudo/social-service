package by.losik.activityservice.service

import by.losik.activityservice.entity.*
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ActivityEventConsumer(
    private val activityEventService: ActivityEventService
) {

    companion object {
        private val log = LoggerFactory.getLogger(ActivityEventConsumer::class.java)
    }

    @KafkaListener(topics = ["activity-events"], groupId = "activity-service-group")
    fun consumeActivityEvent(event: Any) {
        log.debug("Received activity event: {}", event)

        when (event) {
            is LikeEvent -> handleLikeEvent(event)
            is CommentEvent -> handleCommentEvent(event)
            else -> log.warn("Unknown event type: {}", event::class.java.simpleName)
        }
    }

    private fun handleLikeEvent(likeEvent: LikeEvent) {
        val activityEvent = ActivityEvent(
            userId = likeEvent.userId,
            imageId = likeEvent.imageId,
            type = likeEvent.eventType,
            createdAt = LocalDateTime.now()
        )

        activityEventService.save(activityEvent)
            .doOnSuccess { savedEvent ->
                log.debug("Successfully saved like activity event: {}", savedEvent)
            }
            .doOnError { error ->
                log.error("Failed to save like activity event: {}", likeEvent, error)
            }
            .subscribe()
    }

    private fun handleCommentEvent(commentEvent: CommentEvent) {
        val activityEvent = ActivityEvent(
            userId = commentEvent.userId,
            imageId = commentEvent.imageId,
            type = commentEvent.eventType,
            createdAt = LocalDateTime.now(),
            content = commentEvent.content
        )

        activityEventService.save(activityEvent)
            .doOnSuccess { savedEvent ->
                log.debug("Successfully saved comment activity event: {}", savedEvent)
            }
            .doOnError { error ->
                log.error("Failed to save comment activity event: {}", commentEvent, error)
            }
            .subscribe()
    }
}