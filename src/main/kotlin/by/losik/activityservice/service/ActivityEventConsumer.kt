package by.losik.activityservice.service

import by.losik.activityservice.entity.CommentEvent
import by.losik.activityservice.entity.LikeEvent
import by.losik.activityservice.mapping.toActivityEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class ActivityEventConsumer(
    private val activityEventService: ActivityEventService,
) {

    companion object {
        private val log = LoggerFactory.getLogger(ActivityEventConsumer::class.java)
    }

    @KafkaListener(
        topics = ["\${kafka.topics.activity-events:activity-events}"],
        groupId = "\${spring.kafka.consumer.group-id:activity-service-group}"
    )
    fun consumeLikeEvent(event: LikeEvent) {
        log.info("RECEIVED LikeEvent: userId={}, imageId={}, type={}", event.userId, event.imageId, event.eventType)
        try {
            handleLikeEvent(event)
            log.info("SUCCESSFULLY PROCESSED LikeEvent: userId={}", event.userId)
        } catch (e: Exception) {
            log.error("FAILED to process LikeEvent: {}", event, e)
            throw e
        }
    }

    @KafkaListener(
        topics = ["\${kafka.topics.activity-events:activity-events}"],
        groupId = "\${spring.kafka.consumer.group-id:activity-service-group}"
    )
    fun consumeCommentEvent(event: CommentEvent) {
        log.info("RECEIVED CommentEvent: userId={}, imageId={}, content={}", event.userId, event.imageId, event.content)
        try {
            handleCommentEvent(event)
            log.info("SUCCESSFULLY PROCESSED CommentEvent: userId={}", event.userId)
        } catch (e: Exception) {
            log.error("FAILED to process CommentEvent: {}", event, e)
            throw e
        }
    }

    private fun handleLikeEvent(likeEvent: LikeEvent) {
        val activityEvent = likeEvent.toActivityEvent()

        activityEventService.save(activityEvent)
            .doOnSuccess { savedEvent ->
                log.info("SUCCESSFULLY saved like activity event: {}", savedEvent)
            }
            .doOnError { error ->
                log.error("FAILED to save like activity event: {}", likeEvent, error)
            }
    }

    private fun handleCommentEvent(commentEvent: CommentEvent) {
        val activityEvent = commentEvent.toActivityEvent()

        activityEventService.save(activityEvent)
            .doOnSuccess { savedEvent ->
                log.info("SUCCESSFULLY saved comment activity event: {}", savedEvent)
            }
            .doOnError { error ->
                log.error("FAILED to save comment activity event: {}", commentEvent, error)
            }
    }
}