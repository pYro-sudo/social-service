package by.losik.activityservice.controller

import by.losik.activityservice.entity.ActivityEvent
import by.losik.activityservice.entity.ActivityEventType
import by.losik.activityservice.service.ActivityEventService
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/activity")
class ActivityEventController(
    private val activityEventService: ActivityEventService
) {

    @GetMapping
    fun getAllActivities(): Flux<ActivityEvent> {
        return activityEventService.findAll()
    }

    @GetMapping("/{id}")
    fun getActivityById(@PathVariable id: String): Mono<ActivityEvent> {
        return activityEventService.findById(id)
    }

    @GetMapping("/user/{userId}")
    fun getActivitiesByUser(@PathVariable userId: Long): Flux<ActivityEvent> {
        return activityEventService.findByUserId(userId)
    }

    @GetMapping("/image/{imageId}")
    fun getActivitiesByImage(@PathVariable imageId: Long): Flux<ActivityEvent> {
        return activityEventService.findByImageId(imageId)
    }

    @GetMapping("/user/{userId}/image/{imageId}")
    fun getActivitiesByUserAndImage(
        @PathVariable userId: Long,
        @PathVariable imageId: Long
    ): Flux<ActivityEvent> {
        return activityEventService.findByUserIdAndImageId(userId, imageId)
    }

    @GetMapping("/type/{type}")
    fun getActivitiesByType(@PathVariable type: ActivityEventType): Flux<ActivityEvent> {
        return activityEventService.findByType(type)
    }

    @GetMapping("/stats/user/{userId}")
    fun getUserStats(@PathVariable userId: Long): Mono<Map<ActivityEventType, Long>> {
        return activityEventService.getActivityStatsByUser(userId)
    }

    @GetMapping("/stats/image/{imageId}")
    fun getImageStats(@PathVariable imageId: Long): Mono<Map<ActivityEventType, Long>> {
        return activityEventService.getActivityStatsByImage(imageId)
    }

    @GetMapping("/user/{userId}/recent")
    fun getUserRecentActivities(
        @PathVariable userId: Long,
        @RequestParam(defaultValue = "10") limit: Int
    ): Flux<ActivityEvent> {
        return activityEventService.findUserRecentActivity(userId, limit)
    }

    @GetMapping("/period")
    fun getActivitiesByPeriod(
        @RequestParam startDate: String,
        @RequestParam endDate: String
    ): Flux<ActivityEvent> {
        val start = LocalDateTime.parse(startDate)
        val end = LocalDateTime.parse(endDate)
        return activityEventService.findByCreatedAtBetween(start, end)
    }
}