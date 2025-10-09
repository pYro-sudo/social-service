package by.losik.activityservice.service

import by.losik.activityservice.entity.ActivityEvent
import by.losik.activityservice.entity.ActivityEventType
import by.losik.activityservice.repository.ActivityEventRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Service
class ActivityEventService(
    private val activityEventRepository: ActivityEventRepository
) {

    fun save(activityEvent: ActivityEvent): Mono<ActivityEvent> {
        return activityEventRepository.save(activityEvent)
    }

    fun findAll(): Flux<ActivityEvent> {
        return activityEventRepository.findAll()
    }

    fun findById(id: String): Mono<ActivityEvent> {
        return activityEventRepository.findById(id)
    }

    fun findByUserId(userId: Long): Flux<ActivityEvent> {
        return activityEventRepository.findByUserId(userId)
    }

    fun findByImageId(imageId: Long): Flux<ActivityEvent> {
        return activityEventRepository.findByImageId(imageId)
    }

    fun findByUserIdAndImageId(userId: Long, imageId: Long): Flux<ActivityEvent> {
        return activityEventRepository.findByUserIdAndImageId(userId, imageId)
    }

    fun findByType(type: ActivityEventType): Flux<ActivityEvent> {
        return activityEventRepository.findByType(type)
    }

    fun findByCreatedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): Flux<ActivityEvent> {
        return activityEventRepository.findByCreatedAtBetween(startDate, endDate)
    }

    fun findUserRecentActivity(userId: Long, limit: Int): Flux<ActivityEvent> {
        return activityEventRepository.findByUserId(userId)
            .take(limit.toLong())
    }

    fun getActivityStatsByUser(userId: Long): Mono<Map<ActivityEventType, Long>> {
        return activityEventRepository.findByUserId(userId)
            .collectList()
            .map { events ->
                events.groupBy { it.type }
                    .mapValues { it.value.size.toLong() }
            }
    }

    fun getActivityStatsByImage(imageId: Long): Mono<Map<ActivityEventType, Long>> {
        return activityEventRepository.findByImageId(imageId)
            .collectList()
            .map { events ->
                events.groupBy { it.type }
                    .mapValues { it.value.size.toLong() }
            }
    }

    fun deleteByUserId(userId: Long): Mono<Void> {
        return activityEventRepository.deleteAll(
            activityEventRepository.findByUserId(userId)
        )
    }

    fun deleteByImageId(imageId: Long): Mono<Void> {
        return activityEventRepository.deleteAll(
            activityEventRepository.findByImageId(imageId)
        )
    }
}