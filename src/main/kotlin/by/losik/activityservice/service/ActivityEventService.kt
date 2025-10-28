package by.losik.activityservice.service

import by.losik.activityservice.handler.ReactiveExceptionHandler
import by.losik.activityservice.entity.ActivityEvent
import by.losik.activityservice.entity.ActivityEventType
import by.losik.activityservice.exception.NotFoundException
import by.losik.activityservice.repository.ActivityEventRepository
import by.losik.activityservice.utils.ExceptionUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Service
class ActivityEventService(
    private val activityEventRepository: ActivityEventRepository,
    private val reactiveExceptionHandler: ReactiveExceptionHandler
) {

    companion object {
        private val log = LoggerFactory.getLogger(ActivityEventService::class.java)
    }

    fun save(activityEvent: ActivityEvent): Mono<ActivityEvent> {
        log.debug("Saving activity event: userId={}, imageId={}, type={}",
            activityEvent.userId, activityEvent.imageId, activityEvent.type)

        return reactiveExceptionHandler.wrapMono(
            activityEventRepository.save(activityEvent)
                .doOnSuccess { saved ->
                    log.debug("Successfully saved activity event with id: {}", saved.id)
                }
        )
    }

    fun findAll(): Flux<ActivityEvent> {
        log.debug("Finding all activity events")
        return reactiveExceptionHandler.wrapFlux(
            activityEventRepository.findAll()
                .doOnNext { log.trace("Found activity event: {}", it.id) }
        )
    }

    fun findById(id: String): Mono<ActivityEvent> {
        ExceptionUtils.requireValidId(id)
        log.debug("Finding activity event by id: {}", id)

        return reactiveExceptionHandler.wrapMono(
            activityEventRepository.findById(id)
                .handleNotFound("Activity event not found with id: $id")
                .doOnNext { log.debug("Found activity event: {}", it.id) }
        )
    }

    fun findByUserId(userId: Long): Flux<ActivityEvent> {
        ExceptionUtils.requireValidUserId(userId)
        log.debug("Finding activity events by user id: {}", userId)

        return reactiveExceptionHandler.wrapFlux(
            activityEventRepository.findByUserId(userId)
                .doOnNext { log.trace("Found activity event for user {}: {}", userId, it.id) }
        )
    }

    fun findByImageId(imageId: Long): Flux<ActivityEvent> {
        ExceptionUtils.requireValidImageId(imageId)
        log.debug("Finding activity events by image id: {}", imageId)

        return reactiveExceptionHandler.wrapFlux(
            activityEventRepository.findByImageId(imageId)
                .doOnNext { log.trace("Found activity event for image {}: {}", imageId, it.id) }
        )
    }

    fun findByType(type: ActivityEventType): Flux<ActivityEvent> {
        log.debug("Finding activity events by type: {}", type)
        return reactiveExceptionHandler.wrapFlux(
            activityEventRepository.findByType(type)
                .doOnNext { log.trace("Found activity event of type {}: {}", type, it.id) }
        )
    }

    fun findByCreatedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): Flux<ActivityEvent> {
        log.debug("Finding activity events between {} and {}", startDate, endDate)
        return reactiveExceptionHandler.wrapFlux(
            activityEventRepository.findByCreatedAtBetween(startDate, endDate)
                .doOnNext { log.trace("Found activity event in period: {}", it.id) }
        )
    }

    fun findUserRecentActivity(userId: Long, limit: Int): Flux<ActivityEvent> {
        ExceptionUtils.requireValidUserId(userId)
        log.debug("Finding recent {} activities for user id: {}", limit, userId)

        return reactiveExceptionHandler.wrapFlux(
            activityEventRepository.findByUserId(userId)
                .take(limit.toLong())
                .doOnNext { log.trace("Found recent activity for user {}: {}", userId, it.id) }
        )
    }

    fun getActivityStatsByUser(userId: Long): Mono<Map<ActivityEventType, Long>> {
        ExceptionUtils.requireValidUserId(userId)
        log.debug("Getting activity stats for user id: {}", userId)

        return reactiveExceptionHandler.wrapMono(
            activityEventRepository.findByUserId(userId)
                .collectList()
                .map { events ->
                    events.groupBy { it.type }
                        .mapValues { it.value.size.toLong() }
                }
                .doOnSuccess { stats -> log.debug("Stats for user {}: {}", userId, stats) }
        )
    }

    fun findByUserIdAndImageId(userId: Long, imageId: Long): Flux<ActivityEvent> {
        ExceptionUtils.requireValidUserId(userId)
        ExceptionUtils.requireValidImageId(imageId)
        log.debug("Finding activity events by user id: {} and image id: {}", userId, imageId)

        return reactiveExceptionHandler.wrapFlux(
            activityEventRepository.findByUserIdAndImageId(userId, imageId)
                .doOnNext { log.trace("Found activity event for user {} and image {}: {}", userId, imageId, it.id) }
        )
    }

    fun getActivityStatsByImage(imageId: Long): Mono<Map<ActivityEventType, Long>> {
        ExceptionUtils.requireValidImageId(imageId)
        log.debug("Getting activity stats for image id: {}", imageId)

        return reactiveExceptionHandler.wrapMono(
            activityEventRepository.findByImageId(imageId)
                .collectList()
                .map { events ->
                    events.groupBy { it.type }
                        .mapValues { it.value.size.toLong() }
                }
                .doOnSuccess { stats -> log.debug("Stats for image {}: {}", imageId, stats) }
        )
    }

    fun deleteByUserId(userId: Long): Mono<Void> {
        ExceptionUtils.requireValidUserId(userId)
        log.debug("Deleting all activities for user id: {}", userId)

        return reactiveExceptionHandler.wrapMono(
            activityEventRepository.findByUserId(userId)
                .collectList()
                .flatMap { events ->
                    if (events.isEmpty()) {
                        log.debug("No activities found to delete for user id: {}", userId)
                        Mono.empty()
                    } else {
                        activityEventRepository.deleteAll(events)
                            .doOnSuccess { log.debug("Deleted {} activities for user id: {}", events.size, userId) }
                    }
                }
                .then()
        )
    }

    fun deleteByImageId(imageId: Long): Mono<Void> {
        ExceptionUtils.requireValidImageId(imageId)
        log.debug("Deleting all activities for image id: {}", imageId)

        return reactiveExceptionHandler.wrapMono(
            activityEventRepository.findByImageId(imageId)
                .collectList()
                .flatMap { events ->
                    if (events.isEmpty()) {
                        log.debug("No activities found to delete for image id: {}", imageId)
                        Mono.empty()
                    } else {
                        activityEventRepository.deleteAll(events)
                            .doOnSuccess { log.debug("Deleted {} activities for image id: {}", events.size, imageId) }
                    }
                }
                .then()
        )
    }

    private fun <T> Mono<T>.handleNotFound(message: String): Mono<T> {
        return this.switchIfEmpty(Mono.error(NotFoundException(message)))
    }
}