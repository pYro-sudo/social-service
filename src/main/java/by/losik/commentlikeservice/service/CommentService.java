package by.losik.commentlikeservice.service;

import by.losik.commentlikeservice.annotation.Loggable;
import by.losik.commentlikeservice.annotation.PublishActivityEvent;
import by.losik.commentlikeservice.entity.ActivityEventType;
import by.losik.commentlikeservice.entity.Comment;
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

    @Cacheable(value = "comments", key = "#id")
    public Mono<Comment> findById(Long id) {
        return commentRepository.findById(id);
    }

    @Cacheable(value = "comments", key = "'image_' + #imageId")
    public Flux<Comment> findByImageId(Long imageId) {
        return commentRepository.findByImageId(imageId);
    }

    @Cacheable(value = "comments", key = "'image_' + #imageId + '_count'")
    public Mono<Long> countByImageId(Long imageId) {
        return commentRepository.countByImageId(imageId);
    }

    @PublishActivityEvent(type = ActivityEventType.CREATE_COMMENT)
    @CacheEvict(value = {"comments", "stats"}, allEntries = true)
    public Mono<Comment> save(Comment comment) {
        comment.setCreatedAt(LocalDateTime.now());
        return commentRepository.save(comment);
    }

    public Flux<Comment> findAll() {
        return commentRepository.findAll();
    }

    @PublishActivityEvent(type = ActivityEventType.CREATE_COMMENT)
    public Mono<Comment> createComment(Long userId, Long imageId, String content) {
        return Mono.just(new Comment())
                .flatMap(comment -> {
                    comment.setUserId(userId);
                    comment.setImageId(imageId);
                    comment.setContent(content);
                    comment.setCreatedAt(LocalDateTime.now());
                    return commentRepository.save(comment);
                });
    }

    @PublishActivityEvent(type = ActivityEventType.CREATE_COMMENT)
    public Mono<Comment> update(Long id, @NonNull Comment comment) {
        comment.setId(id);
        return commentRepository.save(comment);
    }

    public Mono<Boolean> updateContent(Long id, String content) {
        return commentRepository.updateContent(id, content)
                .map(count -> count > 0);
    }
    @PublishActivityEvent(type = ActivityEventType.REMOVE_COMMENT)
    public Mono<Void> deleteById(Long id) {
        return commentRepository.deleteById(id);
    }

    public Flux<Comment> findByUserId(Long userId) {
        return commentRepository.findByUserId(userId);
    }

    public Flux<Comment> findByUserIdAndImageId(Long userId, Long imageId) {
        return commentRepository.findByUserIdAndImageId(userId, imageId);
    }
    
    public Mono<Long> countByUserId(Long userId) {
        return commentRepository.countByUserId(userId);
    }

    @PublishActivityEvent(type = ActivityEventType.REMOVE_COMMENT)
    public Mono<Void> deleteByImageId(Long imageId) {
        return commentRepository.deleteByImageId(imageId);
    }

    @PublishActivityEvent(type = ActivityEventType.REMOVE_COMMENT)
    public Mono<Void> deleteByUserId(Long userId) {
        return commentRepository.deleteByUserId(userId);
    }

    @PublishActivityEvent(type = ActivityEventType.REMOVE_COMMENT)
    public Mono<Void> deleteByUserIdAndImageId(Long userId, Long imageId) {
        return commentRepository.deleteByUserIdAndImageId(userId, imageId);
    }

    public Flux<Comment> findByCreatedAtAfter(LocalDateTime date) {
        return commentRepository.findByCreatedAtAfter(date);
    }

    public Flux<Comment> findByCreatedAtBefore(LocalDateTime date) {
        return commentRepository.findByCreatedAtBefore(date);
    }

    public Flux<Comment> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return commentRepository.findByCreatedAtBetween(startDate, endDate);
    }

    public Flux<Comment> findByContentContaining(String keyword) {
        return commentRepository.findByContentContaining("%" + keyword + "%");
    }

    public Flux<Comment> findUserRecentComments(Long userId, Integer limit) {
        return findByUserId(userId)
                .take(limit);
    }

    public Flux<Comment> findImageRecentComments(Long imageId, Integer limit) {
        return findByImageId(imageId)
                .take(limit);
    }

}
