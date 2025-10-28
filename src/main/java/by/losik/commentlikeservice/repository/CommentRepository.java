package by.losik.commentlikeservice.repository;

import by.losik.commentlikeservice.entity.Comment;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface CommentRepository extends R2dbcRepository<Comment, Long> {

    @Query("SELECT * FROM gallery.comments WHERE user_id = :userId ORDER BY created_at DESC")
    Flux<Comment> findByUserId(@NonNull Long userId);

    @Query("SELECT * FROM gallery.comments WHERE image_id = :imageId ORDER BY created_at DESC")
    Flux<Comment> findByImageId(@NonNull Long imageId);

    @Query("SELECT * FROM gallery.comments WHERE user_id = :userId AND image_id = :imageId ORDER BY created_at DESC")
    Flux<Comment> findByUserIdAndImageId(@NonNull Long userId, @NonNull Long imageId);

    @Query("SELECT COUNT(*) FROM gallery.comments WHERE image_id = :imageId")
    Mono<Long> countByImageId(@NonNull Long imageId);

    @Query("SELECT COUNT(*) FROM gallery.comments WHERE user_id = :userId")
    Mono<Long> countByUserId(@NonNull Long userId);

    @Query("DELETE FROM gallery.comments WHERE image_id = :imageId")
    Mono<Void> deleteByImageId(@NonNull Long imageId);

    @Query("DELETE FROM gallery.comments WHERE user_id = :userId")
    Mono<Void> deleteByUserId(@NonNull Long userId);

    @Query("DELETE FROM gallery.comments WHERE user_id = :userId AND image_id = :imageId")
    Mono<Void> deleteByUserIdAndImageId(@NonNull Long userId, @NonNull Long imageId);

    @Query("SELECT * FROM gallery.comments WHERE created_at > :date ORDER BY created_at DESC")
    Flux<Comment> findByCreatedAtAfter(@NonNull LocalDateTime date);

    @Query("SELECT * FROM gallery.comments WHERE created_at < :date ORDER BY created_at DESC")
    Flux<Comment> findByCreatedAtBefore(@NonNull LocalDateTime date);

    @Query("SELECT * FROM gallery.comments WHERE created_at BETWEEN :startDate AND :endDate ORDER BY created_at DESC")
    Flux<Comment> findByCreatedAtBetween(@NonNull LocalDateTime startDate, @NonNull LocalDateTime endDate);

    @Query("SELECT * FROM gallery.comments WHERE content LIKE :content ORDER BY created_at DESC")
    Flux<Comment> findByContentContaining(@NonNull String content);

    @Query("UPDATE gallery.comments SET content = :content WHERE id = :id")
    Mono<Integer> updateContent(@NonNull Long id, @NonNull String content);
}