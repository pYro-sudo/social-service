package by.losik.commentlikeservice.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentEvent extends Comment {
    private ActivityEventType eventType;

    public CommentEvent(Comment comment, ActivityEventType eventType) {
        this.setId(comment.getId());
        this.setUserId(comment.getUserId());
        this.setImageId(comment.getImageId());
        this.setContent(comment.getContent());
        this.setCreatedAt(comment.getCreatedAt());
        this.eventType = eventType;
    }
}