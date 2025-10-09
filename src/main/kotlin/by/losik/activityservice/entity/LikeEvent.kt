package by.losik.activityservice.entity

import java.time.LocalDateTime

data class LikeEvent(
    val id: Long? = null,
    val userId: Long,
    val imageId: Long,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val eventType: ActivityEventType
)