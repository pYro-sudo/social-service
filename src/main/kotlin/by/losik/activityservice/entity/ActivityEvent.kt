package by.losik.activityservice.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document(collection = "activity")
data class ActivityEvent(
    @Id
    val id: String? = null,
    val userId: Long,
    val imageId: Long,
    val type: ActivityEventType,
    val status: ActivityStatus = ActivityStatus.PROCESSED,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val content: String? = null
)