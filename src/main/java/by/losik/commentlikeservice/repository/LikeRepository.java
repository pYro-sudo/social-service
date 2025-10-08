package by.losik.commentlikeservice.repository;

import by.losik.commentlikeservice.entity.Like;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface LikeRepository extends R2dbcRepository<Like, Long> {

    @Query("SELECT * FROM gallery.likes WHERE user_id = :userId ORDER BY created_at DESC")
    Flux<Like> findByUserId(@NonNull Long userId);

    @Query("SELECT * FROM gallery.likes WHERE image_id = :imageId ORDER BY created_at DESC")
    Flux<Like> findByImageId(@NonNull Long imageId);

    @Query("SELECT * FROM gallery.likes WHERE user_id = :userId AND image_id = :imageId")
    Mono<Like> findByUserIdAndImageId(@NonNull Long userId, @NonNull Long imageId);

    @Query("SELECT COUNT(*) FROM gallery.likes WHERE image_id = :imageId")
    Mono<Long> countByImageId(@NonNull Long imageId);

    @Query("SELECT COUNT(*) FROM gallery.likes WHERE user_id = :userId")
    Mono<Long> countByUserId(@NonNull Long userId);

    @Query("DELETE FROM gallery.likes WHERE user_id = :userId AND image_id = :imageId")
    Mono<Void> deleteByUserIdAndImageId(@NonNull Long userId, @NonNull Long imageId);

    @Query("DELETE FROM gallery.likes WHERE image_id = :imageId")
    Mono<Void> deleteByImageId(@NonNull Long imageId);

    @Query("DELETE FROM gallery.likes WHERE user_id = :userId")
    Mono<Void> deleteByUserId(@NonNull Long userId);

    @Query("SELECT * FROM gallery.likes WHERE created_at > :date ORDER BY created_at DESC")
    Flux<Like> findByCreatedAtAfter(@NonNull LocalDateTime date);

    @Query("SELECT * FROM gallery.likes WHERE created_at < :date ORDER BY created_at DESC")
    Flux<Like> findByCreatedAtBefore(@NonNull LocalDateTime date);

    @Query("SELECT * FROM gallery.likes WHERE created_at BETWEEN :startDate AND :endDate ORDER BY created_at DESC")
    Flux<Like> findByCreatedAtBetween(@NonNull LocalDateTime startDate, @NonNull LocalDateTime endDate);
}