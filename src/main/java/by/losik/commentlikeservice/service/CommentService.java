package by.losik.commentlikeservice.service;

import by.losik.commentlikeservice.annotation.Loggable;
import by.losik.commentlikeservice.annotation.PublishActivityEvent;
import by.losik.commentlikeservice.dto.UpdateContentRequest;
import by.losik.commentlikeservice.entity.ActivityEventType;
import by.losik.commentlikeservice.entity.Comment;
import by.losik.commentlikeservice.exception.ResourceNotFoundException;
import by.losik.commentlikeservice.exception.ValidationException;
import by.losik.commentlikeservice.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@Loggable(level = Loggable.Level.DEBUG, logResult = true)
@EnableCaching
public class CommentService {

    private final CommentRepository commentRepository;

    @Autowired
    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @Cacheable(value = "comments", key = "#id", unless = "#result == null")
    public Mono<Comment> findById(Long id) {
        return commentRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Comment", id)));
    }

    @Cacheable(value = "comments", key = "'image_' + #imageId", unless = "#result == null")
    public Flux<Comment> findByImageId(Long imageId) {
        return commentRepository.findByImageId(imageId);
    }

    @Cacheable(value = "comments", key = "'image_' + #imageId + '_count'", unless = "#result == null")
    public Mono<Long> countByImageId(Long imageId) {
        return commentRepository.countByImageId(imageId);
    }

    @PublishActivityEvent(type = ActivityEventType.CREATE_COMMENT)
    @CacheEvict(value = {"comments", "stats"}, allEntries = true)
    public Mono<Comment> save(Comment comment) {
        if (comment.getContent() == null || comment.getContent().trim().isEmpty()) {
            return Mono.error(new ValidationException("Content cannot be blank"));
        }
        if (comment.getUserId() == null) {
            return Mono.error(new ValidationException("User ID cannot be null"));
        }
        if (comment.getImageId() == null) {
            return Mono.error(new ValidationException("Image ID cannot be null"));
        }

        comment.setCreatedAt(LocalDateTime.now());
        return commentRepository.save(comment);
    }

    public Flux<Comment> findAll() {
        return commentRepository.findAll();
    }

    @PublishActivityEvent(type = ActivityEventType.CREATE_COMMENT)
    public Mono<Comment> createComment(Long userId, Long imageId, String content) {
        if (userId == null) {
            return Mono.error(new ValidationException("User ID cannot be null"));
        }
        if (imageId == null) {
            return Mono.error(new ValidationException("Image ID cannot be null"));
        }
        if (content == null || content.trim().isEmpty()) {
            return Mono.error(new ValidationException("Content cannot be blank"));
        }

        return Mono.just(new Comment())
                .flatMap(comment -> {
                    comment.setUserId(userId);
                    comment.setImageId(imageId);
                    comment.setContent(content.trim());
                    comment.setCreatedAt(LocalDateTime.now());
                    return commentRepository.save(comment);
                });
    }

