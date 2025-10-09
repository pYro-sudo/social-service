package by.losik.activityservice.shell

import by.losik.activityservice.entity.ActivityEvent
import by.losik.activityservice.entity.ActivityEventType
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
            .map { events -> formatActivityEventsTable(events) }
            .block() ?: "No activities found"
    }

    @ShellMethod(key = ["activity get", "act get"], value = "Get activity by ID")
    fun getActivityById(@ShellOption( help = "Activity ID") id: String): String {
        return activityEventService.findById(id)
            .map { event -> formatSingleActivity(event) }
            .onErrorReturn("Activity not found with ID: $id")
            .block() ?: "Activity not found"
    }

    @ShellMethod(key = ["activity user", "act user"], value = "Get activities by user ID")
    fun getActivitiesByUser(@ShellOption( help = "User ID") userId: Long): String {
        return activityEventService.findByUserId(userId)
            .collectList()
            .map { events ->
                if (events.isEmpty()) "No activities found for user ID: $userId"
                else formatActivityEventsTable(events)
            }
            .block() ?: "No activities found"
    }

    @ShellMethod(key = ["activity image", "act img"], value = "Get activities by image ID")
    fun getActivitiesByImage(@ShellOption( help = "Image ID") imageId: Long): String {
        return activityEventService.findByImageId(imageId)
            .collectList()
            .map { events ->
                if (events.isEmpty()) "No activities found for image ID: $imageId"
                else formatActivityEventsTable(events)
            }
            .block() ?: "No activities found"
    }

    @ShellMethod(key = ["activity type", "act type"], value = "Get activities by type")
    fun getActivitiesByType(@ShellOption(help = "Activity type") type: ActivityEventType): String {
        return activityEventService.findByType(type)
            .collectList()
            .map { events ->
                if (events.isEmpty()) "No activities found for type: $type"
                else formatActivityEventsTable(events)
            }
            .block() ?: "No activities found"
    }

    @ShellMethod(key = ["activity stats user", "act stats user"], value = "Get activity statistics for user")
    fun getUserStats(@ShellOption(help = "User ID") userId: Long): String {
        return activityEventService.getActivityStatsByUser(userId)
            .map { stats -> formatStatsTable(userId, stats, "User") }
            .block() ?: "No statistics available"
    }

    @ShellMethod(key = ["activity stats image", "act stats img"], value = "Get activity statistics for image")
    fun getImageStats(@ShellOption( help = "Image ID") imageId: Long): String {
        return activityEventService.getActivityStatsByImage(imageId)
            .map { stats -> formatStatsTable(imageId, stats, "Image") }
            .block() ?: "No statistics available"
    }

    @ShellMethod(key = ["activity recent", "act recent"], value = "Get recent activities for user")
    fun getUserRecentActivities(
        @ShellOption( help = "User ID") userId: Long,
    @ShellOption(defaultValue = "10", help = "Limit") limit: Int
    ): String {
        return activityEventService.findUserRecentActivity(userId, limit)
            .collectList()
            .map { events ->
                if (events.isEmpty()) "No recent activities found for user ID: $userId"
                else formatActivityEventsTable(events)
            }
            .block() ?: "No activities found"
    }

    @ShellMethod(key = ["activity period", "act period"], value = "Get activities by time period")
    fun getActivitiesByPeriod(
        @ShellOption( help = "Start date (yyyy-MM-dd)") startDate: String,
    @ShellOption( help = "End date (yyyy-MM-dd)") endDate: String
    ): String {
        val start = LocalDateTime.parse("${startDate}T00:00:00")
        val end = LocalDateTime.parse("${endDate}T23:59:59")

        return activityEventService.findByCreatedAtBetween(start, end)
            .collectList()
            .map { events ->
                if (events.isEmpty()) "No activities found for period $startDate to $endDate"
                else formatActivityEventsTable(events)
            }
            .block() ?: "No activities found"
    }

    @ShellMethod(key = ["activity delete user", "act del user"], value = "Delete all activities for user")
    fun deleteUserActivities(@ShellOption( help = "User ID") userId: Long): String {
        return activityEventService.deleteByUserId(userId)
            .then(Mono.just("All activities for user $userId have been deleted"))
            .onErrorReturn("Error deleting activities for user $userId")
            .block() ?: "Operation completed"
    }

    @ShellMethod(key = ["activity delete image", "act del img"], value = "Delete all activities for image")
    fun deleteImageActivities(@ShellOption( help = "Image ID") imageId: Long): String {
        return activityEventService.deleteByImageId(imageId)
            .then(Mono.just("All activities for image $imageId have been deleted"))
            .onErrorReturn("Error deleting activities for image $imageId")
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
            .map { count -> "Total activities: $count" }
            .block() ?: "Unable to count activities"
    }

    private fun formatActivityEventsTable(events: List<ActivityEvent>): String {
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
            Activity Details:
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
        val headers = arrayOf("$type ID", "Activity Type", "Count")

        val data = stats.entries.map { (activityType, count) ->
            arrayOf(id.toString(), activityType.name, count.toString())
        }.toTypedArray()

        if (data.isEmpty()) {
            return "No statistics available for $type ID: $id"
        }

        return buildTable(headers, data)
    }

    private fun buildTable(headers: Array<String>, data: Array<Array<String>>): String {
        val model = ArrayTableModel(arrayOf(headers) + data)
        val tableBuilder = TableBuilder(model)

        tableBuilder.addHeaderBorder(BorderStyle.fancy_light)
        tableBuilder.addInnerBorder(BorderStyle.fancy_light)

        return tableBuilder.build().render(80)
    }
}