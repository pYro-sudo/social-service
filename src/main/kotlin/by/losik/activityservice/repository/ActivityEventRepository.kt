package by.losik.activityservice.repository

import by.losik.activityservice.entity.ActivityEvent
import by.losik.activityservice.entity.ActivityEventType
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.LocalDateTime

@Repository
interface ActivityEventRepository : ReactiveMongoRepository<ActivityEvent, String> {

    fun findByUserId(userId: Long): Flux<ActivityEvent>

    fun findByImageId(imageId: Long): Flux<ActivityEvent>

    fun findByUserIdAndImageId(userId: Long, imageId: Long): Flux<ActivityEvent>

    fun findByType(type: ActivityEventType): Flux<ActivityEvent>

    fun findByCreatedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): Flux<ActivityEvent>

}