package by.losik.activityservice.entity

import java.time.LocalDateTime

data class CommentEvent(
    val id: Long? = null,
    val userId: Long,
    val imageId: Long,
    val content: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val eventType: ActivityEventType
)