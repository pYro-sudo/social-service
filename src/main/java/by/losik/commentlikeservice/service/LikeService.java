package by.losik.commentlikeservice.service;

import by.losik.commentlikeservice.annotation.Loggable;
import by.losik.commentlikeservice.annotation.PublishActivityEvent;
import by.losik.commentlikeservice.entity.ActivityEventType;
import by.losik.commentlikeservice.entity.Like;
import by.losik.commentlikeservice.repository.LikeRepository;
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
public class LikeService {

    private final LikeRepository likeRepository;

    @Autowired
    public LikeService(LikeRepository likeRepository) {
        this.likeRepository = likeRepository;
    }

    @Cacheable(value = "likes", key = "'image_' + #imageId + '_count'", unless = "#result == null")
    public Mono<Long> countByImageId(Long imageId) {
        return likeRepository.countByImageId(imageId);
    }

    @Cacheable(value = "likes", key = "'user_' + #userId + '_image_' + #imageId", unless = "#result == null")
    public Mono<Boolean> existsByUserIdAndImageId(Long userId, Long imageId) {
        return likeRepository.findByUserIdAndImageId(userId, imageId)
                .hasElement();
    }

    @PublishActivityEvent(type = ActivityEventType.ADD_LIKE)
    @PublishActivityEvent(type = ActivityEventType.REMOVE_LIKE)
    @CacheEvict(value = {"likes", "stats"}, allEntries = true)
    public Mono<Boolean> toggleLike(Long userId, Long imageId) {
        return findByUserIdAndImageId(userId, imageId)
                .flatMap(existingLike ->
                        likeRepository.delete(existingLike)
                                .then(Mono.just(false))
                )
                .switchIfEmpty(Mono.defer(() -> {
                    Like newLike = new Like();
                    newLike.setUserId(userId);
                    newLike.setImageId(imageId);
                    newLike.setCreatedAt(LocalDateTime.now());
                    return likeRepository.save(newLike)
                            .then(Mono.just(true));
                }));
    }

    public Flux<Like> findAll() {
        return likeRepository.findAll();
    }

    public Mono<Like> findById(Long id) {
        return likeRepository.findById(id);
    }
    @PublishActivityEvent(type = ActivityEventType.ADD_LIKE)
    public Mono<Like> save(@NonNull Like like) {
        like.setCreatedAt(LocalDateTime.now());
        return likeRepository.save(like);
    }
    @PublishActivityEvent(type = ActivityEventType.REMOVE_LIKE)
    public Mono<Void> deleteById(Long id) {
        return likeRepository.deleteById(id);
    }
    @PublishActivityEvent(type = ActivityEventType.REMOVE_LIKE)
    public Mono<Void> deleteByUserIdAndImageId(Long userId, Long imageId) {
        return likeRepository.deleteByUserIdAndImageId(userId, imageId);
    }

    public Flux<Like> findByUserId(Long userId) {
        return likeRepository.findByUserId(userId);
    }

    public Flux<Like> findByImageId(Long imageId) {
        return likeRepository.findByImageId(imageId);
    }

    public Mono<Long> countByUserId(Long userId) {
        return likeRepository.countByUserId(userId);
    }
    @PublishActivityEvent(type = ActivityEventType.REMOVE_LIKE)
    public Mono<Void> deleteByImageId(Long imageId) {
        return likeRepository.deleteByImageId(imageId);
    }
    @PublishActivityEvent(type = ActivityEventType.REMOVE_LIKE)
    public Mono<Void> deleteByUserId(Long userId) {
        return likeRepository.deleteByUserId(userId);
    }

    public Flux<Like> findByCreatedAtAfter(LocalDateTime date) {
        return likeRepository.findByCreatedAtAfter(date);
    }

    public Flux<Like> findByCreatedAtBefore(LocalDateTime date) {
        return likeRepository.findByCreatedAtBefore(date);
    }

    public Flux<Like> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return likeRepository.findByCreatedAtBetween(startDate, endDate);
    }

    public Mono<Boolean> isImageLikedByUser(Long userId, Long imageId) {
        return existsByUserIdAndImageId(userId, imageId);
    }

    public Mono<Like> findByUserIdAndImageId(Long userId, Long imageId) {
        return likeRepository.findByUserIdAndImageId(userId, imageId);
    }

    public Mono<Void> deleteAll() {
        return likeRepository.deleteAll();
    }
}