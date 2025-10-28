package by.losik.commentlikeservice.annotation;

import by.losik.commentlikeservice.entity.ActivityEventType;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(PublishActivityEvents.class)
public @interface PublishActivityEvent {
    ActivityEventType type();
    String imageId() default "";
    String userId() default "";
}