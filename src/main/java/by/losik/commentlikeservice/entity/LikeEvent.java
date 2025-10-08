package by.losik.commentlikeservice.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LikeEvent extends Like {
    private ActivityEventType eventType;

    public LikeEvent(Like like, ActivityEventType eventType) {
        this.setId(like.getId());
        this.setUserId(like.getUserId());
        this.setImageId(like.getImageId());
        this.setCreatedAt(like.getCreatedAt());
        this.eventType = eventType;
    }
}