package by.losik.activityservice.mapping

import by.losik.activityservice.entity.ActivityEvent
import by.losik.activityservice.entity.ActivityStatus
import by.losik.activityservice.entity.CommentEvent
import by.losik.activityservice.entity.LikeEvent
import java.time.LocalDateTime

fun LikeEvent.toActivityEvent(): ActivityEvent {
    return ActivityEvent(
        userId = this.userId,
        imageId = this.imageId,
        type = this.eventType,
        status = ActivityStatus.PROCESSED,
        createdAt = LocalDateTime.now(),
        content = null
    )
}

fun CommentEvent.toActivityEvent(): ActivityEvent {
    return ActivityEvent(
        userId = this.userId,
        imageId = this.imageId,
        type = this.eventType,
        status = ActivityStatus.PROCESSED,
        createdAt = LocalDateTime.now(),
        content = this.content
    )
}