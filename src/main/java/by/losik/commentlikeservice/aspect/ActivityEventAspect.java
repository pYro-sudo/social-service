package by.losik.commentlikeservice.aspect;

import by.losik.commentlikeservice.annotation.PublishActivityEvent;
import by.losik.commentlikeservice.annotation.PublishActivityEvents;
import by.losik.commentlikeservice.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class ActivityEventAspect {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    @Value("${spring.kafka.topic:activity-events}")
    private String ACTIVITY_TOPIC;

    @AfterReturning(value = "@annotation(publishActivityEvents)", returning = "result")
    public void publishActivityEvents(JoinPoint joinPoint, @NonNull PublishActivityEvents publishActivityEvents, Object result) {
        Arrays.stream(publishActivityEvents.value())
                .forEach(annotation -> handleEvent(joinPoint, annotation, result));
    }

    @AfterReturning(value = "@annotation(publishEvent)", returning = "result")
    public void publishActivityEvent(JoinPoint joinPoint, PublishActivityEvent publishEvent, Object result) {
        handleEvent(joinPoint, publishEvent, result);
    }

    private void handleEvent(JoinPoint joinPoint, PublishActivityEvent publishEvent, Object result) {
        if (result instanceof Mono) {
            handleReactiveMethod((Mono<?>) result, publishEvent.type(), joinPoint);
        } else if (result != null) {
            handleSyncMethod(result, publishEvent.type(), joinPoint);
        }
    }

    private void handleReactiveMethod(@NonNull Mono<?> resultMono, ActivityEventType eventType, JoinPoint joinPoint) {
        resultMono
                .doOnSuccess(entity -> {
                    if (entity != null) {
                        sendEvent(eventType, entity, joinPoint);
                    }
                })
                .doOnError(error -> log.error("Error in reactive method, skipping event sending: {}",
                        joinPoint.getSignature().getName(), error));
    }

    private void handleSyncMethod(Object result, ActivityEventType eventType, JoinPoint joinPoint) {
        sendEvent(eventType, result, joinPoint);
    }

    private void sendEvent(ActivityEventType eventType, Object entity, JoinPoint joinPoint) {
        try {
            Object event = createEvent(eventType, entity);
            if (event != null) {
                CompletableFuture<?> sendFuture = kafkaTemplate.send(ACTIVITY_TOPIC, event);

                sendFuture
                        .thenAccept(
                                sendResult ->
                                        log.debug("Successfully sent activity event: {} with result: {}", event, sendResult)
                        )
                        .exceptionally(
                                throwable -> {
                                    log.error("Failed to send activity event: {}", event, throwable);
                                    return null;
                                });
            }
        } catch (Exception e) {
            log.error("Failed to process activity event for method: {}",
                    joinPoint.getSignature().getName(), e);
        }
    }

    private Object createEvent(ActivityEventType eventType, Object entity) {
        switch (eventType) {
            case ADD_LIKE:
            case REMOVE_LIKE:
                if (entity instanceof Like) {
                    return new LikeEvent((Like) entity, eventType);
                }
                break;
            case CREATE_COMMENT:
            case REMOVE_COMMENT:
                if (entity instanceof Comment) {
                    return new CommentEvent((Comment) entity, eventType);
                }
                break;
        }
        return null;
    }
}