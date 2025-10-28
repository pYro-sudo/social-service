package by.losik.activityservice.entity

import com.fasterxml.jackson.annotation.JsonTypeName
import java.time.LocalDateTime

@JsonTypeName("likeEvent")
data class LikeEvent(
    val id: Long? = null,
    val userId: Long,
    val imageId: Long,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val eventType: ActivityEventType
)