    @PublishActivityEvent(type = ActivityEventType.CREATE_COMMENT)
    public Mono<Comment> update(Long id, @NonNull Comment comment) {
        return commentRepository.existsById(id)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new ResourceNotFoundException("Comment", id));
                    }

                    if (comment.getContent() != null && comment.getContent().trim().isEmpty()) {
                        return Mono.error(new ValidationException("Content cannot be blank"));
                    }

                    comment.setId(id);
                    return commentRepository.save(comment);
                });
    }

    public Mono<Boolean> updateContent(Long id, UpdateContentRequest request) {
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            return Mono.error(new ValidationException("Content cannot be blank"));
        }

        return commentRepository.existsById(id)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new ResourceNotFoundException("Comment", id));
                    }
                    return commentRepository.updateContent(id, request.getContent().trim())
                            .map(count -> count > 0);
                });
    }

    @PublishActivityEvent(type = ActivityEventType.REMOVE_COMMENT)
    public Mono<Void> deleteById(Long id) {
        return commentRepository.existsById(id)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new ResourceNotFoundException("Comment", id));
                    }
                    return commentRepository.deleteById(id);
                });
    }

    public Flux<Comment> findByUserId(Long userId) {
        if (userId == null) {
            return Flux.error(new ValidationException("User ID cannot be null"));
        }
        return commentRepository.findByUserId(userId);
    }

    public Flux<Comment> findByUserIdAndImageId(Long userId, Long imageId) {
        if (userId == null) {
            return Flux.error(new ValidationException("User ID cannot be null"));
        }
        if (imageId == null) {
            return Flux.error(new ValidationException("Image ID cannot be null"));
        }
        return commentRepository.findByUserIdAndImageId(userId, imageId);
    }

    public Mono<Long> countByUserId(Long userId) {
        if (userId == null) {
            return Mono.error(new ValidationException("User ID cannot be null"));
        }
        return commentRepository.countByUserId(userId);
    }

    @PublishActivityEvent(type = ActivityEventType.REMOVE_COMMENT)
    public Mono<Void> deleteByImageId(Long imageId) {
        if (imageId == null) {
            return Mono.error(new ValidationException("Image ID cannot be null"));
        }
        return commentRepository.deleteByImageId(imageId);
    }

    @PublishActivityEvent(type = ActivityEventType.REMOVE_COMMENT)
    public Mono<Void> deleteByUserId(Long userId) {
        if (userId == null) {
            return Mono.error(new ValidationException("User ID cannot be null"));
        }
        return commentRepository.deleteByUserId(userId);
    }

    @PublishActivityEvent(type = ActivityEventType.REMOVE_COMMENT)
    public Mono<Void> deleteByUserIdAndImageId(Long userId, Long imageId) {
        if (userId == null) {
            return Mono.error(new ValidationException("User ID cannot be null"));
        }
        if (imageId == null) {
            return Mono.error(new ValidationException("Image ID cannot be null"));
        }
        return commentRepository.deleteByUserIdAndImageId(userId, imageId);
    }

    public Flux<Comment> findByCreatedAtAfter(LocalDateTime date) {
        if (date == null) {
            return Flux.error(new ValidationException("Date cannot be null"));
        }
        return commentRepository.findByCreatedAtAfter(date);
    }

    public Flux<Comment> findByCreatedAtBefore(LocalDateTime date) {
        if (date == null) {
            return Flux.error(new ValidationException("Date cannot be null"));
        }
        return commentRepository.findByCreatedAtBefore(date);
    }

    public Flux<Comment> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            return Flux.error(new ValidationException("Start date and end date cannot be null"));
        }
        if (startDate.isAfter(endDate)) {
            return Flux.error(new ValidationException("Start date cannot be after end date"));
        }
        return commentRepository.findByCreatedAtBetween(startDate, endDate);
    }

    public Flux<Comment> findByContentContaining(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Flux.error(new ValidationException("Search keyword cannot be blank"));
        }
        return commentRepository.findByContentContaining("%" + keyword.trim() + "%");
    }

    public Flux<Comment> findUserRecentComments(Long userId, Integer limit) {
        if (userId == null) {
            return Flux.error(new ValidationException("User ID cannot be null"));
        }
        if (limit == null || limit <= 0) {
            return Flux.error(new ValidationException("Limit must be positive"));
        }
        return findByUserId(userId)
                .take(limit);
    }

    public Flux<Comment> findImageRecentComments(Long imageId, Integer limit) {
        if (imageId == null) {
            return Flux.error(new ValidationException("Image ID cannot be null"));
        }
        if (limit == null || limit <= 0) {
            return Flux.error(new ValidationException("Limit must be positive"));
        }
        return findByImageId(imageId)
                .take(limit);
    }

    public Mono<Void> deleteAll() {
        return commentRepository.deleteAll();
    }
}