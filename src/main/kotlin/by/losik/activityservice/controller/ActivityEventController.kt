package by.losik.activityservice.controller

import by.losik.activityservice.entity.ActivityEvent
import by.losik.activityservice.entity.ActivityEventType
import by.losik.activityservice.service.ActivityEventService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/activity")
@Tag(name = "Activity Events", description = "API для управления событиями активности пользователей")
class ActivityEventController(
    private val activityEventService: ActivityEventService
) {

    @Operation(
        summary = "Получить все события активности",
        description = "Возвращает все события активности из системы"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Успешное получение списка событий",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ActivityEvent::class))]
    )
    @GetMapping
    fun getAllActivities(): Flux<ActivityEvent> {
        return activityEventService.findAll()
    }

    @Operation(
        summary = "Получить событие по ID",
        description = "Возвращает конкретное событие активности по его идентификатору"
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Событие найдено",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = ActivityEvent::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Событие не найдено"
        )
    )
    @GetMapping("/{id}")
    fun getActivityById(
        @Parameter(description = "Идентификатор события активности", required = true, example = "12345")
        @PathVariable id: String
    ): Mono<ActivityEvent> {
        return activityEventService.findById(id)
    }

    @Operation(
        summary = "Получить события по пользователю",
        description = "Возвращает все события активности для указанного пользователя"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Успешное получение событий пользователя",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ActivityEvent::class))]
    )
    @GetMapping("/user/{userId}")
    fun getActivitiesByUser(
        @Parameter(description = "Идентификатор пользователя", required = true, example = "123")
        @PathVariable userId: Long
    ): Flux<ActivityEvent> {
        return activityEventService.findByUserId(userId)
    }

    @Operation(
        summary = "Получить события по изображению",
        description = "Возвращает все события активности для указанного изображения"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Успешное получение событий изображения",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ActivityEvent::class))]
    )
    @GetMapping("/image/{imageId}")
    fun getActivitiesByImage(
        @Parameter(description = "Идентификатор изображения", required = true, example = "456")
        @PathVariable imageId: Long
    ): Flux<ActivityEvent> {
        return activityEventService.findByImageId(imageId)
    }

    @Operation(
        summary = "Получить события по пользователю и изображению",
        description = "Возвращает события активности для конкретной комбинации пользователя и изображения"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Успешное получение событий",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ActivityEvent::class))]
    )
    @GetMapping("/user/{userId}/image/{imageId}")
    fun getActivitiesByUserAndImage(
        @Parameter(description = "Идентификатор пользователя", required = true, example = "123")
        @PathVariable userId: Long,
        @Parameter(description = "Идентификатор изображения", required = true, example = "456")
        @PathVariable imageId: Long
    ): Flux<ActivityEvent> {
        return activityEventService.findByUserIdAndImageId(userId, imageId)
    }

    @Operation(
        summary = "Получить события по типу",
        description = "Возвращает все события активности указанного типа"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Успешное получение событий по типу",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ActivityEvent::class))]
    )
    @GetMapping("/type/{type}")
    fun getActivitiesByType(
        @Parameter(description = "Тип события активности", required = true, schema = Schema(implementation = ActivityEventType::class))
        @PathVariable type: ActivityEventType
    ): Flux<ActivityEvent> {
        return activityEventService.findByType(type)
    }

    @Operation(
        summary = "Получить статистику пользователя",
        description = "Возвращает статистику активности пользователя по типам событий"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Успешное получение статистики",
        content = [Content(mediaType = "application/json")]
    )
    @GetMapping("/stats/user/{userId}")
    fun getUserStats(
        @Parameter(description = "Идентификатор пользователя", required = true, example = "123")
        @PathVariable userId: Long
    ): Mono<Map<ActivityEventType, Long>> {
        return activityEventService.getActivityStatsByUser(userId)
    }

    @Operation(
        summary = "Получить статистику изображения",
        description = "Возвращает статистику активности для изображения по типам событий"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Успешное получение статистики",
        content = [Content(mediaType = "application/json")]
    )
    @GetMapping("/stats/image/{imageId}")
    fun getImageStats(
        @Parameter(description = "Идентификатор изображения", required = true, example = "456")
        @PathVariable imageId: Long
    ): Mono<Map<ActivityEventType, Long>> {
        return activityEventService.getActivityStatsByImage(imageId)
    }

    @Operation(
        summary = "Получить последние активности пользователя",
        description = "Возвращает указанное количество последних событий активности пользователя"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Успешное получение последних активностей",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ActivityEvent::class))]
    )
    @GetMapping("/user/{userId}/recent")
    fun getUserRecentActivities(
        @Parameter(description = "Идентификатор пользователя", required = true, example = "123")
        @PathVariable userId: Long,
        @Parameter(
            description = "Количество возвращаемых событий",
            required = false,
            example = "10",
            `in` = ParameterIn.QUERY
        )
        @RequestParam(defaultValue = "10") limit: Int
    ): Flux<ActivityEvent> {
        return activityEventService.findUserRecentActivity(userId, limit)
    }

    @Operation(
        summary = "Получить события за период",
        description = "Возвращает события активности за указанный временной период"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Успешное получение событий за период",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ActivityEvent::class))]
    )
    @GetMapping("/period")
    fun getActivitiesByPeriod(
        @Parameter(
            description = "Начальная дата периода (формат: yyyy-MM-dd'T'HH:mm:ss)",
            required = true,
            example = "2024-01-01T00:00:00"
        )
        @RequestParam startDate: String,
        @Parameter(
            description = "Конечная дата периода (формат: yyyy-MM-dd'T'HH:mm:ss)",
            required = true,
            example = "2024-12-31T23:59:59"
        )
        @RequestParam endDate: String
    ): Flux<ActivityEvent> {
        val start = LocalDateTime.parse(startDate)
        val end = LocalDateTime.parse(endDate)
        return activityEventService.findByCreatedAtBetween(start, end)
    }
}