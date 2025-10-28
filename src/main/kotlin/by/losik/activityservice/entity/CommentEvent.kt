package by.losik.activityservice.entity

import com.fasterxml.jackson.annotation.JsonTypeName
import java.time.LocalDateTime

@JsonTypeName("commentEvent")
data class CommentEvent(
    val id: Long? = null,
    val userId: Long,
    val imageId: Long,
    val content: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val eventType: ActivityEventType
)