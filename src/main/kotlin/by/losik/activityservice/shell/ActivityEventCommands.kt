package by.losik.activityservice.shell

import by.losik.activityservice.entity.ActivityEvent
import by.losik.activityservice.entity.ActivityEventType
import by.losik.activityservice.exception.NotFoundException
import by.losik.activityservice.service.ActivityEventService
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption
import org.springframework.shell.table.*
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@ShellComponent
class ActivityEventCommands(
    private val activityEventService: ActivityEventService
) {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @ShellMethod(key = ["activity list", "act ls"], value = "List all activity events")
    fun listAllActivities(): String {
        return activityEventService.findAll()
            .collectList()
            .map { events ->
                if (events.isEmpty()) "No activities found"
                else formatActivityEventsTable(events)
            }
            .onErrorResume { error ->
                Mono.just("Error retrieving activities: ${error.message}")
            }
            .block() ?: "Error retrieving activities"
    }

    @ShellMethod(key = ["activity get", "act get"], value = "Get activity by ID")
    fun getActivityById(@ShellOption(help = "Activity ID") id: String): String {
        return activityEventService.findById(id)
            .map { event -> formatSingleActivity(event) }
            .onErrorResume { error ->
                when (error) {
                    is NotFoundException -> Mono.just("Activity not found with ID: $id")
                    else -> Mono.just("Error retrieving activity: ${error.message}")
                }
            }
            .block() ?: "Error retrieving activity"
    }

    @ShellMethod(key = ["activity user", "act user"], value = "Get activities by user ID")
    fun getActivitiesByUser(@ShellOption(help = "User ID") userId: Long): String {
        return activityEventService.findByUserId(userId)
            .collectList()
            .map { events ->
                if (events.isEmpty()) "No activities found for user ID: $userId"
                else formatActivityEventsTable(events)
            }
            .onErrorResume { error ->
                Mono.just("Error retrieving user activities: ${error.message}")
            }
            .block() ?: "Error retrieving user activities"
    }

    @ShellMethod(key = ["activity image", "act img"], value = "Get activities by image ID")
    fun getActivitiesByImage(@ShellOption(help = "Image ID") imageId: Long): String {
        return activityEventService.findByImageId(imageId)
            .collectList()
            .map { events ->
                if (events.isEmpty()) "No activities found for image ID: $imageId"
                else formatActivityEventsTable(events)
            }
            .onErrorResume { error ->
                Mono.just("Error retrieving image activities: ${error.message}")
            }
            .block() ?: "Error retrieving image activities"
    }

    @ShellMethod(key = ["activity type", "act type"], value = "Get activities by type")
    fun getActivitiesByType(@ShellOption(help = "Activity type") type: ActivityEventType): String {
        return activityEventService.findByType(type)
            .collectList()
            .map { events ->
                if (events.isEmpty()) "No activities found for type: $type"
                else formatActivityEventsTable(events)
            }
            .onErrorResume { error ->
                Mono.just("Error retrieving activities by type: ${error.message}")
            }
            .block() ?: "Error retrieving activities by type"
    }

    @ShellMethod(key = ["activity stats user", "act stats user"], value = "Get activity statistics for user")
    fun getUserStats(@ShellOption(help = "User ID") userId: Long): String {
        return activityEventService.getActivityStatsByUser(userId)
            .map { stats ->
                if (stats.isEmpty()) "No statistics available for user ID: $userId"
                else formatStatsTable(userId, stats, "User")
            }
            .onErrorResume { error ->
                Mono.just("Error retrieving user statistics: ${error.message}")
            }
            .block() ?: "Error retrieving user statistics"
    }

    @ShellMethod(key = ["activity stats image", "act stats img"], value = "Get activity statistics for image")
    fun getImageStats(@ShellOption(help = "Image ID") imageId: Long): String {
        return activityEventService.getActivityStatsByImage(imageId)
            .map { stats ->
                if (stats.isEmpty()) "No statistics available for image ID: $imageId"
                else formatStatsTable(imageId, stats, "Image")
            }
            .onErrorResume { error ->
                Mono.just("Error retrieving image statistics: ${error.message}")
            }
            .block() ?: "Error retrieving image statistics"
    }

    @ShellMethod(key = ["activity recent", "act recent"], value = "Get recent activities for user")
    fun getUserRecentActivities(
        @ShellOption(help = "User ID") userId: Long,
        @ShellOption(defaultValue = "10", help = "Limit") limit: Int
    ): String {
        return activityEventService.findUserRecentActivity(userId, limit)
            .collectList()
            .map { events ->
                if (events.isEmpty()) "No recent activities found for user ID: $userId"
                else formatActivityEventsTable(events)
            }
            .onErrorResume { error ->
                Mono.just("Error retrieving recent activities: ${error.message}")
            }
            .block() ?: "Error retrieving recent activities"
    }

    @ShellMethod(key = ["activity period", "act period"], value = "Get activities by time period")
    fun getActivitiesByPeriod(
        @ShellOption(help = "Start date (yyyy-MM-dd)") startDate: String,
        @ShellOption(help = "End date (yyyy-MM-dd)") endDate: String
    ): String {
        return try {
            val start = LocalDateTime.parse("${startDate}T00:00:00")
            val end = LocalDateTime.parse("${endDate}T23:59:59")

            activityEventService.findByCreatedAtBetween(start, end)
                .collectList()
                .map { events ->
                    if (events.isEmpty()) "No activities found for period $startDate to $endDate"
                    else formatActivityEventsTable(events)
                }
                .onErrorResume { error ->
                    Mono.just("Error retrieving activities for period: ${error.message}")
                }
                .block() ?: "Error retrieving activities for period"
        } catch (e: Exception) {
            "Invalid date format. Please use yyyy-MM-dd format"
        }
    }

    @ShellMethod(key = ["activity delete user", "act del user"], value = "Delete all activities for user")
    fun deleteUserActivities(@ShellOption(help = "User ID") userId: Long): String {
        return activityEventService.deleteByUserId(userId)
            .then(Mono.just("✅ All activities for user $userId have been deleted"))
            .onErrorResume { error ->
                when (error) {
                    is NotFoundException -> Mono.just("No activities found for user ID: $userId")
                    else -> Mono.just("❌ Error deleting activities for user $userId: ${error.message}")
                }
            }
            .block() ?: "Operation completed"
    }

    @ShellMethod(key = ["activity delete image", "act del img"], value = "Delete all activities for image")
    fun deleteImageActivities(@ShellOption(help = "Image ID") imageId: Long): String {
        return activityEventService.deleteByImageId(imageId)
            .then(Mono.just("✅ All activities for image $imageId have been deleted"))
            .onErrorResume { error ->
                when (error) {
                    is NotFoundException -> Mono.just("No activities found for image ID: $imageId")
                    else -> Mono.just("❌ Error deleting activities for image $imageId: ${error.message}")
                }
            }
            .block() ?: "Operation completed"
    }

    @ShellMethod(key = ["activity types", "act types"], value = "List all activity types")
    fun listActivityTypes(): String {
        return ActivityEventType.values().joinToString("\n") { type ->
            "• $type"
        }
    }

    @ShellMethod(key = ["activity count", "act count"], value = "Get total count of activities")
    fun getTotalCount(): String {
        return activityEventService.findAll()
            .count()
            .map { count -> "📊 Total activities: $count" }
            .onErrorResume { error ->
                Mono.just("Error counting activities: ${error.message}")
            }
            .block() ?: "Error counting activities"
    }

    @ShellMethod(key = ["activity search", "act search"], value = "Search activities by user and image")
    fun searchActivities(
        @ShellOption(help = "User ID", defaultValue = ShellOption.NULL) userId: Long?,
        @ShellOption(help = "Image ID", defaultValue = ShellOption.NULL) imageId: Long?
    ): String {
        return when {
            userId != null && imageId != null -> {
                activityEventService.findByUserIdAndImageId(userId, imageId)
                    .collectList()
                    .map { events ->
                        if (events.isEmpty()) "No activities found for user ID: $userId and image ID: $imageId"
                        else formatActivityEventsTable(events)
                    }
                    .onErrorResume { error ->
                        Mono.just("Error searching activities: ${error.message}")
                    }
                    .block() ?: "Error searching activities"
            }
            userId != null -> getActivitiesByUser(userId)
            imageId != null -> getActivitiesByImage(imageId)
            else -> "Please specify at least one search parameter (user ID or image ID)"
        }
    }

    private fun formatActivityEventsTable(events: List<ActivityEvent>): String {
        if (events.isEmpty()) {
            return "No activities found"
        }

        val headers = arrayOf("ID", "User ID", "Image ID", "Type", "Status", "Created At", "Content")

        val data = events.map { event ->
            arrayOf(
                event.id?.take(8) + "...",
                event.userId.toString(),
                event.imageId.toString(),
                event.type.name,
                event.status.name,
                event.createdAt.format(formatter),
                event.content?.take(20) ?: "N/A"
            )
        }.toTypedArray()

        return buildTable(headers, data)
    }

    private fun formatSingleActivity(event: ActivityEvent): String {
        return """
            📋 Activity Details:
            • ID: ${event.id}
            • User ID: ${event.userId}
            • Image ID: ${event.imageId}
            • Type: ${event.type}
            • Status: ${event.status}
            • Created At: ${event.createdAt.format(formatter)}
            • Content: ${event.content ?: "N/A"}
        """.trimIndent()
    }

    private fun formatStatsTable(id: Long, stats: Map<ActivityEventType, Long>, type: String): String {
        if (stats.isEmpty()) {
            return "No statistics available for $type ID: $id"
        }

        val headers = arrayOf("$type ID", "Activity Type", "Count")
        val total = stats.values.sum()

        val data = stats.entries.map { (activityType, count) ->
            arrayOf(id.toString(), activityType.name, count.toString())
        }.toTypedArray()

        val table = buildTable(headers, data)
        return "$table\n\n📈 Total activities: $total"
    }

    private fun buildTable(headers: Array<String>, data: Array<Array<String>>): String {
        val model = ArrayTableModel(arrayOf(headers) + data)
        val tableBuilder = TableBuilder(model)

        tableBuilder.addHeaderBorder(BorderStyle.fancy_light)
        tableBuilder.addInnerBorder(BorderStyle.fancy_light)

        return tableBuilder.build().render(80)
    }
}